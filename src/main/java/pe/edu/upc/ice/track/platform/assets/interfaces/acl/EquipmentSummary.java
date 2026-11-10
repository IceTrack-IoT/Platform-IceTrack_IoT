package pe.edu.upc.ice.track.platform.assets.interfaces.acl;

import java.time.Instant;

/**
 * Published view of a refrigeration unit, returned by {@link AssetContextFacade}.
 *
 * <p>Part of the published language of the {@code assets} context: plain values only, so no
 * {@code Equipment} or {@code Site} aggregate or value object leaves the context. The status travels
 * as the name of its constant.</p>
 *
 * @param equipmentId   identifier of the unit
 * @param name          name assigned to the unit
 * @param status        lifecycle status of the unit, by its constant name
 * @param siteId        identifier of the site the unit is installed at
 * @param siteName      name of that site at the time of the read; {@code null} when the site no
 *                      longer exists
 * @param lastReadingAt when the unit's latest reading batch was taken; {@code null} when it has not
 *                      reported yet
 */
public record EquipmentSummary(
    Long equipmentId,
    String name,
    String status,
    Long siteId,
    String siteName,
    Instant lastReadingAt) {
}
