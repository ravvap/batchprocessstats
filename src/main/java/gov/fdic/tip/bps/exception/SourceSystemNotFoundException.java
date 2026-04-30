package gov.fdic.tip.bps.exception;

import gov.fdic.tip.bps.config.ApplicationConstants.ErrorMessages;

/**
 * Thrown when a batch_source_system record is not found by id.
 * Results in HTTP 404 problem+json response.
 */
public class SourceSystemNotFoundException extends RuntimeException {

    private final Long id;

    public SourceSystemNotFoundException(Long id) {
        super(ErrorMessages.SOURCE_SYSTEM_NOT_FOUND + id);
        this.id = id;
    }

    public Long getId() { return id; }
}
