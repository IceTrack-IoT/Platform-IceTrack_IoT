package pe.edu.upc.ice.track.platform.monitoring.application.commandservices;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.DetectOfflineEquipmentCommand;

/**
 * Application service detecting units that stopped reporting.
 *
 * <p>A unit is offline once the platform received no reading from it for the offline window of its
 * alert policy: one synchronization window of 60 seconds times {@code missed_sync_windows_for_offline}.
 * The unit is then marked offline in Assets Management and a {@code DEVICE_OFFLINE} alert is raised,
 * which Notifications turns into a notice for the owner. The first reading the unit delivers again
 * resolves the alert and brings the unit back online; that recovery is part of reading ingestion.</p>
 *
 * <p>Detection is time-driven: nothing in a request reveals that a unit went silent. This service only
 * implements the check; whatever runs it periodically decides its cadence.</p>
 */
public interface EquipmentConnectivityCommandService {

  /**
   * Checks every connected unit for silence at the command's instant.
   *
   * <p>For each unit silent for at least its offline window: a unit covered by a maintenance window is
   * left alone, because maintenance suppresses offline alerts; otherwise the unit is marked offline,
   * and an {@code OPEN}, {@code WARNING} alert is raised unless the unit already has an active
   * {@code DEVICE_OFFLINE} alert. Each unit is handled in its own transaction, so one failure does not
   * stop the others.</p>
   *
   * @param command the {@link DetectOfflineEquipmentCommand} Command
   * @return how many {@code DEVICE_OFFLINE} alerts were raised
   */
  int handle(DetectOfflineEquipmentCommand command);
}
