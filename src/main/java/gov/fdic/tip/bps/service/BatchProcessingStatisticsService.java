package gov.fdic.tip.bps.service;

import gov.fdic.tip.bps.config.ApplicationConstants.AuditEvents;
import gov.fdic.tip.bps.config.ApplicationConstants.ErrorMessages;
import gov.fdic.tip.bps.config.ApplicationConstants.Pagination;
import gov.fdic.tip.bps.config.ApplicationConstants.SortFields;
import gov.fdic.tip.bps.dto.BatchProcessingStatisticsDto;
import gov.fdic.tip.bps.dto.BatchProcessingStatisticsDto.PagedResponse;
import gov.fdic.tip.bps.dto.BatchProcessingStatisticsDto.Response;
import gov.fdic.tip.bps.entity.BatchJobHistory;
import gov.fdic.tip.bps.entity.BatchSourceSystem;
import gov.fdic.tip.bps.exception.BatchStatisticsNotFoundException;
import gov.fdic.tip.bps.exception.ServerManagedFieldException;
import gov.fdic.tip.bps.exception.SourceSystemNotFoundException;
import gov.fdic.tip.bps.repository.BatchJobHistoryRepository;
import gov.fdic.tip.bps.repository.BatchJobHistorySpecification;
import gov.fdic.tip.bps.repository.BatchSourceSystemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service layer for Batch Processing Statistics (BPS-004 through BPS-007).
 *
 * BPS-010 audit columns requirement:
 *   id, createdBy, createdDateTime, updatedBy, updatedDateTime are server-managed.
 *   - POST: createdBy and updatedBy are set from the caller's identity.
 *           createdDateTime and updatedDateTime are set by @CreationTimestamp / @UpdateTimestamp.
 *   - PUT:  updatedBy is refreshed from caller identity; other audit fields are preserved.
 *   - GET:  all four audit columns are always included in the Response.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchProcessingStatisticsService {

    private static final Map<String, String> SORT_FIELD_MAP = Map.of(
            SortFields.SOURCE_NAME,               SortFields.SOURCE_NAME_JPA_PATH,
            SortFields.START_TIME,                SortFields.START_TIME,
            SortFields.END_TIME,                  SortFields.END_TIME,
            SortFields.JOB_TYPE,                  SortFields.JOB_TYPE,
            SortFields.JOB_STATUS,                SortFields.JOB_STATUS,
            SortFields.STATUS,                    SortFields.STATUS,
            SortFields.RECORDS_GATHERED,          SortFields.RECORDS_GATHERED,
            SortFields.RECORDS_CHANGED,           SortFields.RECORDS_CHANGED,
            SortFields.RECORDS_PROCESSED_CURRENT, SortFields.RECORDS_PROCESSED_CURRENT,
            SortFields.RECORDS_PROCESSED_PRIOR,   SortFields.RECORDS_PROCESSED_PRIOR
    );

    private final BatchJobHistoryRepository   jobHistoryRepository;
    private final BatchSourceSystemRepository sourceSystemRepository;

    // ------------------------------------------------------------------ //
    //  BPS-004: GET paginated, filterable list                            //
    // ------------------------------------------------------------------ //

    @Transactional(readOnly = true)
    public PagedResponse<Response> list(
            int page, int size, String sortParam,
            String sourceName, String jobStatus, String jobType,
            Instant from, Instant to) {

        Pageable pageable = buildPageable(page, size, sortParam);

        Page<BatchJobHistory> resultPage = jobHistoryRepository.findAll(
                BatchJobHistorySpecification.withFilters(
                        sourceName, jobStatus, jobType, from, to),
                pageable);

        List<Response> content = resultPage.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        log.info("{} totalElements={} page={} size={}",
                AuditEvents.BATCH_STATISTICS_LISTED,
                resultPage.getTotalElements(), page, size);

        return new PagedResponse<>(
                content,
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements(),
                resultPage.getTotalPages(),
                sortParam);
    }

    // ------------------------------------------------------------------ //
    //  BPS-005: GET single record by id                                   //
    // ------------------------------------------------------------------ //

    @Transactional(readOnly = true)
    public Response getById(Long id) {
        BatchJobHistory entity = jobHistoryRepository.findById(id)
                .orElseThrow(() -> new BatchStatisticsNotFoundException(id));
        log.info("{} id={}", AuditEvents.BATCH_STATISTICS_VIEWED, id);
        return toResponse(entity);
    }

    // ------------------------------------------------------------------ //
    //  BPS-006: POST – create                                             //
    // ------------------------------------------------------------------ //

    /**
     * @param callerIdentity  resolved from JWT (azp / appid / sub) — stored as createdBy / updatedBy
     */
    @Transactional
    public Response create(BatchProcessingStatisticsDto.PostRequestBody body,
                           String callerIdentity) {
        validateNoServerManagedFields(body);
        validateEndTimeNotBeforeStart(body.startTime(), body.endTime());

        BatchSourceSystem sourceSystem = resolveSourceSystem(body.sourceSystemId());
        BatchJobHistory entity = new BatchJobHistory();
        applyPostFields(entity, body, sourceSystem);

        // Audit columns — server-managed
        entity.setCreatedBy(callerIdentity);
        entity.setUpdatedBy(callerIdentity);

        BatchJobHistory saved = jobHistoryRepository.save(entity);
        log.info("{} id={} jobType={} sourceName={} caller={}",
                AuditEvents.BATCH_STATISTICS_CREATED,
                saved.getId(), saved.getJobType(),
                saved.getSourceSystem().getSourceName(), callerIdentity);
        return toResponse(saved);
    }

    // ------------------------------------------------------------------ //
    //  BPS-007: PUT – targeted field replacement                          //
    // ------------------------------------------------------------------ //

    /**
     * @param callerIdentity  resolved from JWT — updates updatedBy audit column
     */
    @Transactional
    public Response replace(Long id,
                            BatchProcessingStatisticsDto.PutRequestBody body,
                            String callerIdentity) {
        validateEndTimeNotBeforeStart(body.getStartTime(), body.getEndTime());

        BatchJobHistory entity = jobHistoryRepository.findById(id)
                .orElseThrow(() -> new BatchStatisticsNotFoundException(id));

        applyPutFields(entity, body);
        // createdBy / createdDateTime preserved by JPA (updatable = false)
        entity.setUpdatedBy(callerIdentity);

        BatchJobHistory saved = jobHistoryRepository.save(entity);
        log.info("{} id={} caller={}", AuditEvents.BATCH_STATISTICS_UPDATED,
                saved.getId(), callerIdentity);
        return toResponse(saved);
    }

    // ------------------------------------------------------------------ //
    //  Private helpers                                                    //
    // ------------------------------------------------------------------ //

    private BatchSourceSystem resolveSourceSystem(Long sourceSystemId) {
        return sourceSystemRepository.findById(sourceSystemId)
                .orElseThrow(() -> new SourceSystemNotFoundException(sourceSystemId));
    }

    /**
     * Guards against non-null server-managed fields in POST body (BPS-010).
     * Rejected fields: id, createdBy, createdDateTime, updatedBy, updatedDateTime.
     */
    private void validateNoServerManagedFields(
            BatchProcessingStatisticsDto.PostRequestBody body) {
        List<String> offenders = new ArrayList<>();
        if (body.id()              != null) offenders.add("id");
        if (body.createdBy()       != null) offenders.add("createdBy");
        if (body.createdDateTime() != null) offenders.add("createdDateTime");
        if (body.updatedBy()       != null) offenders.add("updatedBy");
        if (body.updatedDateTime() != null) offenders.add("updatedDateTime");
        if (!offenders.isEmpty()) throw new ServerManagedFieldException(offenders);
    }

    private void validateEndTimeNotBeforeStart(Instant start, Instant end) {
        if (end != null && end.isBefore(start)) {
            throw new IllegalArgumentException(ErrorMessages.END_TIME_BEFORE_START);
        }
    }

    private void applyPostFields(BatchJobHistory entity,
                                 BatchProcessingStatisticsDto.PostRequestBody body,
                                 BatchSourceSystem sourceSystem) {
        entity.setSourceSystem(sourceSystem);
        entity.setJobId(body.jobId());
        entity.setJobType(body.jobType());
        entity.setBatchType(body.batchType());
        entity.setRetryCount(body.retryCount());
        entity.setJobStatus(body.jobStatus());
        entity.setStartTime(body.startTime());
        entity.setEndTime(body.endTime());
        entity.setStatus(body.status());
        entity.setErrorMessage(body.errorMessage());
        entity.setRecordsChanged(body.recordsChanged());
        entity.setRecordsGathered(body.recordsGathered());
        entity.setRecordsProcessedCurrentPeriod(body.recordsProcessedCurrentPeriod());
        entity.setRecordsProcessedPriorPeriod(body.recordsProcessedPriorPeriod());
        entity.setRecordsUnpostable(body.recordsUnpostable());
    }

    private void applyPutFields(BatchJobHistory entity,
                                BatchProcessingStatisticsDto.PutRequestBody body) {
        entity.setJobType(body.getProcessName());
        entity.setStartTime(body.getStartTime());
        entity.setEndTime(body.getEndTime());
        entity.setBatchType(body.getBatchType());
        entity.setRecordsGathered(body.getRecordsGathered());
        entity.setRecordsChanged(body.getRecordsChanged());
        entity.setRecordsUnpostable(body.getErrorRecords());
        entity.setRecordsProcessedCurrentPeriod(body.getProcessedRecords());
    }

    /**
     * Maps entity → Response DTO.
     * Audit fields (id, createdBy, createdDateTime, updatedBy, updatedDateTime)
     * are always included per BPS-010.
     */
    private Response toResponse(BatchJobHistory e) {
        return Response.builder()
                // Server-managed — always returned
                .id(e.getId())
                .createdBy(e.getCreatedBy())
                .createdDateTime(e.getCreatedDateTime())
                .updatedBy(e.getUpdatedBy())
                .updatedDateTime(e.getUpdatedDateTime())
                // Source system
                .sourceSystemId(e.getSourceSystem().getId())
                .sourceSystemName(e.getSourceSystem().getSourceName())
                // Job fields
                .jobId(e.getJobId())
                .jobType(e.getJobType())
                .batchType(e.getBatchType())
                .retryCount(e.getRetryCount())
                .jobStatus(e.getJobStatus())
                .startTime(e.getStartTime())
                .endTime(e.getEndTime())
                .status(e.getStatus())
                .errorMessage(e.getErrorMessage())
                // Record counts
                .recordsChanged(e.getRecordsChanged())
                .recordsGathered(e.getRecordsGathered())
                .recordsProcessedCurrentPeriod(e.getRecordsProcessedCurrentPeriod())
                .recordsProcessedPriorPeriod(e.getRecordsProcessedPriorPeriod())
                .recordsUnpostable(e.getRecordsUnpostable())
                .build();
    }

    private Pageable buildPageable(int page, int size, String sortParam) {
        int clampedSize = Math.min(
                Math.max(size, Pagination.MIN_PAGE_SIZE),
                Pagination.MAX_PAGE_SIZE);

        String[] parts = (sortParam != null ? sortParam : SortFields.DEFAULT_SORT).split(",");
        if (parts.length != 2) {
            throw new IllegalArgumentException(ErrorMessages.SORT_FORMAT_INVALID);
        }

        String userField = parts[0].trim();
        String direction = parts[1].trim();

        String jpaField = SORT_FIELD_MAP.get(userField);
        if (jpaField == null) {
            throw new IllegalArgumentException(ErrorMessages.SORT_FIELD_UNKNOWN + userField);
        }

        Sort.Direction dir;
        try {
            dir = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(ErrorMessages.SORT_DIRECTION_INVALID);
        }

        return PageRequest.of(page, clampedSize, Sort.by(dir, jpaField));
    }
}
