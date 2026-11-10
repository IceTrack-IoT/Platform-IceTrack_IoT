package pe.edu.upc.ice.track.platform.monitoring.application.results;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.SensorReading;

/**
 * Result of ingesting one reading batch: either it was new and recorded, or a batch with the same
 * {@code readingUid} had already been recorded. The REST layer answers 201 and 200 respectively.
 */
public sealed interface ReadingIngestionOutcome {

  /** The stored reading, whichever of the two cases occurred. */
  SensorReading reading();

  /** The batch was new: it was stored and its threshold evaluation ran. */
  record Recorded(SensorReading reading) implements ReadingIngestionOutcome {
  }

  /** The batch had already been recorded; nothing was stored and no alert was touched. */
  record Duplicate(SensorReading reading) implements ReadingIngestionOutcome {
  }
}
