package pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * StatusEquipment enumerates the business lifecycle state of a refrigeration unit.
 *
 * <p>The states are those the ubiquitous language distinguishes, and each one answers a different
 * question an operator or a field technician actually asks:</p>
 * <ul>
 *   <li>{@link #ACTIVE} - in service at its site, the state a unit is registered in;</li>
 *   <li>{@link #DESACTIVATE} - taken out of service on purpose, for a seasonal shutdown or because
 *       it is being retired;</li>
 *   <li>{@link #MAINTENANCE} - undergoing preventive maintenance, the visit the reminder interval
 *       schedules;</li>
 *   <li>{@link #REPAIR} - out of service because of a fault, waiting for or under corrective
 *       work.</li>
 * </ul>
 *
 * <p>Connectivity is not a status: whether a unit is currently reporting is the separate
 * {@code online} flag of the aggregate, written from telemetry, so a unit can be {@code ACTIVE}
 * and offline at the same time without either piece of information overwriting the other.</p>
 *
 * <p>The transition matrix is defined here, in the enum itself, because it is a rule about the
 * meaning of the states rather than about a particular aggregate: any part of the domain asking
 * "can this unit go from REPAIR to MAINTENANCE?" gets the same answer. It is enforced by
 * {@link #canTransitionTo(StatusEquipment)} and surfaced by {@link Equipment#changeStatus}.</p>
 *
 * <p>Matrix, where a mark means the transition is allowed:</p>
 * <pre>
 *                -&gt;  ACTIVE   DESACTIVATE   MAINTENANCE   REPAIR
 *   ACTIVE             yes        yes           yes         yes
 *   DESACTIVATE        yes        yes            -          yes
 *   MAINTENANCE        yes        yes           yes         yes
 *   REPAIR             yes        yes            -          yes
 * </pre>
 *
 * <p>A unit may always be returned to service or taken out of it, and a fault may be found in any
 * state, so {@code ACTIVE}, {@code DESACTIVATE} and {@code REPAIR} are reachable from everywhere.
 * {@code MAINTENANCE} is the only restricted target: preventive maintenance is performed on a
 * working unit in service, so it is entered from {@code ACTIVE} only. A deactivated unit is
 * reactivated first, and a unit under repair either returns to service or is deactivated once the
 * fault is settled; a maintenance visit that uncovers a fault, on the other hand, moves straight to
 * {@code REPAIR}. Any transition to the state the unit already holds is allowed, so re-sending the
 * same status is idempotent instead of an error.</p>
 */
public enum StatusEquipment {

  /** In service at its site, the state a unit is registered in. */
  ACTIVE,

  /** Taken out of service on purpose, for a seasonal shutdown or because it is being retired. */
  DESACTIVATE,

  /** Undergoing preventive maintenance. */
  MAINTENANCE,

  /** Out of service because of a fault, waiting for or under corrective work. */
  REPAIR;

  private static final Map<StatusEquipment, Set<StatusEquipment>> ALLOWED_TRANSITIONS =
      buildTransitionMatrix();

  private static Map<StatusEquipment, Set<StatusEquipment>> buildTransitionMatrix() {
    var matrix = new EnumMap<StatusEquipment, Set<StatusEquipment>>(StatusEquipment.class);
    matrix.put(ACTIVE, Collections.unmodifiableSet(EnumSet.of(ACTIVE, DESACTIVATE, MAINTENANCE, REPAIR)));
    matrix.put(DESACTIVATE, Collections.unmodifiableSet(EnumSet.of(ACTIVE, DESACTIVATE, REPAIR)));
    matrix.put(MAINTENANCE, Collections.unmodifiableSet(EnumSet.of(ACTIVE, DESACTIVATE, MAINTENANCE, REPAIR)));
    matrix.put(REPAIR, Collections.unmodifiableSet(EnumSet.of(ACTIVE, DESACTIVATE, REPAIR)));
    return Collections.unmodifiableMap(matrix);
  }

  /**
   * Tells whether this status may be replaced by the given one.
   *
   * @param target the status to move to; {@code null} is never allowed
   * @return {@code true} when the transition is part of the matrix
   */
  public boolean canTransitionTo(StatusEquipment target) {
    return target != null && ALLOWED_TRANSITIONS.get(this).contains(target);
  }

  /**
   * Lists the statuses reachable from this one, itself included.
   *
   * @return an unmodifiable set of the permitted target statuses
   */
  public Set<StatusEquipment> allowedTransitions() {
    return ALLOWED_TRANSITIONS.get(this);
  }

  /**
   * Lists every status an equipment may transition to, used to render the matrix in the OpenAPI
   * documentation of the status endpoint.
   *
   * @return a set of all the statuses of this enumeration
   */
  public static Set<StatusEquipment> allStatuses() {
    return EnumSet.allOf(StatusEquipment.class);
  }
}
