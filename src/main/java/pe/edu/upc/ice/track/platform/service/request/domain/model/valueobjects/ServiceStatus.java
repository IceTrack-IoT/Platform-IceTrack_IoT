package pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Lifecycle status of a service request.
 *
 * <p>The transition matrix lives here, next to the states, for the same reason {@code assets}
 * keeps the equipment matrix in {@code StatusEquipment}: it is a rule about what the states mean
 * rather than about one operation. Every business method of {@code ServiceRequest} narrows the
 * source states it accepts and then asks {@link #canTransitionTo} before recording the move, so an
 * operation added later cannot open a path this matrix forbids.</p>
 *
 * <p>Matrix, where a mark means the transition is allowed:</p>
 * <pre>
 *                -&gt;  PENDING  ACCEPTED  IN_PROGRESS  COMPLETED  CANCELED
 *   PENDING            yes       yes         -           -         yes
 *   ACCEPTED           yes        -         yes          -         yes
 *   IN_PROGRESS         -         -          -          yes         -
 *   COMPLETED           -         -          -           -          -
 *   CANCELED            -         -          -           -          -
 * </pre>
 *
 * <p>{@code PENDING -> PENDING} is an assignment or a rejection: either way the request is still
 * waiting for a technician to commit to it. {@code ACCEPTED -> PENDING} is the owner reassigning the
 * request to another technician. {@code COMPLETED} and {@code CANCELED} are terminal.</p>
 */
public enum ServiceStatus {

  /** Waiting for a technician to be assigned, or for the assigned one to accept. */
  PENDING,

  /** Accepted by the assigned technician, not started yet. */
  ACCEPTED,

  /** The assigned technician is working on the unit. */
  IN_PROGRESS,

  /** The intervention was registered; terminal. */
  COMPLETED,

  /** Canceled by the owner before the work started; terminal. */
  CANCELED;

  private static final Map<ServiceStatus, Set<ServiceStatus>> ALLOWED_TRANSITIONS = buildTransitionMatrix();

  private static Map<ServiceStatus, Set<ServiceStatus>> buildTransitionMatrix() {
    var matrix = new EnumMap<ServiceStatus, Set<ServiceStatus>>(ServiceStatus.class);
    matrix.put(PENDING, Collections.unmodifiableSet(EnumSet.of(PENDING, ACCEPTED, CANCELED)));
    matrix.put(ACCEPTED, Collections.unmodifiableSet(EnumSet.of(PENDING, IN_PROGRESS, CANCELED)));
    matrix.put(IN_PROGRESS, Collections.unmodifiableSet(EnumSet.of(COMPLETED)));
    matrix.put(COMPLETED, Collections.unmodifiableSet(EnumSet.noneOf(ServiceStatus.class)));
    matrix.put(CANCELED, Collections.unmodifiableSet(EnumSet.noneOf(ServiceStatus.class)));
    return Collections.unmodifiableMap(matrix);
  }

  /**
   * Tells whether this status may be replaced by the given one.
   *
   * @param target the status to move to; {@code null} is never allowed
   * @return {@code true} when the transition is part of the matrix
   */
  public boolean canTransitionTo(ServiceStatus target) {
    return target != null && ALLOWED_TRANSITIONS.get(this).contains(target);
  }

  /**
   * Lists the statuses reachable from this one.
   *
   * @return an unmodifiable set of the permitted target statuses, empty for a terminal status
   */
  public Set<ServiceStatus> allowedTransitions() {
    return ALLOWED_TRANSITIONS.get(this);
  }

  /**
   * Tells whether no transition leaves this status.
   *
   * @return {@code true} for {@link #COMPLETED} and {@link #CANCELED}
   */
  public boolean isTerminal() {
    return allowedTransitions().isEmpty();
  }

  /**
   * Tells whether a technician assigned in this status is still engaged with the request.
   *
   * @return {@code true} for {@link #PENDING}, {@link #ACCEPTED} and {@link #IN_PROGRESS}
   */
  public boolean isActive() {
    return this == PENDING || this == ACCEPTED || this == IN_PROGRESS;
  }
}
