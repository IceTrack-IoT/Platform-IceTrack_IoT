package pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * StatusEquipment enumerates the operational state of a refrigeration unit.
 *
 * <p>The states are those the ubiquitous language distinguishes, and each one answers a different
 * question an operator or a field technician actually asks:</p>
 * <ul>
 *   <li>{@link #ACTIVE} - in service at its site, the state a unit is registered in;</li>
 *   <li>{@link #MAINTENANCE} - undergoing preventive maintenance, the visit the reminder interval
 *       schedules;</li>
 *   <li>{@link #REPAIR} - out of service because of a fault, waiting for or under corrective
 *       work;</li>
 *   <li>{@link #DEACTIVATED} - taken out of service on purpose, for a seasonal shutdown or because
 *       it is being retired.</li>
 * </ul>
 *
 * <p>Connectivity is not a status: whether a unit is currently reporting is the separate
 * {@code online} flag of the aggregate, written from telemetry, so a unit can be {@code ACTIVE}
 * and offline at the same time without either piece of information overwriting the other.</p>
 *
 * <p>The transition matrix is defined here, in the enum itself, because it is a rule about the
 * meaning of the states rather than about a particular aggregate. It is enforced by
 * {@link #canTransitionTo(StatusEquipment)} and surfaced by {@code Equipment#changeStatus}.</p>
 *
 * <p>Matrix, where a mark means the transition is allowed:</p>
 * <pre>
 *                -&gt;  ACTIVE   MAINTENANCE   REPAIR   DEACTIVATED
 *   ACTIVE            (same)       yes        yes        yes
 *   MAINTENANCE        yes       (same)       yes        yes
 *   REPAIR             yes         -        (same)       yes
 *   DEACTIVATED        yes         -          -        (same)
 * </pre>
 *
 * <ul>
 *   <li>Preventive maintenance is performed on a working unit, so {@code MAINTENANCE} is entered
 *       from {@code ACTIVE} only. A maintenance visit that uncovers a fault moves straight to
 *       {@code REPAIR}.</li>
 *   <li>A unit under repair either returns to service or is deactivated once the fault is
 *       settled.</li>
 *   <li>A deactivated unit is powered down: it must be reactivated before anything else happens to
 *       it, so {@code DEACTIVATED} only leads back to {@code ACTIVE}.</li>
 *   <li>Re-sending the status a unit already holds is accepted as an idempotent no-op instead of an
 *       error.</li>
 * </ul>
 */
public enum StatusEquipment {

  /** In service at its site, the state a unit is registered in. */
  ACTIVE,

  /** Undergoing preventive maintenance. */
  MAINTENANCE,

  /** Out of service because of a fault, waiting for or under corrective work. */
  REPAIR,

  /** Taken out of service on purpose, for a seasonal shutdown or because it is being retired. */
  DEACTIVATED;

  private static final Map<StatusEquipment, Set<StatusEquipment>> ALLOWED_TRANSITIONS =
      buildTransitionMatrix();

  private static Map<StatusEquipment, Set<StatusEquipment>> buildTransitionMatrix() {
    var matrix = new EnumMap<StatusEquipment, Set<StatusEquipment>>(StatusEquipment.class);
    matrix.put(ACTIVE, Collections.unmodifiableSet(EnumSet.of(ACTIVE, MAINTENANCE, REPAIR, DEACTIVATED)));
    matrix.put(MAINTENANCE, Collections.unmodifiableSet(EnumSet.of(MAINTENANCE, ACTIVE, REPAIR, DEACTIVATED)));
    matrix.put(REPAIR, Collections.unmodifiableSet(EnumSet.of(REPAIR, ACTIVE, DEACTIVATED)));
    matrix.put(DEACTIVATED, Collections.unmodifiableSet(EnumSet.of(DEACTIVATED, ACTIVE)));
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
