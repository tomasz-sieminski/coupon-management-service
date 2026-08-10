import http, { expectedStatuses, setResponseCallback } from 'k6/http';
import { Counter } from 'k6/metrics';
import { check } from 'k6';
import { sleep } from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const readinessUrls = (__ENV.READINESS_URLS || '').split(',').filter((url) => url.length > 0);
const couponMaxUses = Number(__ENV.COUPON_MAX_USES || 100);
const reportDirectory = __ENV.REPORT_DIR || '/reports';

const countryProfiles = [
  { countryCode: 'PL', ipAddress: '203.0.113.10' },
  { countryCode: 'DE', ipAddress: '198.51.100.20' },
  { countryCode: 'US', ipAddress: '192.0.2.30' },
];

setResponseCallback(expectedStatuses(200, 201, 204, 400, 403, 404, 409, 422));

const redemptionSuccess = new Counter('redemption_success');
const redemptionExhausted = new Counter('redemption_exhausted');
const countryRedemptionSuccess = new Counter('country_redemption_success');
const countryRedemptionMismatch = new Counter('country_redemption_mismatch');
const duplicateUserRejected = new Counter('duplicate_user_rejected');
const unknownCouponRejected = new Counter('unknown_coupon_rejected');
const invalidRequestRejected = new Counter('invalid_request_rejected');
const unexpectedRedemptionResponse = new Counter('unexpected_redemption_response');

export const options = {
  scenarios: {
    redemption_race: {
      executor: 'constant-arrival-rate',
      exec: 'redeemRaceCoupon',
      rate: 200,
      timeUnit: '1s',
      duration: '10s',
      preAllocatedVUs: 100,
      maxVUs: 300,
    },
    country_redemption: {
      executor: 'constant-arrival-rate',
      exec: 'redeemCountryCoupon',
      rate: 30,
      timeUnit: '1s',
      duration: '10s',
      preAllocatedVUs: 20,
      maxVUs: 80,
    },
    expected_error_resilience: {
      executor: 'constant-arrival-rate',
      exec: 'redeemExpectedError',
      rate: 30,
      timeUnit: '1s',
      duration: '10s',
      preAllocatedVUs: 20,
      maxVUs: 80,
    },
  },
  thresholds: {
    checks: ['rate==1'],
    'http_req_failed{scenario:redemption_race}': ['rate<0.01'],
    'http_req_failed{scenario:country_redemption}': ['rate<0.01'],
    'http_req_failed{scenario:expected_error_resilience}': ['rate<0.01'],
    redemption_success: [`count<=${couponMaxUses}`],
    country_redemption_success: ['count>0'],
    country_redemption_mismatch: ['count>0'],
    duplicate_user_rejected: ['count>0'],
    unknown_coupon_rejected: ['count>0'],
    invalid_request_rejected: ['count>0'],
    unexpected_redemption_response: ['count==0'],
  },
};

export function setup() {
  waitForReadiness();

  const runId = Date.now();
  const raceCouponCode = `LOAD-${runId}`;
  createCoupon(raceCouponCode, couponMaxUses, 'PL');

  const countryCoupons = {};
  for (const profile of countryProfiles) {
    const couponCode = `COUNTRY-${profile.countryCode}-${runId}`;
    createCoupon(couponCode, 100000, profile.countryCode);
    countryCoupons[profile.countryCode] = couponCode;
  }

  const duplicateCouponCode = `DUPLICATE-${runId}`;
  const duplicateUserId = `duplicate-user-${runId}`;
  createCoupon(duplicateCouponCode, 100000, 'PL');
  const duplicatePrimingResponse = redeem(duplicateCouponCode, duplicateUserId, countryProfiles[0].ipAddress);
  check(duplicatePrimingResponse, {
    'duplicate coupon primed': (res) => res.status === 204,
  });

  const mismatchCouponCode = `MISMATCH-DE-${runId}`;
  createCoupon(mismatchCouponCode, 100000, 'DE');

  return {
    raceCouponCode,
    countryCoupons,
    duplicateCouponCode,
    duplicateUserId,
    mismatchCouponCode,
    unknownCouponCode: `UNKNOWN-${runId}`,
  };
}

function waitForReadiness() {
  if (readinessUrls.length === 0) {
    return;
  }

  const timeoutAt = Date.now() + 60000;
  while (Date.now() < timeoutAt) {
    const responses = readinessUrls.map((url) => http.get(url));
    const allReady = responses.every((response) => response.status === 200 && response.body.includes('"UP"'));

    if (allReady) {
      return;
    }

    sleep(0.5);
  }

  throw new Error(`Application did not become ready within 60s: ${readinessUrls.join(', ')}`);
}

