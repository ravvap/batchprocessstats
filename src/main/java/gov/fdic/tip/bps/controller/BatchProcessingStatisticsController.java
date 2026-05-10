package gov.fdic.tip.bps.controller;

import gov.fdic.tip.bps.config.ApplicationConstants.ApiPaths;
import gov.fdic.tip.bps.config.ApplicationConstants.AuditEvents;
import gov.fdic.tip.bps.config.ApplicationConstants.JwtClaims;
import gov.fdic.tip.bps.config.ApplicationConstants.Roles;
import gov.fdic.tip.bps.config.ApplicationConstants.SortFields;
import gov.fdic.tip.bps.config.ApplicationConstants.Pagination;
import gov.fdic.tip.bps.dto.ApiProblemDetail;
import gov.fdic.tip.bps.dto.BatchProcessingStatisticsDto;
import gov.fdic.tip.bps.dto.BatchProcessingStatisticsDto.PagedResponse;
import gov.fdic.tip.bps.dto.BatchProcessingStatisticsDto.Response;
import gov.fdic.tip.bps.service.BatchProcessingStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;

/**
 * REST controller for Batch Processing Statistics.
 * Backed by batch_job_history + batch_source_system tables.
 *
 * Endpoints (BPS-004 through BPS-007):
 *   GET    /api/v1/batch-processing-statistics          — list   (BPS-004)
 *   GET    /api/v1/batch-processing-statistics/{id}     — by id  (BPS-005)
 *   POST   /api/v1/batch-processing-statistics          — create (BPS-006)
 *   PUT    /api/v1/batch-processing-statistics/{id}     — replace selected fields (BPS-007)
 *
 * DELETE and PATCH are NOT mapped; they return 405 via GlobalExceptionHandler (BPS-008).
 *
 * OpenAPI 3.1 (BPS-010):
 *   Spec published at /v3/api-docs, Swagger UI at /swagger-ui.html.
 *   Declares four operations with 400/401/403/404/405 responses.
 *   DELETE and PATCH are not declared but the 405 schema is attached.
 *
 * Observability (BPS-010):
 *   Two logging tiers:
 *     1. Technical request log  — every HTTP request regardless of outcome.
 *     2. Named business audit   — only on specific success conditions (listed,
 *        viewed, created, updated) using TIP dot-notation event taxonomy.
 *
 *   Every log entry includes: correlation id (from X-Correlation-Id header,
 *   propagated from the inbound request), HTTP method, path, status code,
 *   and latency in milliseconds.
 *
 * Authorization (BPS-003, BPS-009):
 *   GET  → BATCH_PRCS_STATS_VIEW
 *   POST → BATCH_PRCS_STATS_ADD or BATCH_PRCS_STATS_EDIT
 *   PUT  → BATCH_PRCS_STATS_ADD or BATCH_PRCS_STATS_EDIT
 */
@Slf4j
@RestController
@RequestMapping(ApiPaths.BPS_V1_BASE)
@RequiredArgsConstructor
@Tag(name = "Batch Processing Statistics",
     description = "Read and write access to batch job history. BPS-001 through BPS-010.")
public class BatchProcessingStatisticsController {

    private static final String CORRELATION_HEADER = "X-Correlation-Id";

    private final BatchProcessingStatisticsService service;

    // ------------------------------------------------------------------ //
    //  BPS-004: GET list                                                  //
    // ------------------------------------------------------------------ //

