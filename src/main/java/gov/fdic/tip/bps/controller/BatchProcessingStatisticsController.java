package gov.fdic.tip.bps.controller;

import gov.fdic.tip.bps.config.ApplicationConstants.ApiPaths;
import gov.fdic.tip.bps.config.ApplicationConstants.JwtClaims;
import gov.fdic.tip.bps.config.ApplicationConstants.Pagination;
import gov.fdic.tip.bps.config.ApplicationConstants.SortFields;
import gov.fdic.tip.bps.dto.BatchProcessingStatisticsDto;
import gov.fdic.tip.bps.dto.BatchProcessingStatisticsDto.PagedResponse;
import gov.fdic.tip.bps.dto.BatchProcessingStatisticsDto.Response;
import gov.fdic.tip.bps.service.BatchProcessingStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 *   GET    /api/v1/batch-processing-statistics          – list
 *   GET    /api/v1/batch-processing-statistics/{id}     – get by id
 *   POST   /api/v1/batch-processing-statistics          – create
 *   PUT    /api/v1/batch-processing-statistics/{id}     – full replace
 *
 * DELETE and PATCH return 405 via @ControllerAdvice (BPS-008).
 *
 * Authorization (BPS-003, BPS-009):
 *   GET  → Admin | Manager | Sr. Analyst | Analyst
 *   POST → Batch Runner (service principal only)
 *   PUT  → Batch Runner (service principal only)
 */
@Slf4j
@RestController
@RequestMapping(ApiPaths.BPS_V1_BASE)
@RequiredArgsConstructor
@Tag(name = "Batch Processing Statistics", description = "BPS-001 through BPS-010")
public class BatchProcessingStatisticsController {

    private final BatchProcessingStatisticsService service;

    // ------------------------------------------------------------------ //
    //  BPS-004: GET list                                                  //
    // ------------------------------------------------------------------ //

    @GetMapping(produces = "application/json")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SR_ANALYST','ANALYST')")
    @Operation(summary = "Get paginated list of batch job history records (BPS-004)")
    public ResponseEntity<PagedResponse<Response>> list(
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_PAGE)  int    page,
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(defaultValue = SortFields.DEFAULT_SORT)       String sort,
            @RequestParam(required = false) Long    sourceSystemId,
            @RequestParam(required = false) String  jobStatus,
            @RequestParam(required = false) String  jobType,
            @RequestParam(required = false) Instant startTimeFrom,
            @RequestParam(required = false) Instant startTimeTo,
            @AuthenticationPrincipal Jwt jwt
    ) {
        log.info("{} caller={} page={} size={} sort={}",
                gov.fdic.tip.bps.config.ApplicationConstants.AuditEvents.BATCH_STATISTICS_LISTED,
                jwt.getSubject(), page, size, sort);

        return ResponseEntity.ok(
                service.list(page, size, sort,
                        sourceSystemId, jobStatus, jobType,
                        startTimeFrom, startTimeTo));
    }

    // ------------------------------------------------------------------ //
    //  BPS-005: GET by id                                                 //
    // ------------------------------------------------------------------ //

    @GetMapping(path = "/{id}", produces = "application/json")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SR_ANALYST','ANALYST')")
    @Operation(summary = "Get a single batch job history record by id (BPS-005)")
    public ResponseEntity<Response> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        log.info("{} id={} caller={}",
                gov.fdic.tip.bps.config.ApplicationConstants.AuditEvents.BATCH_STATISTICS_VIEWED,
                id, jwt.getSubject());
        return ResponseEntity.ok(service.getById(id));
    }

    // ------------------------------------------------------------------ //
    //  BPS-006: POST – create                                             //
    // ------------------------------------------------------------------ //

    @PostMapping(consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasRole('BATCH_RUNNER')")
    @Operation(summary = "Create a new batch job history record (BPS-006)")
    public ResponseEntity<Response> create(
            @Valid @RequestBody BatchProcessingStatisticsDto.RequestBody body,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Response created = service.create(body);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();

        log.info("{} id={} caller={}",
                gov.fdic.tip.bps.config.ApplicationConstants.AuditEvents.BATCH_STATISTICS_CREATED,
                created.getId(), resolveClientId(jwt));

        return ResponseEntity.created(location).body(created);
    }

    // ------------------------------------------------------------------ //
    //  BPS-007: PUT – full replace                                        //
    // ------------------------------------------------------------------ //

    @PutMapping(path = "/{id}", consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasRole('BATCH_RUNNER')")
    @Operation(summary = "Fully replace a batch job history record (BPS-007)")
    public ResponseEntity<Response> replace(
            @PathVariable Long id,
            @Valid @RequestBody BatchProcessingStatisticsDto.RequestBody body,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Response updated = service.replace(id, body);

        log.info("{} id={} caller={}",
                gov.fdic.tip.bps.config.ApplicationConstants.AuditEvents.BATCH_STATISTICS_UPDATED,
                id, resolveClientId(jwt));

        return ResponseEntity.ok(updated);
    }

    // ------------------------------------------------------------------ //
    //  Helper                                                             //
    // ------------------------------------------------------------------ //

    private String resolveClientId(Jwt jwt) {
        String azp = jwt.getClaimAsString(JwtClaims.AZP);
        if (azp != null && !azp.isBlank()) return azp;
        String appid = jwt.getClaimAsString(JwtClaims.APP_ID);
        if (appid != null && !appid.isBlank()) return appid;
        return jwt.getSubject();
    }
}
