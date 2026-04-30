package gov.fdic.tip.bps.repository;

import gov.fdic.tip.bps.entity.BatchJobHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repository for batch_job_history.
 *
 * Extends JpaSpecificationExecutor so that dynamic nullable filters are
 * handled via the Criteria API — avoiding the PostgreSQL "could not determine
 * data type of parameter $N" error that occurs when JPQL binds null parameters
 * without explicit type information.
 */
@Repository
public interface BatchJobHistoryRepository
        extends JpaRepository<BatchJobHistory, Long>,
                JpaSpecificationExecutor<BatchJobHistory> {
}
