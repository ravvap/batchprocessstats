package gov.fdic.tip.bps.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing a batch source system.
 * Maps to the batch_source_system table.
 *
 * Seed data: SIMS, RRPS, CBIS.
 */
@Entity
@Table(name = "batch_source_system")
@Getter
@Setter
@NoArgsConstructor
public class BatchSourceSystem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "source_name", length = 100, nullable = false)
    private String sourceName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "channel_type", length = 100)
    private String channelType;

    @Column(name = "status", length = 100)
    private String status;
}
