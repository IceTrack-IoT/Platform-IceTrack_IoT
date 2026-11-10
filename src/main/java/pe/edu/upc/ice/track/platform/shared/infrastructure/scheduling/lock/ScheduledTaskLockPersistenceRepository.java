package pe.edu.upc.ice.track.platform.shared.infrastructure.scheduling.lock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Spring Data repository of the scheduled task leases. Taking an expired lease is a Criteria API
 * conditional update built by {@link ScheduledTaskLockPersistenceSpecifications}.
 */
public interface ScheduledTaskLockPersistenceRepository
    extends JpaRepository<ScheduledTaskLockPersistenceEntity, String>,
    JpaSpecificationExecutor<ScheduledTaskLockPersistenceEntity> {
}
