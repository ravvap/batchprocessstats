package gov.fdic.tip.bps.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a batch processing statistics record.
 * Maps to the batch_processing_statistics table in PostgreSQL.
 *
 * BPS-001, BPS-004, BPS-005, BPS-006, BPS-007
 */
@Entity
@Table(name = "batch_processing_statistics")
@Getter
@Setter
@NoArgsConstructor
public class BatchProcessingStatistics {

    /**
     * Allowed values for the type field (BPS-004, BPS-006).
     */
    public enum BatchType {
        EMAIL, API, SFTP_PULL, BATCH
    }

    // ------------------------------------------------------------------ //
    //  Server-managed fields (never accepted in POST/PUT request bodies)  //
    // ------------------------------------------------------------------ //

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "created_by", length = 100, updatable = false, nullable = false)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_date_time", updatable = false, nullable = false)
    private Instant createdDateTime;

    @Column(name = "updated_by", length = 100, nullable = false)
    private String updatedBy;

    @UpdateTimestamp
    @Column(name = "updated_date_time", nullable = false)
    private Instant updatedDateTime;

    // ------------------------------------------------------------------ //
    //  Editable fields (accepted in POST and PUT bodies)                  //
    // ------------------------------------------------------------------ //

    @Column(name = "process_name", length = 100, nullable = false)
    private String processName;

    @Column(name = "start_date_time", nullable = false)
    private Instant startDateTime;

    /**
     * Null while the job is still in progress. When set, must be >= startDateTime.
     */
    @Column(name = "end_date_time")
    private Instant endDateTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false)
    private BatchType type;

    @Column(name = "records_gathered", nullable = false)
    private Integer recordsGathered;

    @Column(name = "records_changed", nullable = false)
    private Integer recordsChanged;

    @Column(name = "error_records", nullable = false)
    private Integer errorRecords;

    @Column(name = "processed_records", nullable = false)
    private Integer processedRecords;
}
