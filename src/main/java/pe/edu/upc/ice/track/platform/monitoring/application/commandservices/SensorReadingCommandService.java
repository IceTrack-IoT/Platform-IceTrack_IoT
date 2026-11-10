package pe.edu.upc.ice.track.platform.monitoring.application.commandservices;

import pe.edu.upc.ice.track.platform.monitoring.application.results.ReadingIngestionOutcome;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.SensorReading;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.RecordReadingBatchCommand;

/** Application service handling write operations on {@link SensorReading}. */
public interface SensorReadingCommandService {

  /**
   * Records an incoming reading batch, de-duplicating by {@code readingUid}, and evaluates it
   * against the unit's temperature threshold, read from Assets Management, using the detection
   * settings of the applicable {@code AlertPolicy}.
   *
   * @return {@link ReadingIngestionOutcome.Recorded} for a new batch, or
   *         {@link ReadingIngestionOutcome.Duplicate} with the stored reading for a resubmission
   * @throws IllegalArgumentException when the batch breaks a physical bound of the sensor
   */
  ReadingIngestionOutcome handle(RecordReadingBatchCommand command);
}
