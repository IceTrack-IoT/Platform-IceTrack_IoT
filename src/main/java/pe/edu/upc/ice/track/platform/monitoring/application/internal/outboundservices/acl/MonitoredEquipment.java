package pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl;

import java.time.Instant;

/**
 * A unit of an owner, in the monitoring context's own terms: what an owner-wide read needs to scope
 * itself to the caller's units and to judge whether each of them is reporting.
 *
 * @param equipmentId   identifier of the unit
 * @param status        lifecycle status of the unit, by the name Assets Management gives it
 * @param lastReadingAt when the unit's latest reading batch was taken, or {@code null} when it has
 *                      not reported yet
 */
public record MonitoredEquipment(Long equipmentId, String status, Instant lastReadingAt) {
}
