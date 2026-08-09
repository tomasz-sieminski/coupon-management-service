package pl.tomaszsieminski.coupon.application;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class CouponMetrics {

    private static final String REDEMPTIONS_COUNTER = "coupon.redemptions";

    private final MeterRegistry meterRegistry;

    public CouponMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordSuccessfulRedemption() {
        redemptionCounter("success", "none").increment();
    }

    public void recordFailedRedemption(Throwable exception) {
        redemptionCounter("failure", exception.getClass().getSimpleName()).increment();
    }

    private Counter redemptionCounter(String outcome, String reason) {
        return Counter.builder(REDEMPTIONS_COUNTER)
                .description("Coupon redemption attempts")
                .tag("outcome", outcome)
                .tag("reason", reason)
                .register(meterRegistry);
    }
}
