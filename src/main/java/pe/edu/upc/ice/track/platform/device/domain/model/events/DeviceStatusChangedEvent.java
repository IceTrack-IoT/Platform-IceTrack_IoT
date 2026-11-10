package pe.edu.upc.ice.track.platform.device.domain.model.events;

import pe.edu.upc.ice.track.platform.device.domain.model.valueobjects.DeviceStatus;

import java.time.Instant;

/**
 * Published after a board's lifecycle moved: it was paired, its credential was rotated or revoked, or
 * it was unpaired. A rotation keeps the board {@code PAIRED}, so its previous and new status may be
 * the same.
 *
 * @param deviceId          the board
 * @param equipmentId       the unit the board is, or was until this change, linked to; the unit's
 *                          owner is who the change concerns
 * @param pairedEquipmentId the unit the board is linked to after the change, or {@code null} once
 *                          unpaired
 * @param previousStatus    the status before the change
 * @param status            the status after the change
 * @param changedAt         when the change happened
 */
public record DeviceStatusChangedEvent(
    Long deviceId,
    Long equipmentId,
    Long pairedEquipmentId,
    DeviceStatus previousStatus,
    DeviceStatus status,
    Instant changedAt) {
}
