package gov.fdic.tip.bps.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import gov.fdic.tip.bps.config.ApplicationConstants.ValidationMessages;
import gov.fdic.tip.bps.entity.BatchType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.time.Instant;
import java.util.List;

/**
 * DTOs for the Batch Processing Statistics API.
 * Backed by batch_job_history + batch_source_system tables.
 *
 * PostRequestBody — full job registration payload (BPS-006)
 * PutRequestBody  — targeted replacement of business fields (BPS-007):
 *                   processName, startTime, endTime (optional), batchType,
 *                   recordsGathered, recordsChanged, errorRecords, processedRecords
 *
 * BPS-001/004/006/007: type field stores EMAIL/API/SFTP_PULL/BATCH enum values.
 * BPS-010: id + 4 audit columns always returned; never accepted in request bodies.
 */
public final class BatchProcessingStatisticsDto {

    private BatchProcessingStatisticsDto() {}

    // ------------------------------------------------------------------ //
    //  POST request body — BPS-006                                        //
    // ------------------------------------------------------------------ //

    public record PostRequestBody(

            @JsonProperty("sourceSystemId")
            @NotNull(message = ValidationMessages.SOURCE_SYSTEM_ID_REQUIRED)
            Long sourceSystemId,

            @JsonProperty("jobId")
            Long jobId,

            @JsonProperty("jobType")
            @NotBlank(message = ValidationMessages.JOB_TYPE_REQUIRED)
            @Size(max = 100, message = ValidationMessages.JOB_TYPE_SIZE)
            String jobType,

            /**
             * Batch type — EMAIL, API, SFTP_PULL, BATCH.
             * Validated via BatchType enum; unknown values return 400.
             */
            @JsonProperty("batchType")
            BatchType batchType,

            @JsonProperty("retryCount")
            Integer retryCount,

            @JsonProperty("jobStatus")
            @NotBlank(message = ValidationMessages.JOB_STATUS_REQUIRED)
            @Size(max = 50, message = ValidationMessages.JOB_STATUS_SIZE)
            String jobStatus,

            @JsonProperty("startTime")
            @NotNull(message = ValidationMessages.START_TIME_REQUIRED)
            Instant startTime,

            /** Optional — null while job is still in progress */
            @JsonProperty("endTime")
            Instant endTime,

            @JsonProperty("status")
            @NotBlank(message = ValidationMessages.STATUS_REQUIRED)
            @Size(max = 100, message = ValidationMessages.STATUS_SIZE)
            String status,

            @JsonProperty("errorMessage")
            @Size(max = 500, message = ValidationMessages.ERROR_MESSAGE_SIZE)
            String errorMessage,

            @JsonProperty("recordsChanged")
            @Min(value = 0, message = ValidationMessages.RECORDS_CHANGED_MIN)
            Integer recordsChanged,

            @JsonProperty("recordsGathered")
            @Min(value = 0, message = ValidationMessages.RECORDS_GATHERED_MIN)
            Integer recordsGathered,

            @JsonProperty("recordsProcessedCurrentPeriod")
            @Min(value = 0, message = ValidationMessages.RECORDS_PROCESSED_CURRENT_MIN)
            Integer recordsProcessedCurrentPeriod,

            @JsonProperty("recordsProcessedPriorPeriod")
            @Min(value = 0, message = ValidationMessages.RECORDS_PROCESSED_PRIOR_MIN)
            Integer recordsProcessedPriorPeriod,

            @JsonProperty("recordsUnpostable")
            @Min(value = 0, message = ValidationMessages.RECORDS_UNPOSTABLE_MIN)
            Integer recordsUnpostable,

            // ---- Server-managed — rejected with 400 if non-null (BPS-010) ----
            @JsonProperty("id")              Long    id,
            @JsonProperty("createdBy")       String  createdBy,
            @JsonProperty("createdDateTime") Instant createdDateTime,
            @JsonProperty("updatedBy")       String  updatedBy,
            @JsonProperty("updatedDateTime") Instant updatedDateTime
    ) {}

    // ------------------------------------------------------------------ //
    //  PUT request body — BPS-007                                         //
    //  Replaces: processName, startTime, endTime (optional), batchType,   //
    //            recordsGathered, recordsChanged, errorRecords,           //
    //            processedRecords                                         //
    // ------------------------------------------------------------------ //

