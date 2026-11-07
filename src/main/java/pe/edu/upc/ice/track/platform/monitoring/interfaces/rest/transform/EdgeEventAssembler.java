package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.EdgeEvent;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.RecordEdgeEventCommand;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.EdgeEventResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.RecordEdgeEventResource;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.AuthenticatedDevice;

/**
 * Assembler between the edge event REST resources and the domain.
 */
public final class EdgeEventAssembler {

  private EdgeEventAssembler() {
  }

  /**
   * Builds the command from the payload, taking the device and the unit from the API key.
   *
   * @param resource the payload
   * @param device   the authenticated device
   * @return the command
   */
  public static RecordEdgeEventCommand toCommandFromResource(RecordEdgeEventResource resource, AuthenticatedDevice device) {
    return new RecordEdgeEventCommand(
        resource.eventUid(),
        resource.type().toDomain(),
        device.deviceId(),
        device.equipmentId(),
        resource.occurredAt(),
        resource.temperatureCelsius(),
        resource.peakTemperatureCelsius(),
        resource.readingUid());
  }

  /**
   * @param event the processed event
   * @return its REST representation
   */
  public static EdgeEventResource toResourceFromEntity(EdgeEvent event) {
    return new EdgeEventResource(
        event.getEventUid(), event.getType().name(), event.getOccurredAt(), event.getResult().name(), event.getAlertId());
  }
}
