package gov.fdic.tip.bps.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Entity representing the history of a batch job execution.
 * Maps to the batch_job_history table.
 *
 * References batch_source_system via source_system_id FK.
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "job_id")
    private Long jobId;

    @Column(name = "job_type", length = 100, nullable = false)
    private String jobType;

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

    /**
     * FK to batch_source_system.id
     */
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
