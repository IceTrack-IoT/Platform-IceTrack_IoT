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
 * question an operator actually asks:</p>
 * <ul>
 *   <li>{@link #AVAILABLE} - catalogued and ready to be put into service, never connected yet;</li>
 *   <li>{@link #ON} - powered and running, whether or not it is currently reporting;</li>
 *   <li>{@link #OFF} - powered down on purpose, for cleaning, defrosting or a scheduled shutdown;</li>
 *   <li>{@link #OFFLINE} - supposed to be running but has stopped reporting, which is the state an
 *       operator has to react to.</li>
 * </ul>
 *
 * <p>The transition matrix is defined here, in the enum itself, because it is a rule about the
 * meaning of the states rather than about a particular aggregate: any part of the domain asking
 * "can this unit go from ON to OFFLINE?" gets the same answer. It is enforced by
 * {@link #canTransitionTo(StatusEquipment)} and surfaced by {@link Equipment#changeStatus}.</p>
 *
 * <p>Matrix, where a mark means the transition is allowed:</p>
 * <pre>
 *              -&gt;  ON    OFF    AVAILABLE    OFFLINE
 *   ON              yes   yes      -            yes
 *   OFF             yes   yes      yes          yes
 *   AVAILABLE       yes   yes      yes          -
 *   OFFLINE         yes   yes      yes          yes
 * </pre>
 *
 * <p>Two entries deserve a word. A unit may always be switched on or switched off: those are the
 * two direct operator actions. {@code AVAILABLE -&gt; OFFLINE} is the only forbidden move, because
 * a unit that was never put into service cannot lose a connection it never had - reporting it as
 * offline would raise a false alarm. A unit can only become {@code AVAILABLE} again from a state
 * it was actually running in, which is what makes {@code AVAILABLE} mean "ready, never in
 * service" rather than merely "stopped". Any transition to the state the unit already holds is
 * allowed, so re-sending the same status is idempotent instead of an error.</p>
 */
public enum StatusEquipment {

  /** Catalogued and ready to be put into service, never connected yet. */
  AVAILABLE,

  /** Powered and running, whether or not it is currently reporting. */
  ON,

  /** Powered down on purpose, for cleaning, defrosting or a scheduled shutdown. */
  OFF,

  /** Supposed to be running but has stopped reporting. */
  OFFLINE;

  private static final Map<StatusEquipment, Set<StatusEquipment>> ALLOWED_TRANSITIONS =
      buildTransitionMatrix();

  private static Map<StatusEquipment, Set<StatusEquipment>> buildTransitionMatrix() {
    var matrix = new EnumMap<StatusEquipment, Set<StatusEquipment>>(StatusEquipment.class);
    matrix.put(AVAILABLE, EnumSet.of(ON, OFF, AVAILABLE));
    matrix.put(ON, EnumSet.of(ON, OFF, OFFLINE));
    matrix.put(OFF, EnumSet.of(ON, OFF, AVAILABLE, OFFLINE));
    matrix.put(OFFLINE, EnumSet.of(ON, OFF, AVAILABLE, OFFLINE));
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
   * @return an unmodifiable set of all the statuses of this enumeration
   */
  public static Set<StatusEquipment> allStatuses() {
    return EnumSet.allOf(StatusEquipment.class);
  }
}