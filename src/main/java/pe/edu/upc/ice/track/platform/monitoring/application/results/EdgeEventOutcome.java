package pe.edu.upc.ice.track.platform.monitoring.application.results;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.EdgeEvent;

/**
 * Result of recording one edge event: either it was new and processed, or an event with the same
 * {@code eventUid} had already been recorded.
 */
public sealed interface EdgeEventOutcome {

  /** The stored record of the event, whichever of the two cases occurred. */
  EdgeEvent event();

  /** The event was new: it was processed and recorded together with its alert change. */
  record Processed(EdgeEvent event) implements EdgeEventOutcome {
  }

  /** The event had already been recorded; nothing was processed again. */
  record Duplicate(EdgeEvent event) implements EdgeEventOutcome {
  }
}