    @Data
    @NoArgsConstructor
    public static class PutRequestBody {

        @NotBlank(message = ValidationMessages.PROCESS_NAME_REQUIRED)
        @Size(min = 1, max = 100, message = ValidationMessages.PROCESS_NAME_SIZE)
        private String processName;

        @NotNull(message = ValidationMessages.START_TIME_REQUIRED)
        private Instant startTime;

        /** Optional — null means the job has not ended. Must be >= startTime when set. */
        private Instant endTime;

        /**
         * Batch type — EMAIL, API, SFTP_PULL, BATCH.
         * Required for PUT. Validated via BatchType enum; unknown values return 400.
         */
        @NotNull(message = ValidationMessages.BATCH_TYPE_REQUIRED)
        private BatchType batchType;

        @NotNull(message = ValidationMessages.RECORDS_GATHERED_REQUIRED)
        @Min(value = 0, message = ValidationMessages.RECORDS_GATHERED_MIN)
        private Integer recordsGathered;

        @NotNull(message = ValidationMessages.RECORDS_CHANGED_REQUIRED)
        @Min(value = 0, message = ValidationMessages.RECORDS_CHANGED_MIN)
        private Integer recordsChanged;

        @NotNull(message = ValidationMessages.ERROR_RECORDS_REQUIRED)
        @Min(value = 0, message = ValidationMessages.ERROR_RECORDS_MIN)
        private Integer errorRecords;

        @NotNull(message = ValidationMessages.PROCESSED_RECORDS_REQUIRED)
        @Min(value = 0, message = ValidationMessages.PROCESSED_RECORDS_MIN)
        private Integer processedRecords;

        @JsonCreator
        public PutRequestBody(
                @JsonProperty("processName")      String    processName,
                @JsonProperty("startTime")        Instant   startTime,
                @JsonProperty("endTime")          Instant   endTime,
                @JsonProperty("batchType")        BatchType batchType,
                @JsonProperty("recordsGathered")  Integer   recordsGathered,
                @JsonProperty("recordsChanged")   Integer   recordsChanged,
                @JsonProperty("errorRecords")     Integer   errorRecords,
                @JsonProperty("processedRecords") Integer   processedRecords
        ) {
            this.processName     = processName;
            this.startTime       = startTime;
            this.endTime         = endTime;
            this.batchType       = batchType;
            this.recordsGathered  = recordsGathered;
            this.recordsChanged   = recordsChanged;
            this.errorRecords     = errorRecords;
            this.processedRecords = processedRecords;
        }
    }

    // ------------------------------------------------------------------ //
    //  Response body (BPS-004, BPS-005)                                   //
    //                                                                     //
    //  BPS-001: batchType is returned as stored value (EMAIL etc.);       //
    //           UI maps to label via BatchType.getLabel().                //
    //  BPS-010: id + 4 audit columns always returned.                    //
    // ------------------------------------------------------------------ //

    @Value
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {

        // Server-managed — always returned (BPS-010)
        Long      id;
        String    createdBy;
        Instant   createdDateTime;
        String    updatedBy;
        Instant   updatedDateTime;

        // Source system
        Long      sourceSystemId;
        String    sourceSystemName;

        // Job fields
        Long      jobId;
        String    jobType;
        /**
         * Batch type enum — serialised as stored value (EMAIL, API, SFTP_PULL, BATCH).
         * UI consumers should map to the label via BatchType.getLabel().
         */
        BatchType batchType;
        Integer   retryCount;
        String    jobStatus;
        Instant   startTime;
        Instant   endTime;
        String    status;
        String    errorMessage;

        // Record counts
        Integer   recordsChanged;
        Integer   recordsGathered;
        Integer   recordsProcessedCurrentPeriod;
        Integer   recordsProcessedPriorPeriod;
        Integer   recordsUnpostable;

        // PUT-targeted business fields
        String    processName;
        Integer   errorRecords;
        Integer   processedRecords;
    }

    // ------------------------------------------------------------------ //
    //  Paginated list response envelope (BPS-004)                         //
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
