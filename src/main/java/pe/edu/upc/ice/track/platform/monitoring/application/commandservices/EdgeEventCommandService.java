package pe.edu.upc.ice.track.platform.monitoring.application.commandservices;

import pe.edu.upc.ice.track.platform.monitoring.application.results.EdgeEventOutcome;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.RecordEdgeEventCommand;

/**
 * Application service processing the events edge gateways report.
 */
public interface EdgeEventCommandService {

  /**
   * Records an edge event and applies it to the unit's thermal excursion alert.
   *
   * <ul>
   *   <li>{@code THERMAL_EXCURSION_DETECTED} opens a CRITICAL alert, unless one is already OPEN or
   *       ACKNOWLEDGED for the unit.</li>
   *   <li>{@code THERMAL_EXCURSION_NORMALIZED} resolves the active alert, if any.</li>
   * </ul>
   *
   * @param command the event
   * @return {@code Processed} for a new event, {@code Duplicate} for an event already recorded
   * @throws IllegalArgumentException when a reported temperature is outside the plausible range
   */
  EdgeEventOutcome handle(RecordEdgeEventCommand command);
}
