package pe.edu.upc.ice.track.platform.assets.interfaces.events;

import java.time.Instant;

/**
 * Integration event published after the connectivity of a unit changed: a batch arrived after a
 * silence, offline detection judged it silent, it was deactivated or reactivated, or a maintenance
 * window opened or closed on it.
 *
 * <p>The recipient is resolved by this context from the unit's site, so a consumer never decides who
 * the change concerns.</p>
 *
 * @param equipmentId      the unit
 * @param ownerProfileId   the owner of the unit
 * @param previousState    {@code REPORTING}, {@code SILENT}, {@code NEVER_REPORTED} or
 *                         {@code NOT_MONITORED}, before the change
 * @param state            the same values, after the change
 * @param lastReceivedAt   server arrival time of the unit's latest batch; may be {@code null}
 * @param underMaintenance whether a maintenance window covers the unit after the change
 * @param changedAt        when the change happened
 */
public record EquipmentConnectivityChangedIntegrationEvent(
    Long equipmentId,
    Long ownerProfileId,
    String previousState,
    String state,
    Instant lastReceivedAt,
    boolean underMaintenance,
    Instant changedAt) {
}
