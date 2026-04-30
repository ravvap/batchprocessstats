package gov.fdic.tip.bps.config;

import gov.fdic.tip.bps.config.ApplicationConstants.ErrorMessages;
import gov.fdic.tip.bps.config.ApplicationConstants.ProblemTitle;
import gov.fdic.tip.bps.config.ApplicationConstants.ProblemType;
import gov.fdic.tip.bps.dto.ApiProblemDetail;
import gov.fdic.tip.bps.exception.BatchStatisticsNotFoundException;
import gov.fdic.tip.bps.exception.SourceSystemNotFoundException;
import gov.fdic.tip.bps.exception.ServerManagedFieldException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/**
 * Centralised error handling — all string literals sourced from ApplicationConstants.
 *
 * Produces application/problem+json for all error responses (BPS-004 through BPS-008).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String PROBLEM_CONTENT_TYPE = "application/problem+json";

    // ------------------------------------------------------------------ //
    //  405 — DELETE / PATCH not allowed (BPS-008)                        //
    // ------------------------------------------------------------------ //

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiProblemDetail> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {

        log.warn("{} method={} path={}",
                ApplicationConstants.AuditEvents.BATCH_STATISTICS_METHOD_REJECTED,
                ex.getMethod(), request.getRequestURI());

        HttpHeaders headers = new HttpHeaders();
        if (ex.getSupportedMethods() != null) {
            headers.add(HttpHeaders.ALLOW, String.join(", ", ex.getSupportedMethods()));
        }
        headers.setContentType(MediaType.parseMediaType(PROBLEM_CONTENT_TYPE));

        ApiProblemDetail body = ApiProblemDetail.builder()
                .type(ProblemType.METHOD_NOT_ALLOWED)
                .title(ProblemTitle.METHOD_NOT_ALLOWED)
                .status(405)
                .detail(ErrorMessages.METHOD_NOT_SUPPORTED_PREFIX + ex.getMethod()
                        + ErrorMessages.METHOD_NOT_SUPPORTED_SUFFIX
                        + ex.getSupportedMethods())
                .instance(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .headers(headers)
                .body(body);
    }

    // ------------------------------------------------------------------ //
    //  400 — Bean Validation failures (@Valid)                            //
    // ------------------------------------------------------------------ //

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiProblemDetail> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        List<ApiProblemDetail.FieldViolation> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> ApiProblemDetail.FieldViolation.builder()
                        .field(fe.getField())
                        .message(fe.getDefaultMessage())
                        .build())
                .toList();

        ApiProblemDetail body = ApiProblemDetail.builder()
                .type(ProblemType.VALIDATION_ERROR)
                .title(ProblemTitle.VALIDATION_FAILED)
                .status(400)
                .detail(ErrorMessages.VALIDATION_FAILED)
                .instance(request.getRequestURI())
                .errors(errors)
                .build();

        return responseEntity(HttpStatus.BAD_REQUEST, body);
    }

    // ------------------------------------------------------------------ //
    //  400 — Server-managed fields present in request body               //
    // ------------------------------------------------------------------ //

    @ExceptionHandler(ServerManagedFieldException.class)
    public ResponseEntity<ApiProblemDetail> handleServerManagedField(
            ServerManagedFieldException ex,
            HttpServletRequest request) {

        List<ApiProblemDetail.FieldViolation> errors = ex.getOffendingFields().stream()
                .map(f -> ApiProblemDetail.FieldViolation.builder()
                        .field(f)
                        .message(ErrorMessages.SERVER_MANAGED_FIELD)
                        .build())
                .toList();

        ApiProblemDetail body = ApiProblemDetail.builder()
                .type(ProblemType.SERVER_MANAGED_FIELD)
                .title(ProblemTitle.SERVER_MANAGED_FIELD)
                .status(400)
                .detail("Request body contained one or more server-managed fields: "
                        + ex.getOffendingFields())
                .instance(request.getRequestURI())
                .errors(errors)
                .build();

        return responseEntity(HttpStatus.BAD_REQUEST, body);
    }

    // ------------------------------------------------------------------ //
    //  400 — Sort / type mismatch / endDate < startDate                  //
    // ------------------------------------------------------------------ //

    @ExceptionHandler({IllegalArgumentException.class,
                       MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiProblemDetail> handleBadRequest(
            Exception ex,
            HttpServletRequest request) {

        ApiProblemDetail body = ApiProblemDetail.builder()
                .type(ProblemType.BAD_REQUEST)
                .title(ProblemTitle.BAD_REQUEST)
                .status(400)
                .detail(ex.getMessage())
                .instance(request.getRequestURI())
                .build();

        return responseEntity(HttpStatus.BAD_REQUEST, body);
    }

    // ------------------------------------------------------------------ //
    //  404 — Record not found (BPS-005, BPS-007)                         //
    // ------------------------------------------------------------------ //

    @ExceptionHandler({BatchStatisticsNotFoundException.class,
                       SourceSystemNotFoundException.class})
    public ResponseEntity<ApiProblemDetail> handleNotFound(
            RuntimeException ex,
            HttpServletRequest request) {

        ApiProblemDetail body = ApiProblemDetail.builder()
                .type(ProblemType.NOT_FOUND)
                .title(ProblemTitle.NOT_FOUND)
                .status(404)
                .detail(ex.getMessage())
                .instance(request.getRequestURI())
                .build();

        return responseEntity(HttpStatus.NOT_FOUND, body);
    }

    // ------------------------------------------------------------------ //
    //  401 — Unauthenticated                                              //
    // ------------------------------------------------------------------ //

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiProblemDetail> handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request) {

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.WWW_AUTHENTICATE, "Bearer");

        ApiProblemDetail body = ApiProblemDetail.builder()
                .type(ProblemType.UNAUTHORIZED)
                .title(ProblemTitle.UNAUTHORIZED)
                .status(401)
                .detail(ErrorMessages.BEARER_TOKEN_REQUIRED)
                .instance(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .headers(headers)
                .body(body);
    }

    // ------------------------------------------------------------------ //
    //  403 — Forbidden                                                    //
    // ------------------------------------------------------------------ //

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiProblemDetail> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {

        ApiProblemDetail body = ApiProblemDetail.builder()
                .type(ProblemType.FORBIDDEN)
                .title(ProblemTitle.FORBIDDEN)
                .status(403)
                .detail(ErrorMessages.FORBIDDEN)
                .instance(request.getRequestURI())
                .build();

        return responseEntity(HttpStatus.FORBIDDEN, body);
    }

    // ------------------------------------------------------------------ //
    //  500 — Catch-all                                                    //
    // ------------------------------------------------------------------ //

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiProblemDetail> handleUnexpected(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unhandled exception on {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        ApiProblemDetail body = ApiProblemDetail.builder()
                .type(ProblemType.INTERNAL_SERVER_ERROR)
                .title(ProblemTitle.INTERNAL_SERVER_ERROR)
                .status(500)
                .detail(ErrorMessages.INTERNAL_SERVER_ERROR)
                .instance(request.getRequestURI())
                .build();

        return responseEntity(HttpStatus.INTERNAL_SERVER_ERROR, body);
    }

    // ------------------------------------------------------------------ //
    //  Helper                                                             //
    // ------------------------------------------------------------------ //

    private ResponseEntity<ApiProblemDetail> responseEntity(HttpStatus status, ApiProblemDetail body) {
        return ResponseEntity.status(status)
                .contentType(MediaType.parseMediaType(PROBLEM_CONTENT_TYPE))
                .body(body);
    }
}
// NOTE: SourceSystemNotFoundException handler added below the existing class closing brace
