package gov.fdic.tip.bps.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import gov.fdic.tip.bps.config.ApplicationConstants.ValidationMessages;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

/**
 * DTOs for the Batch Processing Statistics API.
 * Backed by batch_job_history + batch_source_system tables.
 *
 * BPS-001, BPS-004, BPS-005, BPS-006, BPS-007
 */
public final class BatchProcessingStatisticsDto {

    private BatchProcessingStatisticsDto() {}

    // ------------------------------------------------------------------ //
    //  Request body: POST (BPS-006) and PUT (BPS-007)                    //
    // ------------------------------------------------------------------ //

    /**
     * Fields accepted in POST / PUT bodies.
     * Server-managed field (id) must NOT be present with a non-null value.
     */
    public record RequestBody(

            /** FK to batch_source_system.id */
            @NotNull(message = "sourceSystemId is required")
            Long sourceSystemId,

            Long jobId,

            @NotBlank(message = ValidationMessages.JOB_TYPE_REQUIRED)
            @Size(max = 100, message = ValidationMessages.JOB_TYPE_SIZE)
            String jobType,

            Integer retryCount,

            @NotBlank(message = ValidationMessages.JOB_STATUS_REQUIRED)
            @Size(max = 50, message = ValidationMessages.JOB_STATUS_SIZE)
            String jobStatus,

            @NotNull(message = ValidationMessages.START_TIME_REQUIRED)
            Instant startTime,

            Instant endTime,

            @NotBlank(message = ValidationMessages.STATUS_REQUIRED)
            @Size(max = 100, message = ValidationMessages.STATUS_SIZE)
            String status,

            @Size(max = 500, message = ValidationMessages.ERROR_MESSAGE_SIZE)
            String errorMessage,

            @Min(value = 0, message = ValidationMessages.RECORDS_CHANGED_MIN)
            Integer recordsChanged,

            @Min(value = 0, message = ValidationMessages.RECORDS_GATHERED_MIN)
            Integer recordsGathered,

            @Min(value = 0, message = ValidationMessages.RECORDS_PROCESSED_CURRENT_MIN)
            Integer recordsProcessedCurrentPeriod,

            @Min(value = 0, message = ValidationMessages.RECORDS_PROCESSED_PRIOR_MIN)
            Integer recordsProcessedPriorPeriod,

            @Min(value = 0, message = ValidationMessages.RECORDS_UNPOSTABLE_MIN)
            Integer recordsUnpostable,

            // Server-managed — must be null/absent in every request body
            Long id
    ) {}

    // ------------------------------------------------------------------ //
    //  Response body (BPS-004, BPS-005)                                  //
    // ------------------------------------------------------------------ //

    @Value
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {

        Long    id;

        // Source system (denormalized for convenience)
        Long    sourceSystemId;
        String  sourceSystemName;

        Long    jobId;
        String  jobType;
        Integer retryCount;
        String  jobStatus;
        Instant startTime;
        Instant endTime;        // null while job is in progress (rendered as em-dash in UI)
        String  status;
        String  errorMessage;
        Integer recordsChanged;
        Integer recordsGathered;
        Integer recordsProcessedCurrentPeriod;
        Integer recordsProcessedPriorPeriod;
        Integer recordsUnpostable;
    }

    // ------------------------------------------------------------------ //
    //  Paginated list response envelope (BPS-004)                        //
    // ------------------------------------------------------------------ //

    public record PagedResponse<T>(
            List<T> content,
            int     page,
            int     size,
            long    totalElements,
            int     totalPages,
            String  sort
    ) {}
}
