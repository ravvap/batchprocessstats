package gov.fdic.tip.bps.repository;

import gov.fdic.tip.bps.entity.BatchJobHistory;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Criteria API specifications for batch_job_history dynamic filtering (BPS-002, BPS-004).
 *
 * Each filter is only added to the predicate list when the parameter is non-null,
 * completely avoiding the PostgreSQL "could not determine data type of parameter $N"
 * error that occurs with JPQL IS NULL checks on untyped null bindings.
 */
public final class BatchJobHistorySpecification {

    private BatchJobHistorySpecification() {}

    /**
     * Builds a compound AND specification from all provided (non-null) filters.
     *
     * @param sourceSystemId  exact match on source_system_id FK
     * @param jobStatus       exact match on job_status
     * @param jobType         case-insensitive LIKE contains on job_type
     * @param from            inclusive lower bound on start_time
     * @param to              inclusive upper bound on start_time
     */
    public static Specification<BatchJobHistory> withFilters(
            Long    sourceSystemId,
            String  jobStatus,
            String  jobType,
            Instant from,
            Instant to) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (sourceSystemId != null) {
                predicates.add(
                    cb.equal(root.get("sourceSystem").get("id"), sourceSystemId));
            }

            if (jobStatus != null && !jobStatus.isBlank()) {
                predicates.add(
                    cb.equal(root.get("jobStatus"), jobStatus));
            }

            if (jobType != null && !jobType.isBlank()) {
                predicates.add(
                    cb.like(
                        cb.lower(root.get("jobType")),
                        "%" + jobType.toLowerCase() + "%"));
            }

            if (from != null) {
                predicates.add(
                    cb.greaterThanOrEqualTo(root.get("startTime"), from));
            }

            if (to != null) {
                predicates.add(
                    cb.lessThanOrEqualTo(root.get("startTime"), to));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
