package pe.edu.upc.ice.track.platform.notifications.application.internal.outboundservices.assetmanagement;

import java.util.Optional;

/**
 * Outbound port (Anti-Corruption Layer) through which the notifications application layer
 * resolves the recipient of an equipment-related notification.
 *
 * <p>The notifications application layer depends on this port and on nothing else: it never
 * sees an Asset Management aggregate, repository or command. The adapter implementing it, in
 * {@code notifications.infrastructure.assetmanagement.services}, is the only class in
 * {@code notifications} allowed to know that an {@code assetmanagement} context exists.</p>
 */
public interface ExternalAssetManagementServiceForNotifications {

  /**
   * @param equipmentId identifier of the equipment to resolve; required
   * @return the identifier of the equipment's owner, or {@code 0L} when the equipment does
   * not exist
   */
  Optional<Double> fetchEquipmentOwnerId(Long equipmentId);
}
