package pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.assets.interfaces.acl.AssetContextFacade;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Outbound service (the outbound half of the Anti-Corruption Layer between {@code monitoring} and
 * {@code assets}) through which the monitoring context retrieves the temperature threshold of an
 * equipment before evaluating an incoming reading, checks who owns a unit or a site, lists the units
 * of an owner, and resolves the owner a telemetry stream event must be delivered to.
 *
 * <p>This is the only class in {@code monitoring} that knows an {@code assets} context exists. It
 * talks to it exclusively through {@link AssetContextFacade}, whose whole signature is
 * primitives: no assets aggregate, repository or command is reachable from here.</p>
 *
 * <p>The threshold returned is the one actually configured on the unit. There is deliberately no
 * fallback band: when the unit is unknown this service answers empty, and
 * {@code SensorReadingCommandServiceImpl} logs a warning and skips the evaluation rather than
 * judging the reading against a band that does not belong to it.</p>
 */
@Service
public class ExternalAssetManagementService {

  private final AssetContextFacade assetContextFacade;

  public ExternalAssetManagementService(AssetContextFacade assetContextFacade) {
    this.assetContextFacade = assetContextFacade;
  }

  /**
   * @param equipmentId identifier of the equipment to evaluate; required
   * @return the threshold as {@code [minCelsius, maxCelsius]}, or empty when the equipment
   *         does not exist
   */
  public Optional<double[]> fetchTemperatureThreshold(Long equipmentId) {
    if (equipmentId == null) {
      return Optional.empty();
    }
    return assetContextFacade.getEquipmentThresholdRange(equipmentId);
  }

  /**
   * @param equipmentId identifier of the equipment
   * @param ownerId     identifier of the owner profile expected to own it
   * @return {@code true} only when the unit exists and its site belongs to that owner
   */
  public boolean isEquipmentOwnedBy(Long equipmentId, Long ownerId) {
    return assetContextFacade.existsEquipmentForOwner(equipmentId, ownerId);
  }

  /**
   * Resolves the owner of a unit, through the unit's site. The telemetry stream uses it to deliver an
   * event to the owner of the unit it concerns and to nobody else.
   *
   * @param equipmentId identifier of the equipment
   * @return the owner profile identifier, or empty when the identifier is {@code null} or the unit or
   *         its site does not exist
   */
  public Optional<Long> fetchOwnerId(Long equipmentId) {
    if (equipmentId == null) {
      return Optional.empty();
    }
    return assetContextFacade.getEquipmentOwnerId(equipmentId);
  }

  /**
   * Reports a newly stored reading batch to the unit, which goes online and moves its last reading
   * forward.
   *
   * @param equipmentId        identifier of the equipment
   * @param temperatureCelsius the temperature the batch reports
   * @param recordedAt         when the batch was taken
   * @return {@code true} when the unit exists and was updated
   */
  public boolean recordEquipmentReading(Long equipmentId, Double temperatureCelsius, Instant recordedAt) {
    return assetContextFacade.recordEquipmentReading(equipmentId, temperatureCelsius, recordedAt);
  }

  /**
   * Reports that a unit stopped reporting.
   *
   * @param equipmentId identifier of the equipment
   * @return {@code true} when the unit was online and is now offline
   */
  public boolean markEquipmentOffline(Long equipmentId) {
    return assetContextFacade.markEquipmentOffline(equipmentId);
  }

  /**
   * @return the identifiers of the units currently shown as connected
   */
  public List<Long> fetchOnlineEquipmentIds() {
    return assetContextFacade.fetchOnlineEquipmentIds();
  }

  /**
   * @param siteId  identifier of the site
   * @param ownerId identifier of the owner profile expected to own it
   * @return {@code true} only when the site exists and belongs to that owner
   */
  public boolean isSiteOwnedBy(Long siteId, Long ownerId) {
    return assetContextFacade.existsSiteForOwner(siteId, ownerId);
  }

  /**
   * Lists the units of an owner. Monitoring keys its data by unit alone, so these units are what
   * scopes an owner-wide read of alerts, policies or dashboard figures to the caller.
   *
   * @param ownerId identifier of the owner profile
   * @param siteId  restrict to the units of a single site, may be {@code null}
   * @return the units, empty when the owner is {@code null} or owns none there
   */
  public List<MonitoredEquipment> fetchEquipmentOwnedBy(Long ownerId, Long siteId) {
    if (ownerId == null) {
      return List.of();
    }
    return assetContextFacade.fetchEquipmentSummariesByOwner(ownerId, siteId).stream()
        .map(unit -> new MonitoredEquipment(unit.equipmentId(), unit.status(), unit.lastReadingAt()))
        .toList();
  }

  /**
   * @param ownerId identifier of the owner profile
   * @return the number of sites of that owner
   */
  public int countSitesOwnedBy(Long ownerId) {
    return assetContextFacade.countSitesByOwner(ownerId);
  }

  /**
   * @return the name of every lifecycle status a unit can hold, in declaration order
   */
  public List<String> fetchEquipmentStatusNames() {
    return assetContextFacade.fetchEquipmentStatusNames();
  }
}
