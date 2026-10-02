package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.Alert;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.AlertResource;

/**
 * Maps an {@link Alert} into an {@link AlertResource}.
 *
 * <p>{@code Temperature} is unwrapped to a plain {@code Double} here, the same boundary
 * discipline as {@code SensorReadingResourceFromEntityAssembler}.</p>
 */
public final class AlertResourceFromEntityAssembler {

  private AlertResourceFromEntityAssembler() {
  }

  public static AlertResource toResourceFromEntity(Alert alert) {
    return new AlertResource(
        alert.getId(), alert.getEquipmentId(), alert.getType().name(),
        alert.getSeverity().name(), alert.getStatus().name(), alert.getPeakTemperature().celsius(),
        alert.getOpenedAt(), alert.getResolvedAt());
  }
}
