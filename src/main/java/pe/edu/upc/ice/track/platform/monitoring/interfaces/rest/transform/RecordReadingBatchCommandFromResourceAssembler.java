package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.RecordReadingBatchCommand;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.RecordReadingBatchResource;

/** Maps a {@link RecordReadingBatchResource} into a {@link RecordReadingBatchCommand}. */
public final class RecordReadingBatchCommandFromResourceAssembler {

  private RecordReadingBatchCommandFromResourceAssembler() {
  }

  public static RecordReadingBatchCommand toCommandFromResource(RecordReadingBatchResource resource) {
    return new RecordReadingBatchCommand(
        resource.readingUid(), resource.equipmentId(), resource.deviceId(),
        resource.minTemperature(), resource.maxTemperature(), resource.avgTemperature(),
        resource.humidity(), resource.sampleCount(), resource.recordedAt());
  }
}
