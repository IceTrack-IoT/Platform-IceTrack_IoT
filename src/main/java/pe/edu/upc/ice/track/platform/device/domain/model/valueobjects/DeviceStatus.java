package pe.edu.upc.ice.track.platform.device.domain.model.valueobjects;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Lifecycle state of an IoT board managed by the platform. */
public enum DeviceStatus {
  /** Registered but not yet paired with a refrigeration unit. */
  PROVISIONED,
  /** Paired and allowed to authenticate and submit telemetry. */
  ACTIVE,
  /** Temporarily blocked by an operator. */
  SUSPENDED,
  /** Credential access has been revoked and cannot be restored. */
  REVOKED,
  /** Hardware was permanently retired from the fleet. */
  DECOMMISSIONED;

  private static final Map<DeviceStatus, Set<DeviceStatus>> ALLOWED_TRANSITIONS = matrix();

  private static Map<DeviceStatus, Set<DeviceStatus>> matrix() {
    var values = new EnumMap<DeviceStatus, Set<DeviceStatus>>(DeviceStatus.class);
    values.put(PROVISIONED, EnumSet.of(PROVISIONED, ACTIVE, REVOKED, DECOMMISSIONED));
    values.put(ACTIVE, EnumSet.of(ACTIVE, SUSPENDED, REVOKED, DECOMMISSIONED));
    values.put(SUSPENDED, EnumSet.of(SUSPENDED, ACTIVE, REVOKED, DECOMMISSIONED));
    values.put(REVOKED, EnumSet.of(REVOKED, DECOMMISSIONED));
    values.put(DECOMMISSIONED, EnumSet.of(DECOMMISSIONED));
    return Collections.unmodifiableMap(values);
  }

  public boolean canTransitionTo(DeviceStatus target) {
    return target != null && ALLOWED_TRANSITIONS.get(this).contains(target);
  }

  public Set<DeviceStatus> allowedTransitions() {
    return ALLOWED_TRANSITIONS.get(this);
  }
}
