package pl.tomaszsieminski.coupon.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class IsoCountryCodeValidator implements ConstraintValidator<IsoCountryCode, String> {

    private static final Set<String> ISO_COUNTRY_CODES =
            Arrays.stream(Locale.getISOCountries()).collect(Collectors.toUnmodifiableSet());

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return ISO_COUNTRY_CODES.contains(value.toUpperCase(Locale.ROOT));
    }
}