export function redeemRaceCoupon(data) {
  const response = redeem(data.raceCouponCode, `load-user-${__VU}-${__ITER}`, countryProfiles[0].ipAddress);

  if (response.status === 204) {
    redemptionSuccess.add(1);
    return;
  }

  if (response.status === 422) {
    redemptionExhausted.add(1);
    return;
  }

  unexpectedRedemptionResponse.add(1, { status: String(response.status) });
}

export function redeemCountryCoupon(data) {
  const profile = countryProfiles[__ITER % countryProfiles.length];
  const response = redeem(
    data.countryCoupons[profile.countryCode],
    `country-user-${profile.countryCode}-${__VU}-${__ITER}`,
    profile.ipAddress
  );

  if (response.status === 204) {
    countryRedemptionSuccess.add(1, { country: profile.countryCode });
    return;
  }

  unexpectedRedemptionResponse.add(1, { status: String(response.status), scenario: 'country_redemption' });
}

export function redeemExpectedError(data) {
  const caseIndex = __ITER % 4;

  if (caseIndex === 0) {
    const response = redeem(data.mismatchCouponCode, `mismatch-user-${__VU}-${__ITER}`, countryProfiles[0].ipAddress);
    if (response.status === 403) {
      countryRedemptionMismatch.add(1);
      return;
    }
    unexpectedRedemptionResponse.add(1, { status: String(response.status), scenario: 'country_mismatch' });
    return;
  }

  if (caseIndex === 1) {
    const response = redeem(data.duplicateCouponCode, data.duplicateUserId, countryProfiles[0].ipAddress);
    if (response.status === 409) {
      duplicateUserRejected.add(1);
      return;
    }
    unexpectedRedemptionResponse.add(1, { status: String(response.status), scenario: 'duplicate_user' });
    return;
  }

  if (caseIndex === 2) {
    const response = redeem(data.unknownCouponCode, `unknown-user-${__VU}-${__ITER}`, countryProfiles[0].ipAddress);
    if (response.status === 404) {
      unknownCouponRejected.add(1);
      return;
    }
    unexpectedRedemptionResponse.add(1, { status: String(response.status), scenario: 'unknown_coupon' });
    return;
  }

  const response = http.post(
    `${baseUrl}/api/v1/coupons/${data.unknownCouponCode}/redeem`,
    JSON.stringify({
      userId: '',
    }),
    {
      headers: jsonHeaders(countryProfiles[0].ipAddress),
    }
  );

  if (response.status === 400) {
    invalidRequestRejected.add(1);
    return;
  }

  unexpectedRedemptionResponse.add(1, { status: String(response.status), scenario: 'invalid_or_unknown_coupon' });
}

export function handleSummary(data) {
  const summary = {
    successCount: metricCount(data, 'redemption_success'),
    exhaustedCount: metricCount(data, 'redemption_exhausted'),
    countrySuccessCount: metricCount(data, 'country_redemption_success'),
    countryMismatchCount: metricCount(data, 'country_redemption_mismatch'),
    duplicateRejectedCount: metricCount(data, 'duplicate_user_rejected'),
    unknownCouponRejectedCount: metricCount(data, 'unknown_coupon_rejected'),
    invalidRequestRejectedCount: metricCount(data, 'invalid_request_rejected'),
    unexpectedCount: metricCount(data, 'unexpected_redemption_response'),
    scenarioHttpFailedRate: Math.max(
      metricRate(data, 'http_req_failed{scenario:redemption_race}'),
      metricRate(data, 'http_req_failed{scenario:country_redemption}'),
      metricRate(data, 'http_req_failed{scenario:expected_error_resilience}')
    ),
    requestDuration: data.metrics.http_req_duration?.values || {},
    couponMaxUses,
  };

  return {
    stdout: textSummary(summary),
    [`${reportDirectory}/redemption-race-summary.json`]: JSON.stringify(data, null, 2),
    [`${reportDirectory}/redemption-race-report.html`]: htmlReport(summary),
  };
}

function createCoupon(code, maxUses, countryCode) {
  const response = http.post(
    `${baseUrl}/api/v1/coupons`,
    JSON.stringify({
      code,
      maxUses,
      countryCode,
    }),
    {
      headers: jsonHeaders(countryProfiles[0].ipAddress),
    }
  );

  check(response, {
    [`coupon ${code} created`]: (res) => res.status === 201,
  });
}

function redeem(couponCode, userId, ipAddress) {
  return http.post(
    `${baseUrl}/api/v1/coupons/${couponCode}/redeem`,
    JSON.stringify({
      userId,
    }),
    {
      headers: jsonHeaders(ipAddress),
    }
  );
}

function jsonHeaders(ipAddress) {
  return {
    'Content-Type': 'application/json',
    'X-Forwarded-For': ipAddress,
  };
}

