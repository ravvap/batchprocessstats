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
 * Backed by batch_job_history + batch_source_system.
 *
 * Filters: sourceName (LIKE on source_name), jobStatus, jobType, startTimeFrom/To.
 * Sort:    sourceName maps to "sourceSystem.sourceName" (Spring Data JPA joined path).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchProcessingStatisticsService {

    /**
     * User-facing sort key → actual JPA field path.
     * sourceName is on the joined BatchSourceSystem entity, so its path must be
     * "sourceSystem.sourceName" for Spring Data JPA to resolve it correctly.
     */
    private static final Map<String, String> SORT_FIELD_MAP = Map.of(
            SortFields.SOURCE_NAME,                SortFields.SOURCE_NAME_JPA_PATH,
            SortFields.START_TIME,                 SortFields.START_TIME,
            SortFields.END_TIME,                   SortFields.END_TIME,
            SortFields.JOB_TYPE,                   SortFields.JOB_TYPE,
            SortFields.JOB_STATUS,                 SortFields.JOB_STATUS,
            SortFields.STATUS,                     SortFields.STATUS,
            SortFields.RECORDS_GATHERED,           SortFields.RECORDS_GATHERED,
            SortFields.RECORDS_CHANGED,            SortFields.RECORDS_CHANGED,
            SortFields.RECORDS_PROCESSED_CURRENT,  SortFields.RECORDS_PROCESSED_CURRENT,
            SortFields.RECORDS_PROCESSED_PRIOR,    SortFields.RECORDS_PROCESSED_PRIOR
    );

    private final BatchJobHistoryRepository   jobHistoryRepository;
    private final BatchSourceSystemRepository sourceSystemRepository;

    // ------------------------------------------------------------------ //
    //  BPS-004: GET paginated, filterable list                            //
    // ------------------------------------------------------------------ //

    /**
     * @param sourceName  case-insensitive substring filter on batch_source_system.source_name
     * @param jobStatus   exact match filter on job_status
     * @param jobType     case-insensitive substring filter on job_type
     * @param from        inclusive UTC lower bound on start_time
     * @param to          inclusive UTC upper bound on start_time
     */
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

    @Transactional
    public Response create(BatchProcessingStatisticsDto.RequestBody body) {
        validateNoServerManagedFields(body);
        validateEndTimeNotBeforeStart(body.startTime(), body.endTime());

        BatchSourceSystem sourceSystem = resolveSourceSystem(body.sourceSystemId());
        BatchJobHistory entity = new BatchJobHistory();
        applyFields(entity, body, sourceSystem);

        BatchJobHistory saved = jobHistoryRepository.save(entity);
        log.info("{} id={} jobType={} sourceName={}",
                AuditEvents.BATCH_STATISTICS_CREATED,
                saved.getId(), saved.getJobType(),
                saved.getSourceSystem().getSourceName());
        return toResponse(saved);
    }

    // ------------------------------------------------------------------ //
    //  BPS-007: PUT – full replace                                        //
    // ------------------------------------------------------------------ //

    @Transactional
    public Response replace(Long id, BatchProcessingStatisticsDto.RequestBody body) {
        validateNoServerManagedFields(body);
        validateEndTimeNotBeforeStart(body.startTime(), body.endTime());

        BatchJobHistory entity = jobHistoryRepository.findById(id)
                .orElseThrow(() -> new BatchStatisticsNotFoundException(id));

        BatchSourceSystem sourceSystem = resolveSourceSystem(body.sourceSystemId());
        applyFields(entity, body, sourceSystem);

        BatchJobHistory saved = jobHistoryRepository.save(entity);
        log.info("{} id={}", AuditEvents.BATCH_STATISTICS_UPDATED, saved.getId());
        return toResponse(saved);
    }

    // ------------------------------------------------------------------ //
    //  Private helpers                                                    //
    // ------------------------------------------------------------------ //

    private BatchSourceSystem resolveSourceSystem(Long sourceSystemId) {
        return sourceSystemRepository.findById(sourceSystemId)
                .orElseThrow(() -> new SourceSystemNotFoundException(sourceSystemId));
    }

    private void validateNoServerManagedFields(BatchProcessingStatisticsDto.RequestBody body) {
        List<String> offenders = new ArrayList<>();
        if (body.id() != null) offenders.add("id");
        if (!offenders.isEmpty()) throw new ServerManagedFieldException(offenders);
    }

    private void validateEndTimeNotBeforeStart(Instant start, Instant end) {
        if (end != null && end.isBefore(start)) {
            throw new IllegalArgumentException(ErrorMessages.END_TIME_BEFORE_START);
        }
    }

    private void applyFields(BatchJobHistory entity,
                             BatchProcessingStatisticsDto.RequestBody body,
                             BatchSourceSystem sourceSystem) {
        entity.setSourceSystem(sourceSystem);
        entity.setJobId(body.jobId());
        entity.setJobType(body.jobType());
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

    private Response toResponse(BatchJobHistory e) {
        return Response.builder()
                .id(e.getId())
                .sourceSystemId(e.getSourceSystem().getId())
                .sourceSystemName(e.getSourceSystem().getSourceName())
                .jobId(e.getJobId())
                .jobType(e.getJobType())
                .retryCount(e.getRetryCount())
                .jobStatus(e.getJobStatus())
                .startTime(e.getStartTime())
                .endTime(e.getEndTime())
                .status(e.getStatus())
                .errorMessage(e.getErrorMessage())
                .recordsChanged(e.getRecordsChanged())
                .recordsGathered(e.getRecordsGathered())
                .recordsProcessedCurrentPeriod(e.getRecordsProcessedCurrentPeriod())
                .recordsProcessedPriorPeriod(e.getRecordsProcessedPriorPeriod())
                .recordsUnpostable(e.getRecordsUnpostable())
                .build();
    }

    /**
     * Parses "field,direction" sort param and builds a Pageable.
     *
     * "sourceName" is mapped to "sourceSystem.sourceName" — Spring Data JPA
     * resolves dotted paths across associations for Sort when used with
     * JpaSpecificationExecutor.
     */
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

        // Map user-facing field name → JPA field path
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
