package gov.fdic.tip.bps.repository;

import gov.fdic.tip.bps.entity.BatchSourceSystem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for batch_source_system table.
 */
@Repository
public interface BatchSourceSystemRepository extends JpaRepository<BatchSourceSystem, Long> {

    Optional<BatchSourceSystem> findBySourceName(String sourceName);
}