function metricCount(data, name) {
  return data.metrics[name]?.values?.count || 0;
}

function metricRate(data, name) {
  return data.metrics[name]?.values?.rate || 0;
}

function textSummary(summary) {
  return `
Coupon redemption load report
=============================
Race successful redemptions: ${summary.successCount}
Race expected exhausted responses: ${summary.exhaustedCount}
Country redemption successes: ${summary.countrySuccessCount}
Country mismatch rejections: ${summary.countryMismatchCount}
Duplicate user rejections: ${summary.duplicateRejectedCount}
Unknown coupon rejections: ${summary.unknownCouponRejectedCount}
Invalid request rejections: ${summary.invalidRequestRejectedCount}
Unexpected responses: ${summary.unexpectedCount}
Scenario HTTP failed rate: ${(summary.scenarioHttpFailedRate * 100).toFixed(2)}%
HTTP duration p95: ${formatMs(summary.requestDuration['p(95)'])}
HTTP duration max: ${formatMs(summary.requestDuration.max)}
`;
}

function htmlReport(summary) {
  const generatedAt = new Date().toISOString();
  const passedChecks =
    summary.successCount <= summary.couponMaxUses &&
    summary.countrySuccessCount > 0 &&
    summary.countryMismatchCount > 0 &&
    summary.duplicateRejectedCount > 0 &&
    summary.unknownCouponRejectedCount > 0 &&
    summary.invalidRequestRejectedCount > 0 &&
    summary.unexpectedCount === 0 &&
    summary.scenarioHttpFailedRate < 0.01;

  return `<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <title>Coupon redemption load report</title>
  <style>
    body { font-family: Inter, Segoe UI, Arial, sans-serif; margin: 2rem; color: #172033; background: #f6f8fb; }
    main { max-width: 1080px; margin: 0 auto; }
    h1 { margin-bottom: 0.25rem; }
    .subtitle { color: #526072; margin-top: 0; }
    .status { display: inline-block; padding: 0.35rem 0.7rem; border-radius: 999px; font-weight: 700; }
    .pass { background: #dcfce7; color: #166534; }
    .fail { background: #fee2e2; color: #991b1b; }
    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 1rem; margin-top: 1.5rem; }
    .card { background: #fff; border: 1px solid #e4e8f0; border-radius: 14px; padding: 1.1rem; box-shadow: 0 1px 3px rgba(15, 23, 42, 0.06); }
    .label { color: #667085; font-size: 0.9rem; }
    .value { font-size: 2rem; font-weight: 800; margin-top: 0.35rem; }
    .note { background: #fff; border-left: 4px solid #2563eb; padding: 1rem; border-radius: 8px; margin-top: 1.5rem; }
    code { background: #eef2f7; padding: 0.15rem 0.35rem; border-radius: 4px; }
  </style>
</head>
<body>
<main>
  <h1>Coupon redemption load report</h1>
  <p class="subtitle">Generated at ${generatedAt}</p>
  <span class="status ${passedChecks ? 'pass' : 'fail'}">${passedChecks ? 'PASSED' : 'FAILED'}</span>

  <section class="grid">
    ${metricCard('Coupon max uses', summary.couponMaxUses)}
    ${metricCard('Race successful redemptions', summary.successCount)}
    ${metricCard('Race exhausted responses', summary.exhaustedCount)}
    ${metricCard('Country redemption successes', summary.countrySuccessCount)}
    ${metricCard('Country mismatch rejections', summary.countryMismatchCount)}
    ${metricCard('Duplicate user rejections', summary.duplicateRejectedCount)}
    ${metricCard('Unknown coupon rejections', summary.unknownCouponRejectedCount)}
    ${metricCard('Invalid request rejections', summary.invalidRequestRejectedCount)}
    ${metricCard('Unexpected responses', summary.unexpectedCount)}
    ${metricCard('Scenario HTTP failed rate', `${(summary.scenarioHttpFailedRate * 100).toFixed(2)}%`)}
    ${metricCard('HTTP duration p95', formatMs(summary.requestDuration['p(95)']))}
    ${metricCard('HTTP duration max', formatMs(summary.requestDuration.max))}
  </section>

  <section class="note">
    <strong>Checks:</strong>
    successful race redemptions must not exceed <code>COUPON_MAX_USES</code>. The script also verifies successful
    country-specific redemptions for PL/DE/US and expected API rejections for country mismatch, duplicate user,
    unknown coupon, and invalid request paths.
  </section>
</main>
</body>
</html>`;
}

function metricCard(label, value) {
  return `<article class="card"><div class="label">${label}</div><div class="value">${value}</div></article>`;
}

function formatMs(value) {
  if (value === undefined) {
    return 'n/a';
  }
  return `${value.toFixed(2)} ms`;
}
