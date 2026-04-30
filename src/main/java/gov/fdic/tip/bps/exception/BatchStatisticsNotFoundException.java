package gov.fdic.tip.bps.exception;

import gov.fdic.tip.bps.config.ApplicationConstants.ErrorMessages;

/**
 * Thrown when a batch_job_history record is not found by id (BPS-005, BPS-007).
 * Results in HTTP 404 problem+json response.
 */
public class BatchStatisticsNotFoundException extends RuntimeException {

    private final Long id;

    public BatchStatisticsNotFoundException(Long id) {
        super(ErrorMessages.RECORD_NOT_FOUND + id);
        this.id = id;
    }

    public Long getId() { return id; }
}
