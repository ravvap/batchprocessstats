package gov.fdic.tip.bps.repository;

import gov.fdic.tip.bps.entity.BatchJobHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

/**
 * Repository for batch_job_history — primary backing table for BPS-004 through BPS-007.
 */
@Repository
public interface BatchJobHistoryRepository extends JpaRepository<BatchJobHistory, Long> {

    /**
     * Filtered paginated query (BPS-004 / BPS-002).
     * All filter parameters are optional; combined with AND semantics.
     *
     * @param sourceSystemId  filter by source system FK (exact match)
     * @param jobStatus       filter by job_status (exact match)
     * @param jobType         filter by job_type (case-insensitive contains)
     * @param from            inclusive lower bound on start_time (UTC)
     * @param to              inclusive upper bound on start_time (UTC)
     * @param pageable        pagination + sort
     */
    @Query("""
            SELECT h FROM BatchJobHistory h
            WHERE (:sourceSystemId IS NULL OR h.sourceSystem.id = :sourceSystemId)
              AND (:jobStatus      IS NULL OR h.jobStatus = :jobStatus)
              AND (:jobType        IS NULL
                    OR LOWER(h.jobType) LIKE LOWER(CONCAT('%', :jobType, '%')))
              AND (:from           IS NULL OR h.startTime >= :from)
              AND (:to             IS NULL OR h.startTime <= :to)
            """)
    Page<BatchJobHistory> findAllFiltered(
            @Param("sourceSystemId") Long    sourceSystemId,
            @Param("jobStatus")      String  jobStatus,
            @Param("jobType")        String  jobType,
            @Param("from")           Instant from,
            @Param("to")             Instant to,
            Pageable pageable
    );
}
