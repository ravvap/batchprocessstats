package gov.fdic.tip.bps.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Allowed values for the batch_job_history.batch_type column.
 *
 * Stored values (API / DB): EMAIL, API, SFTP_PULL, BATCH
 * UI display labels:        Email, API, SFTP Pull, Batch
 *
 * BPS-001: The Type column displays user-facing labels mapped from stored values.
 * BPS-004: Filter accepts stored values (EMAIL, API, SFTP_PULL, BATCH).
 * BPS-006/007: Request body uses stored values; validated against this allow-list.
 */
public enum BatchType {

    EMAIL("Email"),
    API("API"),
    SFTP_PULL("SFTP Pull"),
    BATCH("Batch");

    private final String label;

    BatchType(String label) {
        this.label = label;
    }

    /** UI-facing label (e.g. "SFTP Pull" for SFTP_PULL). */
    public String getLabel() {
        return label;
    }

    /**
     * Serialised as the stored value name (EMAIL, API, SFTP_PULL, BATCH).
     * This is what is sent in API request/response bodies and stored in the DB.
     */
    @JsonValue
    public String getStoredValue() {
        return this.name();
    }

    /**
     * Deserialises from the stored value string (case-insensitive).
     * Rejects unknown values — Jackson will surface a 400 to the client.
     */
    @JsonCreator
    public static BatchType fromStoredValue(String value) {
        if (value == null) return null;
        for (BatchType t : values()) {
            if (t.name().equalsIgnoreCase(value.trim())) {
                return t;
            }
        }
        throw new IllegalArgumentException(
                "Invalid batch type: '" + value + "'. Allowed values: EMAIL, API, SFTP_PULL, BATCH");
    }
}
