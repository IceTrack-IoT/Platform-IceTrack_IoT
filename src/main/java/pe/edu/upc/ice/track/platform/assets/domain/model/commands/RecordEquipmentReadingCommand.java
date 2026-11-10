package pe.edu.upc.ice.track.platform.assets.domain.model.commands;

import java.time.Instant;

/**
 * Command recording that a reading batch of a unit was stored by Monitoring and Alerting.
 *
 * <p>Issued through the published facade of this context, never by a REST caller, so it carries no
 * username and is not owner-scoped.</p>
 *
 * @param equipmentId        the unit; required
 * @param temperatureCelsius the temperature the batch reports, or {@code null} when unknown
 * @param recordedAt         when the batch was taken; required
 * @param receivedAt         when the platform received the batch, by the server clock; {@code null}
 *                           is read as now
 */
public record RecordEquipmentReadingCommand(
    Long equipmentId, Double temperatureCelsius, Instant recordedAt, Instant receivedAt) {

  /**
   * Validates the command.
   *
   * @throws IllegalArgumentException when the unit or the reading instant is missing
   */
  public RecordEquipmentReadingCommand {
    if (equipmentId == null || recordedAt == null) {
      throw new IllegalArgumentException("equipmentId and recordedAt must not be null");
    }
  }
}
