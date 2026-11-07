package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.RecordReadingBatchCommand;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.RecordReadingBatchResource;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.AuthenticatedDevice;

/**
 * Maps a {@link RecordReadingBatchResource} into a {@link RecordReadingBatchCommand}.
 *
 * <p>The equipment and device identifiers are taken from the authenticated device, never from the
 * payload, whose optional copies are only cross-checked by the controller.</p>
 */
public final class RecordReadingBatchCommandFromResourceAssembler {

  private RecordReadingBatchCommandFromResourceAssembler() {
  }

  public static RecordReadingBatchCommand toCommandFromResource(
      RecordReadingBatchResource resource, AuthenticatedDevice device) {
    return new RecordReadingBatchCommand(
        resource.readingUid(), device.equipmentId(), device.deviceId(),
        resource.minTemperature(), resource.maxTemperature(), resource.avgTemperature(),
        resource.rawMinTemperature(), resource.rawMaxTemperature(), resource.rawAvgTemperature(),
        resource.humidity(), resource.sampleCount(),
        resource.lightLevelMax(), resource.doorOpenSeconds(), resource.presenceDetected(),
        resource.recordedAt(), resource.appliedConfigVersion());
  }
}
