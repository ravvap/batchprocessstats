package gov.fdic.tip.bps.repository;

import gov.fdic.tip.bps.entity.BatchJobHistory;
import gov.fdic.tip.bps.entity.BatchSourceSystem;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Criteria API specifications for batch_job_history dynamic filtering (BPS-002, BPS-004).
 *
 * Null filters are simply not added to the predicate list, avoiding the
 * PostgreSQL "could not determine data type of parameter $N" error entirely.
 *
 * Filters supported:
 *   sourceName   — case-insensitive LIKE contains on batch_source_system.source_name (JOIN)
 *   jobStatus    — exact match on job_status
 *   jobType      — case-insensitive LIKE contains on job_type
 *   from / to    — inclusive bounds on start_time
 */
public final class BatchJobHistorySpecification {

    private BatchJobHistorySpecification() {}

    /**
     * Builds a compound AND specification from all provided (non-null) filters.
     *
     * @param sourceName  case-insensitive substring match on batch_source_system.source_name
     * @param jobStatus   exact match on job_status
     * @param jobType     case-insensitive substring match on job_type
     * @param from        inclusive lower bound on start_time (UTC)
     * @param to          inclusive upper bound on start_time (UTC)
     */
    public static Specification<BatchJobHistory> withFilters(
            String  sourceName,
            String  jobStatus,
            String  jobType,
            Instant from,
            Instant to) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // sourceName filter — join to batch_source_system
            if (sourceName != null && !sourceName.isBlank()) {
                Join<BatchJobHistory, BatchSourceSystem> sourceJoin =
                        root.join("sourceSystem", JoinType.INNER);
                predicates.add(
                    cb.like(
                        cb.lower(sourceJoin.get("sourceName")),
                        "%" + sourceName.trim().toLowerCase() + "%"));
            }

            // jobStatus — exact match
            if (jobStatus != null && !jobStatus.isBlank()) {
                predicates.add(cb.equal(root.get("jobStatus"), jobStatus));
            }

            // jobType — case-insensitive contains
            if (jobType != null && !jobType.isBlank()) {
                predicates.add(
                    cb.like(
                        cb.lower(root.get("jobType")),
                        "%" + jobType.trim().toLowerCase() + "%"));
            }

            // startTime range
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startTime"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startTime"), to));
            }

            // ManyToOne join to sourceSystem — no duplicate rows possible,
            // so distinct(true) is not needed and would conflict with ORDER BY
            // on the joined column in PostgreSQL.

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
