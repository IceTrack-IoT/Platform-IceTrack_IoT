package pe.edu.upc.ice.track.platform.device.interfaces.events;

import java.time.Instant;

/**
 * Integration event published after a board was paired, had its credential rotated or revoked, or
 * was unpaired, from whatever session.
 *
 * <p>The recipient is resolved by this context from the unit the board is, or was, linked to, so a
 * consumer never decides who the change concerns.</p>
 *
 * @param deviceId          the board
 * @param ownerProfileId    the owner of the unit the board is, or was until this change, linked to
 * @param pairedEquipmentId the unit the board is linked to after the change; {@code null} once unpaired
 * @param previousStatus    {@code UNPAIRED}, {@code PAIRED} or {@code REVOKED}, before the change
 * @param status            the same values, after the change
 * @param changedAt         when the change happened
 */
public record DeviceStatusChangedIntegrationEvent(
    Long deviceId,
    Long ownerProfileId,
    Long pairedEquipmentId,
    String previousStatus,
    String status,
    Instant changedAt) {
}
