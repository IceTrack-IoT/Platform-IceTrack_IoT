package pe.edu.upc.ice.track.platform.device.domain.model.valueobjects;

import pe.edu.upc.ice.track.platform.device.domain.model.aggregates.Device;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * The connectivity of a board, evaluated at one server instant.
 *
 * <p>Silence is measured from {@code last_seen_at}, the server arrival time of the board's latest
 * batch, never from a device clock. The window is the offline window of the alert policy that applies
 * to the unit the board is linked to, so a board and its unit are judged by the same rule.</p>
 *
 * @param state        the evaluated state; required
 * @param silentAfter  the offline window of the unit's policy; {@code null} when the board is unpaired
 * @param silentSince  when the board turned silent ({@code last_seen_at + silentAfter}); {@code null}
 *                     unless the state is {@link DeviceConnectivityState#SILENT}
 * @param evaluatedAt  the server instant of the evaluation; required
 */
public record DeviceConnectivity(
    DeviceConnectivityState state,
    Duration silentAfter,
    Instant silentSince,
    Instant evaluatedAt) {

  /**
   * Validates that only a silent board carries the instant it turned silent.
   */
  public DeviceConnectivity {
    Objects.requireNonNull(state, "state must not be null");
    Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
    if (state != DeviceConnectivityState.SILENT) {
      silentSince = null;
    }
  }

  /**
   * Evaluates the connectivity of a board.
   *
   * @param device      the board; required
   * @param silentAfter the offline window of the policy of the unit the board is linked to; ignored for
   *                    an unpaired board
   * @param now         the server instant of the evaluation; required
   * @return the evaluated connectivity
   */
  public static DeviceConnectivity evaluate(Device device, Duration silentAfter, Instant now) {
    Objects.requireNonNull(device, "device must not be null");
    Objects.requireNonNull(now, "now must not be null");
    return switch (device.getStatus()) {
      case UNPAIRED -> new DeviceConnectivity(DeviceConnectivityState.UNPAIRED, null, null, now);
      case REVOKED -> new DeviceConnectivity(DeviceConnectivityState.KEY_REVOKED, silentAfter, null, now);
      case PAIRED -> evaluatePaired(device.getLastSeenAt(), silentAfter, now);
    };
  }

  private static DeviceConnectivity evaluatePaired(Instant lastSeenAt, Duration silentAfter, Instant now) {
    if (lastSeenAt == null) {
      return new DeviceConnectivity(DeviceConnectivityState.NEVER_REPORTED, silentAfter, null, now);
    }
    if (silentAfter == null) {
      return new DeviceConnectivity(DeviceConnectivityState.REPORTING, null, null, now);
    }
    var silentSince = lastSeenAt.plus(silentAfter);
    return silentSince.isBefore(now)
        ? new DeviceConnectivity(DeviceConnectivityState.SILENT, silentAfter, silentSince, now)
        : new DeviceConnectivity(DeviceConnectivityState.REPORTING, silentAfter, null, now);
  }
}
