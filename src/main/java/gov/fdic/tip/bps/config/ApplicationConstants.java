package gov.fdic.tip.bps.config;

/**
 * Central repository of all application-wide constants.
 */
public final class ApplicationConstants {

    private ApplicationConstants() {}

    // ====================================================================== //
    //  Roles — Entra JWT claim values (as they appear in the 'roles' claim)  //
    // ====================================================================== //
    public static final class EntraRoles {
        private EntraRoles() {}
        public static final String BATCH_PRCS_STATS_VIEW  = "BATCH_PRCS_STATS_VIEW";
        public static final String BATCH_PRCS_STATS_ADD   = "BATCH_PRCS_STATS_ADD";
        public static final String BATCH_PRCS_STATS_EDIT  = "BATCH_PRCS_STATS_EDIT";
    }

    // ====================================================================== //
    //  Roles — Spring Security ROLE_ strings                                 //
    // ====================================================================== //
    public static final class Roles {
        private Roles() {}

        public static final String BATCH_PRCS_STATS_VIEW  = "BATCH_PRCS_STATS_VIEW";
        public static final String BATCH_PRCS_STATS_ADD   = "BATCH_PRCS_STATS_ADD";
        public static final String BATCH_PRCS_STATS_EDIT  = "BATCH_PRCS_STATS_EDIT";

        // A single shared Batch Runner service principal is used by all
        // upstream runners; finer-grained attribution is out of scope.

        /** SpEL expressions — used directly in @PreAuthorize */
        public static final class Expr {
            private Expr() {}

            public static final String LIST_VIEWERS =
                    "hasAnyRole('" + BATCH_PRCS_STATS_VIEW + "')";

            public static final String BATCH_RUNNERS =
                    "hasAnyRole('" + BATCH_PRCS_STATS_ADD + "','" + BATCH_PRCS_STATS_EDIT + "')";
        }
    }

    // ====================================================================== //
    //  Job status values (batch_job_history.job_status)                      //
    // ====================================================================== //
    public static final class JobStatus {
        private JobStatus() {}
        public static final String PENDING   = "PENDING";
        public static final String RUNNING   = "RUNNING";
        public static final String SUCCESS   = "SUCCESS";
        public static final String FAILED    = "FAILED";
        public static final String RETRYING  = "RETRYING";
        public static final String CANCELLED = "CANCELLED";
    }

    // ====================================================================== //
    //  Source system seed names (batch_source_system.source_name)            //
    // ====================================================================== //
    public static final class SourceSystem {
        private SourceSystem() {}
        public static final String SIMS = "SIMS";
        public static final String RRPS = "RRPS";
        public static final String CBIS = "CBIS";
    }

    // ====================================================================== //
    //  Pagination defaults (BPS-004)                                         //
    // ====================================================================== //
    public static final class Pagination {
        private Pagination() {}
        public static final int DEFAULT_PAGE      = 0;
        public static final int DEFAULT_PAGE_SIZE = 25;
        public static final int MAX_PAGE_SIZE     = 100;
        public static final int MIN_PAGE_SIZE     = 1;
        public static final int[] ALLOWED_PAGE_SIZES = {10, 25, 50, 100};
    }

    // ====================================================================== //
    //  Sortable field names for batch_job_history (BPS-004)                  //
    // ====================================================================== //
    public static final class SortFields {
        private SortFields() {}

        /** User-facing sort key → used in API query param */
        public static final String SOURCE_NAME                     = "sourceName";
        public static final String START_TIME                      = "startTime";
        public static final String END_TIME                        = "endTime";
        public static final String JOB_TYPE                        = "jobType";
        public static final String JOB_STATUS                      = "jobStatus";
        public static final String STATUS                          = "status";
        public static final String RECORDS_GATHERED                = "recordsGathered";
        public static final String RECORDS_CHANGED                 = "recordsChanged";
        public static final String RECORDS_PROCESSED_CURRENT       = "recordsProcessedCurrentPeriod";
        public static final String RECORDS_PROCESSED_PRIOR         = "recordsProcessedPriorPeriod";
        public static final String RECORDS_UNPOSTABLE              = "recordsUnpostable";

