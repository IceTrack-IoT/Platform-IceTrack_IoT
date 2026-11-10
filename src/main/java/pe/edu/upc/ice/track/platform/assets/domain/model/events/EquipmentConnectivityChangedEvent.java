package pe.edu.upc.ice.track.platform.assets.domain.model.events;

import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.ConnectivityState;

import java.time.Instant;

/**
 * Domain event registered when the stored connectivity of a unit changes: its state moves (a batch
 * arrives after a silence, offline detection judges it silent, it is deactivated or reactivated), or
 * a maintenance window opens or closes on it.
 *
 * @param equipmentId      the unit
 * @param siteId           the site the unit is installed at, which resolves its owner
 * @param previousState    the state before the change
 * @param state            the state after the change
 * @param lastReceivedAt   server arrival time of the unit's latest batch; may be {@code null}
 * @param underMaintenance whether a maintenance window covers the unit after the change
 * @param changedAt        when the change happened
 */
public record EquipmentConnectivityChangedEvent(
    Long equipmentId,
    Long siteId,
    ConnectivityState previousState,
    ConnectivityState state,
    Instant lastReceivedAt,
    boolean underMaintenance,
    Instant changedAt) {
}
