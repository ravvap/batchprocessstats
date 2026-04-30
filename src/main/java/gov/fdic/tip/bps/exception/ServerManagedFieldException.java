package gov.fdic.tip.bps.exception;

import java.util.List;

/**
 * Thrown when a POST or PUT request body contains non-null server-managed fields
 * (id, createdBy, createdDateTime, updatedBy, updatedDateTime).
 *
 * Results in HTTP 400 problem+json identifying each offending field (BPS-006, BPS-007).
 */
public class ServerManagedFieldException extends RuntimeException {

    private final List<String> offendingFields;

    public ServerManagedFieldException(List<String> offendingFields) {
        super("Request body must not contain server-managed fields: " + offendingFields);
        this.offendingFields = offendingFields;
    }

    public List<String> getOffendingFields() {
        return offendingFields;
    }
}