        /** Internal JPA path for sourceName — joined field on BatchSourceSystem */
        public static final String SOURCE_NAME_JPA_PATH            = "sourceSystem.sourceName";

        public static final String DEFAULT_SORT                    = START_TIME + ",desc";
    }

    // ====================================================================== //
    //  Splunk audit event type names (BPS-010)                               //
    // ====================================================================== //
    public static final class AuditEvents {
        private AuditEvents() {}
        public static final String BATCH_STATISTICS_LISTED          = "batch.statistics.listed";
        public static final String BATCH_STATISTICS_VIEWED          = "batch.statistics.viewed";
        public static final String BATCH_STATISTICS_CREATED         = "batch.statistics.created";
        public static final String BATCH_STATISTICS_UPDATED         = "batch.statistics.updated";
        public static final String BATCH_STATISTICS_ACCESS_DENIED   = "batch.statistics.access_denied";
        public static final String BATCH_STATISTICS_METHOD_REJECTED = "batch.statistics.method_rejected";
        public static final String BATCH_STATISTICS_UI_ACCESS_GRANTED = "batch.statistics.ui.access_granted";
        public static final String BATCH_STATISTICS_UI_ACCESS_DENIED  = "batch.statistics.ui.access_denied";
        public static final String REASON_UNAUTHENTICATED = "unauthenticated";
        public static final String REASON_UNAUTHORIZED    = "unauthorized";
    }

    // ====================================================================== //
    //  Problem+JSON type URIs (RFC 7807)                                     //
    // ====================================================================== //
    public static final class ProblemType {
        private ProblemType() {}
        private static final String BASE = "https://tip.fdic.gov/problems/";
        public static final String VALIDATION_ERROR      = BASE + "validation-error";
        public static final String SERVER_MANAGED_FIELD  = BASE + "server-managed-field";
        public static final String BAD_REQUEST           = BASE + "bad-request";
        public static final String NOT_FOUND             = BASE + "not-found";
        public static final String UNAUTHORIZED          = BASE + "unauthorized";
        public static final String FORBIDDEN             = BASE + "forbidden";
        public static final String METHOD_NOT_ALLOWED    = BASE + "method-not-allowed";
        public static final String INTERNAL_SERVER_ERROR = BASE + "internal-server-error";
    }

    // ====================================================================== //
    //  Problem+JSON title strings                                            //
    // ====================================================================== //
    public static final class ProblemTitle {
        private ProblemTitle() {}
        public static final String VALIDATION_FAILED     = "Validation Failed";
        public static final String SERVER_MANAGED_FIELD  = "Server-Managed Field in Request";
        public static final String BAD_REQUEST           = "Bad Request";
        public static final String NOT_FOUND             = "Record Not Found";
        public static final String UNAUTHORIZED          = "Unauthorized";
        public static final String FORBIDDEN             = "Forbidden";
        public static final String METHOD_NOT_ALLOWED    = "Method Not Allowed";
        public static final String INTERNAL_SERVER_ERROR = "Internal Server Error";
    }

    // ====================================================================== //
    //  Error detail messages                                                 //
    // ====================================================================== //
    public static final class ErrorMessages {
        private ErrorMessages() {}
        public static final String VALIDATION_FAILED         = "One or more fields failed validation.";
        public static final String SERVER_MANAGED_FIELD      = "Server-managed field must not be supplied in the request body.";
        public static final String BEARER_TOKEN_REQUIRED     = "A valid Bearer token is required.";
        public static final String FORBIDDEN                 = "You do not have permission to perform this operation.";
        public static final String INTERNAL_SERVER_ERROR     = "An unexpected error occurred. Please try again later.";
        public static final String RECORD_NOT_FOUND          = "Batch job history record not found: ";
        public static final String SOURCE_SYSTEM_NOT_FOUND   = "Batch source system not found with id: ";
        public static final String SORT_FIELD_UNKNOWN        = "Unknown sort field: ";
        public static final String SORT_FORMAT_INVALID       = "Sort parameter must be 'field,direction'. Multi-field sort is not supported.";
        public static final String SORT_DIRECTION_INVALID    = "Sort direction must be 'asc' or 'desc'.";
        public static final String END_TIME_BEFORE_START     = "endTime must be >= startTime.";
        public static final String METHOD_NOT_SUPPORTED_PREFIX = "HTTP method ";
        public static final String METHOD_NOT_SUPPORTED_SUFFIX = " is not supported for this resource. Supported methods: ";
    }

