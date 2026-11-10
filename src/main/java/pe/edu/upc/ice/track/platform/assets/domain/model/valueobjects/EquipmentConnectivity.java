package pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects;

import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Equipment;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Connectivity of a unit, evaluated by the server clock at one instant.
 *
 * <p>Silence is measured from the server arrival time of the unit's latest batch, never from the
 * time the device says it took it, so a gateway whose clock drifts cannot flip the state. The rule
 * is the one the scheduled offline detection applies: reaching the offline window exactly already
 * counts as silent.</p>
 *
 * @param state              the evaluated state
 * @param lastReceivedAt     server arrival time of the latest batch; {@code null} when the unit never
 *                           reported
 * @param silentAfter        the offline window of the unit's alert policy
 * @param silentSince        {@code lastReceivedAt + silentAfter}; set only when {@link ConnectivityState#SILENT}
 * @param underMaintenance   whether a maintenance window covers the evaluation instant
 * @param evaluatedAt        the server instant of the evaluation
 */
public record EquipmentConnectivity(
    ConnectivityState state,
    Instant lastReceivedAt,
    Duration silentAfter,
    Instant silentSince,
    boolean underMaintenance,
    Instant evaluatedAt) {

  public EquipmentConnectivity {
    Objects.requireNonNull(state, "state must not be null");
    Objects.requireNonNull(silentAfter, "silentAfter must not be null");
    Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
  }

  /**
   * Evaluates the connectivity of a unit.
   *
   * <p>A {@code DEACTIVATED} unit is {@link ConnectivityState#NOT_MONITORED}. Otherwise a unit with no
   * arrival time is {@link ConnectivityState#NEVER_REPORTED}, one whose latest batch arrived at least
   * {@code silentAfter} ago is {@link ConnectivityState#SILENT}, and any other is
   * {@link ConnectivityState#REPORTING}.</p>
   *
   * @param equipment   the unit; required
   * @param silentAfter the offline window of the unit's alert policy; required and positive
   * @param now         the server instant to judge at; required
   * @return the connectivity
   */
  public static EquipmentConnectivity evaluate(Equipment equipment, Duration silentAfter, Instant now) {
    Objects.requireNonNull(equipment, "equipment must not be null");
    Objects.requireNonNull(silentAfter, "silentAfter must not be null");
    Objects.requireNonNull(now, "now must not be null");
    var lastReceivedAt = equipment.lastArrivalAt();
    var underMaintenance = equipment.isCoveredByMaintenanceWindow();
    if (equipment.getStatus() == StatusEquipment.DEACTIVATED) {
      return new EquipmentConnectivity(
          ConnectivityState.NOT_MONITORED, lastReceivedAt, silentAfter, null, underMaintenance, now);
    }
    if (lastReceivedAt == null) {
      return new EquipmentConnectivity(
          ConnectivityState.NEVER_REPORTED, null, silentAfter, null, underMaintenance, now);
    }
    var silentSince = lastReceivedAt.plus(silentAfter);
    if (!now.isBefore(silentSince)) {
      return new EquipmentConnectivity(
          ConnectivityState.SILENT, lastReceivedAt, silentAfter, silentSince, underMaintenance, now);
    }
    return new EquipmentConnectivity(
        ConnectivityState.REPORTING, lastReceivedAt, silentAfter, null, underMaintenance, now);
  }
}
