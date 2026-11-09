package pe.edu.upc.ice.track.platform.monitoring.domain.model.commands;

import java.time.Instant;
import java.util.Objects;

/**
 * Command checking every connected unit for silence at a given instant, raising a
 * {@code DEVICE_OFFLINE} alert for each one that stopped reporting.
 *
 * @param detectedAt the instant to judge silence at, by the server clock; required
 */
public record DetectOfflineEquipmentCommand(Instant detectedAt) {

  /**
   * Validates the command.
   *
   * @throws NullPointerException when the instant is missing
   */
  public DetectOfflineEquipmentCommand {
    Objects.requireNonNull(detectedAt, "detectedAt must not be null");
  }
}