    @GetMapping(produces = "application/json")
    @PreAuthorize(Roles.Expr.LIST_VIEWERS)
    @Operation(
        summary     = "List batch job history records",
        description = "Returns a paginated, sortable, filterable list. " +
                      "Emits 'batch.statistics.listed' audit event on success."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = "Invalid query parameter",
                     content = @Content(schema = @Schema(implementation = ApiProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Unauthenticated",
                     content = @Content(schema = @Schema(implementation = ApiProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden — caller lacks VIEW role",
                     content = @Content(schema = @Schema(implementation = ApiProblemDetail.class))),
        @ApiResponse(responseCode = "405", description = "Method Not Allowed (DELETE / PATCH)",
                     content = @Content(schema = @Schema(implementation = ApiProblemDetail.class)))
    })
    public ResponseEntity<PagedResponse<Response>> list(
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_PAGE)      int    page,
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_PAGE_SIZE) int    size,
            @RequestParam(defaultValue = SortFields.DEFAULT_SORT)           String sort,
            @RequestParam(required = false) String  sourceName,
            @RequestParam(required = false) String  jobStatus,
            @RequestParam(required = false) String  jobType,
            @RequestParam(required = false) Instant startTimeFrom,
            @RequestParam(required = false) Instant startTimeTo,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request
    ) {
        long start = System.currentTimeMillis();
        String correlationId = correlationId(request);
        String caller        = subject(jwt);

        log.debug("REQUEST correlationId={} method=GET path={} caller={}",
                correlationId, request.getRequestURI(), caller);

        PagedResponse<Response> result = service.list(
                page, size, sort,
                sourceName, jobStatus, jobType,
                startTimeFrom, startTimeTo);

        log.info("{} correlationId={} caller={} page={} size={} totalElements={} latencyMs={}",
                AuditEvents.BATCH_STATISTICS_LISTED,
                correlationId, caller, page, size,
                result.totalElements(), elapsed(start));

        return ResponseEntity.ok(result);
    }

    // ------------------------------------------------------------------ //
    //  BPS-005: GET by id                                                 //
    // ------------------------------------------------------------------ //

