package pl.tomaszsieminski.coupon.web;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import pl.tomaszsieminski.coupon.domain.exception.CouponAlreadyExistsException;
import pl.tomaszsieminski.coupon.domain.exception.CouponCountryMismatchException;
import pl.tomaszsieminski.coupon.domain.exception.CouponExhaustedException;
import pl.tomaszsieminski.coupon.domain.exception.CouponNotFoundException;
import pl.tomaszsieminski.coupon.domain.exception.GeoIpCountryResolutionException;
import pl.tomaszsieminski.coupon.domain.exception.UserAlreadyUsedCouponException;

@RestControllerAdvice
@NullMarked
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "Request validation failed");
        problem.setTitle("Validation Failed");
        problem.setProperty("errors", toValidationErrors(ex.getBindingResult().getFieldErrors()));

        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        problem.setTitle("Validation Failed");
        problem.setProperty(
                "errors",
                ex.getConstraintViolations().stream()
                        .map(violation ->
                                new ValidationError(violation.getPropertyPath().toString(), violation.getMessage()))
                        .toList());
        return problem;
    }

    @ExceptionHandler(CouponAlreadyExistsException.class)
    public ProblemDetail handleCouponAlreadyExists(CouponAlreadyExistsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Coupon Already Exists");
        return problem;
    }

    @ExceptionHandler(CouponNotFoundException.class)
    public ProblemDetail handleCouponNotFound(CouponNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Coupon not found");
        return problem;
    }

    @ExceptionHandler(CouponExhaustedException.class)
    public ProblemDetail handleCouponExhausted(CouponExhaustedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
        problem.setTitle("Coupon Exhausted");
        return problem;
    }

    @ExceptionHandler(UserAlreadyUsedCouponException.class)
    public ProblemDetail handleUserAlreadyUsed(UserAlreadyUsedCouponException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("User Already Redeemed");
        return problem;
    }

    @ExceptionHandler(CouponCountryMismatchException.class)
    public ProblemDetail handleCouponCountryMismatch(CouponCountryMismatchException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Coupon Country Mismatch");
        return problem;
    }

    @ExceptionHandler(GeoIpCountryResolutionException.class)
    public ProblemDetail handleGeoIpCountryResolution(GeoIpCountryResolutionException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        problem.setTitle("GeoIP Country Resolution Failed");
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error");
        problem.setTitle("Internal Server Error");
        return problem;
    }

    private List<ValidationError> toValidationErrors(List<FieldError> fieldErrors) {
        return fieldErrors.stream()
                .map(error -> new ValidationError(error.getField(), error.getDefaultMessage()))
                .toList();
    }

    record ValidationError(String field, String message) {}
}
