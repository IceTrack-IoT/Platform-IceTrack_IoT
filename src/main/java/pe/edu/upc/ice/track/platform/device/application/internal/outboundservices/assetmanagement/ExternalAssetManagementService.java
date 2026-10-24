package pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.assetmanagement;

/**
 * Outbound port through which Device Management checks an equipment before pairing a board.
 *
 * <p>The implementation will be the only class in this context allowed to know about Assets
 * Management; command services depend on this small contract instead of on an asset aggregate or
 * repository.</p>
 */
public interface ExternalAssetManagementService {
  boolean existsEquipmentForOwner(Long equipmentId, Long ownerId);
}