    @GetMapping(path = "/{id}", produces = "application/json")
    @PreAuthorize(Roles.Expr.LIST_VIEWERS)
    @Operation(
        summary     = "Get a single batch job history record by id",
        description = "Emits 'batch.statistics.viewed' audit event on success."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthenticated",
                     content = @Content(schema = @Schema(implementation = ApiProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden",
                     content = @Content(schema = @Schema(implementation = ApiProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Record not found",
                     content = @Content(schema = @Schema(implementation = ApiProblemDetail.class))),
        @ApiResponse(responseCode = "405", description = "Method Not Allowed",
                     content = @Content(schema = @Schema(implementation = ApiProblemDetail.class)))
    })
    public ResponseEntity<Response> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request
    ) {
        long start = System.currentTimeMillis();
        String correlationId = correlationId(request);
        String caller        = subject(jwt);

        log.debug("REQUEST correlationId={} method=GET path={} caller={}",
                correlationId, request.getRequestURI(), caller);

        Response result = service.getById(id);

        log.info("{} correlationId={} id={} caller={} latencyMs={}",
                AuditEvents.BATCH_STATISTICS_VIEWED,
                correlationId, id, caller, elapsed(start));

        return ResponseEntity.ok(result);
    }

    // ------------------------------------------------------------------ //
    //  BPS-006: POST – create                                             //
    // ------------------------------------------------------------------ //

    @PostMapping(consumes = "application/json", produces = "application/json")
    @PreAuthorize(Roles.Expr.BATCH_RUNNERS)
    @Operation(
        summary     = "Create a new batch job history record",
        description = "Called at job registration or start time. " +
                      "Server manages id. endTime is optional (null while job is running). " +
                      "Emits 'batch.statistics.created' audit event on success."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created — Location header points to new record"),
        @ApiResponse(responseCode = "400", description = "Validation failure or server-managed field supplied",
                     content = @Content(schema = @Schema(implementation = ApiProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Unauthenticated",
                     content = @Content(schema = @Schema(implementation = ApiProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden — caller lacks ADD or EDIT role",
                     content = @Content(schema = @Schema(implementation = ApiProblemDetail.class))),
        @ApiResponse(responseCode = "405", description = "Method Not Allowed",
                     content = @Content(schema = @Schema(implementation = ApiProblemDetail.class)))
    })
    public ResponseEntity<Response> create(
            @Valid @RequestBody BatchProcessingStatisticsDto.PostRequestBody body,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request
    ) {
        long start = System.currentTimeMillis();
        String correlationId = correlationId(request);
        String clientId      = resolveClientId(jwt);

        log.debug("REQUEST correlationId={} method=POST path={} caller={}",
                correlationId, request.getRequestURI(), clientId);

        Response created = service.create(body, clientId);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();

        log.info("{} correlationId={} id={} caller={} latencyMs={}",
                AuditEvents.BATCH_STATISTICS_CREATED,
                correlationId, created.getId(), clientId, elapsed(start));

        return ResponseEntity.created(location).body(created);
    }

    // ------------------------------------------------------------------ //
    //  BPS-007: PUT – targeted field replacement                          //
    //  Fields: processName, startTime, endTime (optional), type,          //
    //          recordsGathered, recordsChanged, errorRecords,             //
    //          processedRecords                                           //
    // ------------------------------------------------------------------ //

    @PutMapping(path = "/{id}", consumes = "application/json", produces = "application/json")
    @PreAuthorize(Roles.Expr.BATCH_RUNNERS)
    @Operation(
        summary     = "Update business fields on a batch job history record",
        description = "Replaces: processName, startTime, endTime (optional), type, " +
                      "recordsGathered, recordsChanged, errorRecords, processedRecords. " +
                      "All other fields (sourceSystem, jobStatus, retryCount, etc.) are preserved. " +
                      "Emits 'batch.statistics.updated' audit event on success."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK — updated record returned"),
        @ApiResponse(responseCode = "400", description = "Validation failure",
                     content = @Content(schema = @Schema(implementation = ApiProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Unauthenticated",
                     content = @Content(schema = @Schema(implementation = ApiProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden — caller lacks ADD or EDIT role",
                     content = @Content(schema = @Schema(implementation = ApiProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Record not found",
                     content = @Content(schema = @Schema(implementation = ApiProblemDetail.class))),
        @ApiResponse(responseCode = "405", description = "Method Not Allowed",
                     content = @Content(schema = @Schema(implementation = ApiProblemDetail.class)))
    })
    public ResponseEntity<Response> replace(
            @PathVariable Long id,
            @Valid @RequestBody BatchProcessingStatisticsDto.PutRequestBody body,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request
    ) {
        long start = System.currentTimeMillis();
        String correlationId = correlationId(request);
        String clientId      = resolveClientId(jwt);

        log.debug("REQUEST correlationId={} method=PUT path={} caller={}",
                correlationId, request.getRequestURI(), clientId);

        Response updated = service.replace(id, body, clientId);

        log.info("{} correlationId={} id={} caller={} latencyMs={}",
                AuditEvents.BATCH_STATISTICS_UPDATED,
                correlationId, id, clientId, elapsed(start));

        return ResponseEntity.ok(updated);
    }

    // ------------------------------------------------------------------ //
    //  Private helpers                                                    //
    // ------------------------------------------------------------------ //

    /**
     * Reads the correlation id from the inbound X-Correlation-Id header.
     * Propagated from the calling service; null-safe with a fallback.
     */
    private String correlationId(HttpServletRequest request) {
        String id = request.getHeader(CORRELATION_HEADER);
        return (id != null && !id.isBlank()) ? id : "no-correlation-id";
    }

    private long elapsed(long startMs) {
        return System.currentTimeMillis() - startMs;
    }

    private String subject(Jwt jwt) {
        if (jwt == null) return "anonymous";
        return jwt.getSubject();
    }

    /**
     * Resolves the caller's identity from the JWT.
     * Service principals use 'azp' / 'appid'; human users fall back to 'sub'.
     */
    private String resolveClientId(Jwt jwt) {
        if (jwt == null) return "anonymous";
        String azp = jwt.getClaimAsString(JwtClaims.AZP);
        if (azp != null && !azp.isBlank()) return azp;
        String appid = jwt.getClaimAsString(JwtClaims.APP_ID);
        if (appid != null && !appid.isBlank()) return appid;
        return jwt.getSubject();
    }
}
