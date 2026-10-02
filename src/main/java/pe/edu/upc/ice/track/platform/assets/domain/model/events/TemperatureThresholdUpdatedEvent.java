package pe.edu.upc.ice.track.platform.assets.domain.model.events;

import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Equipment;

import java.time.LocalDateTime;

/**
 * Domain event published when the acceptable temperature band of an {@link Equipment} changes.
 *
 * <p>This is the only signal other contexts receive when the threshold moves, which is what lets
 * Monitoring and Alerting pick the new band up without this context pushing it to them.</p>
 *
 * <p>Registered by {@link Equipment#changeThreshold} only after the new band has been validated,
 * so a rejected threshold - {@code minCelsius >= maxCelsius} - never produces this event.</p>
 *
 * @param equipmentId the unit whose threshold changed
 * @param siteId      the site the unit is installed at
 * @param minCelsius   the new lowest acceptable temperature, in Celsius
 * @param maxCelsius   the new highest acceptable temperature, in Celsius
 * @param changedAt    when the change was applied
 */
public record TemperatureThresholdUpdatedEvent(
    Long equipmentId,
    Long siteId,
    Double minCelsius,
    Double maxCelsius,
    LocalDateTime changedAt) {

  /**
   * Extracts the event fields from an {@link Equipment} whose threshold has just been replaced.
   *
   * @param equipment the unit, after its threshold was changed
   * @return the populated event
   */
  public static TemperatureThresholdUpdatedEvent from(Equipment equipment) {
    return new TemperatureThresholdUpdatedEvent(
        equipment.getEquipmentId(),
        equipment.getSiteId(),
        equipment.getTemperatureThreshold().minCelsius(),
        equipment.getTemperatureThreshold().maxCelsius(),
        LocalDateTime.now());
  }
}