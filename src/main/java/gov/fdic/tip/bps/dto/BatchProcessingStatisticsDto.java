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
 * Separate request bodies for POST (BPS-006) and PUT (BPS-007):
 *
 *   PostRequestBody — full job registration payload (all batch_job_history fields)
 *   PutRequestBody  — job completion / correction payload:
 *                     processName, startTime, endTime (optional), type,
 *                     recordsGathered, recordsChanged, errorRecords, processedRecords
 */
public final class BatchProcessingStatisticsDto {

    private BatchProcessingStatisticsDto() {}

    // ------------------------------------------------------------------ //
    //  POST request body — BPS-006                                        //
    //  Full job registration at start time                                //
    // ------------------------------------------------------------------ //

    public record PostRequestBody(

            @NotNull(message = ValidationMessages.SOURCE_SYSTEM_ID_REQUIRED)
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

            /** Optional — null while job is still in progress */
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

            /** Server-managed — must be null/absent in request body */
            Long id
    ) {}

    // ------------------------------------------------------------------ //
    //  PUT request body — BPS-007                                         //
    //  Full replacement: job completion or correction of a posted record  //
    //  Fields: processName, startTime, endTime (optional), type,          //
    //          recordsGathered, recordsChanged, errorRecords,             //
    //          processedRecords                                           //
    // ------------------------------------------------------------------ //

    public record PutRequestBody(

            @NotBlank(message = ValidationMessages.PROCESS_NAME_REQUIRED)
            @Size(min = 1, max = 100, message = ValidationMessages.PROCESS_NAME_SIZE)
            String processName,

            @NotNull(message = ValidationMessages.START_TIME_REQUIRED)
            Instant startTime,

            /** Optional — null indicates the job has not ended. Must be >= startTime when set. */
            Instant endTime,

            @NotBlank(message = ValidationMessages.TYPE_REQUIRED)
            @Size(max = 100, message = ValidationMessages.TYPE_SIZE)
            String type,

            @NotNull(message = ValidationMessages.RECORDS_GATHERED_REQUIRED)
            @Min(value = 0, message = ValidationMessages.RECORDS_GATHERED_MIN)
            Integer recordsGathered,

            @NotNull(message = ValidationMessages.RECORDS_CHANGED_REQUIRED)
            @Min(value = 0, message = ValidationMessages.RECORDS_CHANGED_MIN)
            Integer recordsChanged,

            @NotNull(message = ValidationMessages.ERROR_RECORDS_REQUIRED)
            @Min(value = 0, message = ValidationMessages.ERROR_RECORDS_MIN)
            Integer errorRecords,

            @NotNull(message = ValidationMessages.PROCESSED_RECORDS_REQUIRED)
            @Min(value = 0, message = ValidationMessages.PROCESSED_RECORDS_MIN)
            Integer processedRecords
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
        Instant endTime;        // null while job is in progress (em-dash in UI)
        String  status;
        String  errorMessage;
        Integer recordsChanged;
        Integer recordsGathered;
        Integer recordsProcessedCurrentPeriod;
        Integer recordsProcessedPriorPeriod;
        Integer recordsUnpostable;

        // PUT-updated fields
        String  processName;
        String  type;
        Integer errorRecords;
        Integer processedRecords;
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
