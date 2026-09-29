package pe.edu.upc.ice.track.platform.monitoring.application.commandservices;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.SensorReading;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.RecordReadingBatchCommand;

import java.util.Optional;

/** Application service handling write operations on {@link SensorReading}. */
public interface SensorReadingCommandService {

  /**
   * Records an incoming reading batch, de-duplicating by {@code readingUid}, and triggers
   * threshold evaluation against the applicable {@code AlertPolicy}.
   *
   * @return the persisted reading, or empty when it was a duplicate delivery
   */
  Optional<SensorReading> handle(RecordReadingBatchCommand command);
}
