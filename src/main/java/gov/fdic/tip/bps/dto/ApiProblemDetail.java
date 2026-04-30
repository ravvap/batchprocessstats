package gov.fdic.tip.bps.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * RFC 7807 problem+json response body.
 * Named ApiProblemDetail to avoid collision with Spring 6's
 * org.springframework.http.ProblemDetail.
 *
 * Used for 400, 401, 403, 404, 405 responses per BPS-004 through BPS-010.
 */
@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiProblemDetail {

    String type;
    String title;
    int status;
    String detail;
    String instance;

    /** Per-field validation errors for 400 responses (BPS-006, BPS-007). */
    List<FieldViolation> errors;

    @Value
    @Builder
    public static class FieldViolation {
        String field;
        String message;
    }
}