    // ====================================================================== //
    //  Validation constraint messages                                        //
    // ====================================================================== //
    public static final class ValidationMessages {
        private ValidationMessages() {}

        // POST fields
        public static final String SOURCE_SYSTEM_ID_REQUIRED       = "sourceSystemId is required";
        public static final String JOB_TYPE_REQUIRED               = "jobType is required";
        public static final String JOB_TYPE_SIZE                   = "jobType must be <= 100 characters";
        public static final String JOB_STATUS_REQUIRED             = "jobStatus is required";
        public static final String JOB_STATUS_SIZE                 = "jobStatus must be <= 50 characters";
        public static final String START_TIME_REQUIRED             = "startTime is required";
        public static final String STATUS_REQUIRED                 = "status is required";
        public static final String STATUS_SIZE                     = "status must be <= 100 characters";
        public static final String ERROR_MESSAGE_SIZE              = "errorMessage must be <= 500 characters";
        public static final String RECORDS_CHANGED_MIN             = "recordsChanged must be >= 0";
        public static final String RECORDS_GATHERED_MIN            = "recordsGathered must be >= 0";
        public static final String RECORDS_PROCESSED_CURRENT_MIN   = "recordsProcessedCurrentPeriod must be >= 0";
        public static final String RECORDS_PROCESSED_PRIOR_MIN     = "recordsProcessedPriorPeriod must be >= 0";
        public static final String RECORDS_UNPOSTABLE_MIN          = "recordsUnpostable must be >= 0";

        // PUT fields (BPS-007)
        public static final String PROCESS_NAME_REQUIRED           = "processName is required";
        public static final String PROCESS_NAME_SIZE               = "processName must be 1–100 characters";
        public static final String BATCH_TYPE_REQUIRED             = "batchType is required; allowed values: EMAIL, API, SFTP_PULL, BATCH";
        public static final String TYPE_REQUIRED                   = "type is required";
        public static final String TYPE_SIZE                       = "type must be <= 100 characters";
        public static final String RECORDS_GATHERED_REQUIRED       = "recordsGathered is required";
        public static final String RECORDS_CHANGED_REQUIRED        = "recordsChanged is required";
        public static final String ERROR_RECORDS_REQUIRED          = "errorRecords is required";
        public static final String ERROR_RECORDS_MIN               = "errorRecords must be >= 0";
        public static final String PROCESSED_RECORDS_REQUIRED      = "processedRecords is required";
        public static final String PROCESSED_RECORDS_MIN           = "processedRecords must be >= 0";
    }

    // ====================================================================== //
    //  API path constants                                                    //
    // ====================================================================== //
    public static final class ApiPaths {
        private ApiPaths() {}
        public static final String BPS_V1_BASE   = "/api/v1/batch-processing-statistics";
        public static final String BPS_V1_BY_ID  = BPS_V1_BASE + "/{id}";
        public static final String OPENAPI_DOCS  = "/v3/api-docs";
        public static final String SWAGGER_UI    = "/swagger-ui.html";
        public static final String SWAGGER_UI_ALL = "/swagger-ui/**";
        public static final String OPENAPI_ALL   = "/v3/api-docs/**";
    }

    // ====================================================================== //
    //  JWT claim names                                                       //
    // ====================================================================== //
    public static final class JwtClaims {
        private JwtClaims() {}
        public static final String ROLES  = "roles";
        public static final String AZP    = "azp";
        public static final String APP_ID = "appid";
        public static final String OID    = "oid";
        public static final String TID    = "tid";
    }
}
