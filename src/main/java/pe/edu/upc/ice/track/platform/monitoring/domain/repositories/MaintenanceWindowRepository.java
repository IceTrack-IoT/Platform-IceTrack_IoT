package pe.edu.upc.ice.track.platform.monitoring.domain.repositories;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.MaintenanceWindow;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain repository port for {@link MaintenanceWindow}.
 */
public interface MaintenanceWindowRepository {

  /**
   * Persists a window and publishes the domain events it registered. The unique index on
   * {@code start_event_uid} rejects a second window for the same start event.
   *
   * @param window the window to persist
   * @return the persisted window, carrying its identity
   */
  MaintenanceWindow save(MaintenanceWindow window);

  /**
   * @param startEventUid identifier of the gateway event that started the window
   * @return the window, or empty when no window was started by that event
   */
  Optional<MaintenanceWindow> findByStartEventUid(UUID startEventUid);

  /**
   * @param equipmentId the unit
   * @return the unit's active window, or empty when none is active
   */
  Optional<MaintenanceWindow> findActiveByEquipmentId(Long equipmentId);

  /**
   * Finds the window of a unit that covers an instant: started at or before it and either still
   * active or ended after it. Used to judge readings and gateway events by when they happened, even
   * when they are delivered late.
   *
   * @param equipmentId the unit
   * @param timestamp   the instant to test
   * @return the covering window, the latest started one if several qualify, or empty
   */
  Optional<MaintenanceWindow> findCovering(Long equipmentId, Instant timestamp);

  /**
   * Lists the windows of a unit started within a period, newest first.
   *
   * @param equipmentId the unit
   * @param from        inclusive lower bound of the start, or {@code null} for no lower bound
   * @param to          inclusive upper bound of the start, or {@code null} for no upper bound
   * @return the matching windows, newest first
   */
  List<MaintenanceWindow> findByEquipmentIdAndStartedAtBetween(Long equipmentId, Instant from, Instant to);

  /**
   * Lists the windows still active although they expired before a cutoff.
   *
   * @param cutoff the instant their expiration must precede
   * @return the overdue windows, oldest expiration first
   */
  List<MaintenanceWindow> findActiveExpiredBefore(Instant cutoff);
}
