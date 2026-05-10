package gov.fdic.tip.bps.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Entity representing the history of a batch job execution.
 * Maps to the batch_job_history table.
 *
 * Four audit columns (server-managed — never accepted in POST/PUT bodies,
 * always returned in responses per BPS-010 requirement):
 *   created_by          — set from caller identity on POST
 *   created_date_time   — set by DB/Hibernate on INSERT
 *   updated_by          — set from caller identity on POST and PUT
 *   updated_date_time   — set by DB/Hibernate on INSERT and UPDATE
 */
@Entity
@Table(name = "batch_job_history",
       indexes = {
           @Index(name = "idx_batch_job_history_source_system_start_time",
                  columnList = "source_system_id, start_time DESC")
       })
@Getter
@Setter
@NoArgsConstructor
public class BatchJobHistory {

    // ------------------------------------------------------------------ //
    //  Server-managed fields                                              //
    // ------------------------------------------------------------------ //

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "created_by", length = 100, nullable = false, updatable = false)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_date_time", nullable = false, updatable = false)
    private Instant createdDateTime;

    @Column(name = "updated_by", length = 100, nullable = false)
    private String updatedBy;

    @UpdateTimestamp
    @Column(name = "updated_date_time", nullable = false)
    private Instant updatedDateTime;

    // ------------------------------------------------------------------ //
    //  Editable fields                                                    //
    // ------------------------------------------------------------------ //

    @Column(name = "job_id")
    private Long jobId;

    @Column(name = "job_type", length = 100, nullable = false)
    private String jobType;

    /**
     * Batch processing type — validated against the EMAIL/API/SFTP_PULL/BATCH allow-list.
     * Stored as VARCHAR using the enum name (e.g. "SFTP_PULL").
     * UI displays the user-facing label from BatchType.getLabel().
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "batch_type", length = 20)
    private BatchType batchType;

    @Column(name = "retry_count")
    private Integer retryCount;

    @Column(name = "job_status", length = 50, nullable = false)
    private String jobStatus;

    @Column(name = "start_time")
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "status", length = 100, nullable = false)
    private String status;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    /** FK to batch_source_system.id */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_system_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_batch_job_history_source_system"))
    private BatchSourceSystem sourceSystem;

    @Column(name = "records_changed")
    private Integer recordsChanged;

    @Column(name = "records_gathered")
    private Integer recordsGathered;

    @Column(name = "records_processed_current_period")
    private Integer recordsProcessedCurrentPeriod;

    @Column(name = "records_processed_prior_period")
    private Integer recordsProcessedPriorPeriod;

    @Column(name = "records_unpostable")
    private Integer recordsUnpostable;
}
