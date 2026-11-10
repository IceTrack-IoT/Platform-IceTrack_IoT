package pe.edu.upc.ice.track.platform.assets.interfaces.acl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.assets.application.commandservices.EquipmentCommandService;
import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Equipment;
import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Site;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.MarkEquipmentOfflineCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.RecordEquipmentReadingCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.StatusEquipment;
import pe.edu.upc.ice.track.platform.assets.domain.repositories.EquipmentRepository;
import pe.edu.upc.ice.track.platform.assets.domain.repositories.SiteRepository;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * ACL facade that exposes Assets Management capabilities to other bounded contexts.
 *
 * <p>This class <em>is</em> the published contract of the Anti-Corruption Layer of the
 * {@code assets} context, and it is upstream: nothing here calls another context, so Monitoring
 * and Alerting or Device Management can ask about equipment without this context needing them in
 * return. Every parameter and return type is a primitive, a boxed primitive, a primitive array, a
 * {@link String}, an {@link Instant}, a collection of those or a value record of this package such
 * as {@link EquipmentSummary} - no {@code Site} or
 * {@code Equipment} aggregate, value object, command, repository or JPA entity appears in its
 * signature. Callers depend on this class alone and must never reach into
 * {@code assets.domain.*}, {@code assets.application.*} or {@code assets.infrastructure.*}.</p>
 *
 * <p>Every method answers empty rather than throwing when the equipment does not exist, because
 * callers use these methods to decide whether to continue with their own work. A {@code null}
 * identifier is answered as "not found" instead of being passed down, so a caller that fails to
 * resolve an id cannot provoke a database round trip for nothing.</p>
 *
 * <p>Reads go straight to the repositories rather than through the query services: those resolve
 * an authenticated caller's username to an owner, while a cross-context caller either asks about a
 * unit whatever its owner or already holds the owner identifier to check against. Writes go
 * through {@link EquipmentCommandService}, so the aggregate enforces its own rules.</p>
 */
@Service
@Slf4j
public class AssetContextFacade {

  private final EquipmentRepository equipmentRepository;
  private final SiteRepository siteRepository;
  private final EquipmentCommandService equipmentCommandService;

  /**
   * Constructor
   *
   * @param equipmentRepository     The {@link EquipmentRepository} instance
   * @param siteRepository          The {@link SiteRepository} instance, used to resolve the owner of a
   *                                unit's site
   * @param equipmentCommandService The {@link EquipmentCommandService} instance, applying the
   *                                telemetry state Monitoring and Alerting reports
   */
  public AssetContextFacade(EquipmentRepository equipmentRepository, SiteRepository siteRepository,
                            EquipmentCommandService equipmentCommandService) {
    this.equipmentRepository = equipmentRepository;
    this.siteRepository = siteRepository;
    this.equipmentCommandService = equipmentCommandService;
  }

  /**
   * Returns the highest temperature a unit may reach, in Celsius.
   *
   * <p><strong>Signature note.</strong> The specification for this method asks for a single
   * {@code Optional<Double>} even though a threshold has two bounds. The signature is kept
   * verbatim and resolves to the <em>maximum</em> bound, because a single scalar is what an alarm
   * rule needs to raise a "too warm" alert, and because changing it would silently change the
   * meaning for every existing caller. Callers that need both bounds use
   * {@link #getEquipmentThresholdRange(Long)} instead.</p>
   *
   * @param equipmentId identifier of the equipment
   * @return the maximum acceptable temperature in Celsius, or empty when the unit does not exist
   */
  @Transactional(readOnly = true)
  public Optional<Double> getEquipmentThreshold(Long equipmentId) {
    return findAnyEquipment(equipmentId).map(equipment -> equipment.getTemperatureThreshold().maxCelsius());
  }

  /**
   * Returns the full temperature band a unit must stay within, in Celsius.
   *
   * <p>Monitoring and Alerting calls this to evaluate a reading against both bounds of the unit's
   * configured threshold. The band is returned as a primitive {@code double[]} so that the
   * {@code TemperatureThreshold} value object never leaves this context. Both bounds are
   * guaranteed non-null and ordered by that value object's invariant, so unboxing them here
   * cannot fail.</p>
   *
   * @param equipmentId identifier of the equipment
   * @return the threshold as {@code [minCelsius, maxCelsius]}, or empty when the identifier is
   *         {@code null} or the unit does not exist
   */
  @Transactional(readOnly = true)
  public Optional<double[]> getEquipmentThresholdRange(Long equipmentId) {
    return findAnyEquipment(equipmentId)
        .map(Equipment::getTemperatureThreshold)
        .map(threshold -> new double[] {threshold.minCelsius(), threshold.maxCelsius()});
  }

  /**
   * Returns the version of the configuration a unit's edge gateway must enforce.
   *
   * <p>Device Management uses it as the ETag of the gateway's configuration poll. It is read
   * straight from the table rather than from a cached aggregate, so it always reflects the latest
   * committed increment.</p>
   *
   * @param equipmentId identifier of the equipment
   * @return the configuration version, or empty when the identifier is {@code null} or the unit
   *         does not exist
   */
  @Transactional(readOnly = true)
  public Optional<Integer> getEquipmentConfigVersion(Long equipmentId) {
    return equipmentRepository.findConfigVersion(equipmentId);
  }

  /**
   * Returns the identifier of the owner a unit belongs to, resolved through the unit's site.
   *
   * <p>Notifications calls this to decide who must be told about an alert raised for a unit.</p>
   *
   * @param equipmentId identifier of the equipment
   * @return the owner identifier, or empty when the unit or its site does not exist
   */
  @Transactional(readOnly = true)
  public Optional<Long> getEquipmentOwnerId(Long equipmentId) {
    return findAnyEquipment(equipmentId)
        .flatMap(equipment -> siteRepository.findById(equipment.getSiteId()))
        .map(Site::getOwnerId);
  }

  /**
   * Tells whether a unit exists and belongs to a given owner.
   *
   * <p>Device Management calls this before pairing a device with a unit: pairing somebody else's
   * unit would hand that owner a device without consent.</p>
   *
   * @param equipmentId identifier of the equipment
   * @param ownerId identifier of the owner expected to own it
   * @return {@code true} only when the unit exists and its site belongs to that owner
   */
  @Transactional(readOnly = true)
  public boolean existsEquipmentForOwner(Long equipmentId, Long ownerId) {
    if (equipmentId == null || ownerId == null) {
      return false;
    }
    return findAnyEquipment(equipmentId)
        .flatMap(equipment -> siteRepository.findById(equipment.getSiteId()))
        .filter(site -> site.belongsTo(ownerId))
        .isPresent();
  }

  /**
   * Tells whether a site exists and belongs to a given owner.
   *
   * <p>Reporting calls this before computing a site-scoped report, so an owner can never report on
   * somebody else's site.</p>
   *
   * @param siteId  identifier of the site
   * @param ownerId identifier of the owner expected to own it
   * @return {@code true} only when the site exists and belongs to that owner
   */
  @Transactional(readOnly = true)
  public boolean existsSiteForOwner(Long siteId, Long ownerId) {
    if (siteId == null || ownerId == null) {
      return false;
    }
    return siteRepository.findById(siteId).filter(site -> site.belongsTo(ownerId)).isPresent();
  }

  /**
   * Returns the site a unit is installed at.
   *
   * @param equipmentId identifier of the equipment
   * @return the site identifier, or empty when the unit does not exist
   */
  @Transactional(readOnly = true)
  public Optional<Long> getSiteIdByEquipment(Long equipmentId) {
    return findAnyEquipment(equipmentId).map(Equipment::getSiteId);
  }

  /**
   * Returns the identifiers of every unit installed at a site, whatever its owner.
   *
   * <p>Reporting calls this to resolve the units a site-scoped report covers.</p>
   *
   * @param siteId identifier of the site
   * @return the unit identifiers, empty when the identifier is {@code null}, the site has no units
   *         or does not exist
   */
  @Transactional(readOnly = true)
  public List<Long> fetchEquipmentIdsBySite(Long siteId) {
    if (siteId == null) {
      return List.of();
    }
    return equipmentRepository.findBySiteId(siteId).stream()
        .map(Equipment::getEquipmentId)
        .toList();
  }

  /**
   * Returns every unit of an owner, optionally narrowed to one of its sites, ordered by name.
   *
   * <p>Device Management and Monitoring and Alerting call this to answer an owner-wide read in one
   * step: the identifiers scope their own query to the caller's units, and the names spare the client
   * a second read. A site that is not the owner's matches no unit, so it never widens the answer.</p>
   *
   * @param ownerId identifier of the owner
   * @param siteId  restrict to a single site, may be {@code null}
   * @return the units, empty when the owner is {@code null} or owns none there
   */
  @Transactional(readOnly = true)
  public List<EquipmentSummary> fetchEquipmentSummariesByOwner(Long ownerId, Long siteId) {
    if (ownerId == null) {
      return List.of();
    }
    return equipmentRepository.findAllByOwnerId(ownerId, siteId, Set.of()).stream()
        .map(AssetContextFacade::toSummary)
        .toList();
  }

  /**
   * Returns the published view of one unit, whatever its owner.
   *
   * <p>Device Management calls this to name the unit a device is paired with, after it checked the
   * device belongs to the caller.</p>
   *
   * @param equipmentId identifier of the equipment
   * @return the unit, or empty when the identifier is {@code null} or the unit does not exist
   */
  @Transactional(readOnly = true)
  public Optional<EquipmentSummary> fetchEquipmentSummary(Long equipmentId) {
    return findAnyEquipment(equipmentId).map(AssetContextFacade::toSummary);
  }

  /**
   * Counts the sites of an owner.
   *
   * @param ownerId identifier of the owner
   * @return the number of sites, {@code 0} when the owner is {@code null} or has none
   */
  @Transactional(readOnly = true)
  public int countSitesByOwner(Long ownerId) {
    return ownerId == null ? 0 : siteRepository.findByOwnerId(ownerId).size();
  }

  /**
   * Returns the name of every lifecycle status a unit can hold, in declaration order.
   *
   * <p>Lets a caller that totals units per status report the statuses no unit currently holds,
   * without learning the status type itself.</p>
   *
   * @return the status names
   */
  public List<String> fetchEquipmentStatusNames() {
    return Arrays.stream(StatusEquipment.values()).map(Enum::name).toList();
  }

  /**
   * Returns the identifier of a unit, given the uid printed on the device.
   *
   * <p>Device Management calls this when a freshly provisioned board reports the uid it was
   * flashed with, which is the only thing it knows about the unit at that point.</p>
   *
   * @param uid the unit's own identifier, unique across the platform
   * @return the unit identifier, or empty when no unit carries that uid
   */
  @Transactional(readOnly = true)
  public Optional<Long> getEquipmentIdByUid(String uid) {
    if (uid == null || uid.isBlank()) {
      return Optional.empty();
    }
    return equipmentRepository.findByUid(uid).map(Equipment::getEquipmentId);
  }

  /**
   * Records that a reading batch of a unit was stored.
   *
   * <p>Monitoring and Alerting calls this for every new batch, inside its ingestion transaction. The
   * unit goes online, and its last reading instant and temperature move to the batch's unless the
   * batch is older than the latest one recorded.</p>
   *
   * @param equipmentId        identifier of the equipment
   * @param temperatureCelsius the temperature the batch reports, may be {@code null}
   * @param recordedAt         when the batch was taken
   * @return {@code true} when the unit exists and was updated; {@code false} when an identifier or
   *         the instant is {@code null}, or the unit does not exist
   */
  public boolean recordEquipmentReading(Long equipmentId, Double temperatureCelsius, Instant recordedAt) {
    if (equipmentId == null || recordedAt == null) {
      return false;
    }
    return equipmentCommandService.handle(
        new RecordEquipmentReadingCommand(equipmentId, temperatureCelsius, recordedAt));
  }

  /**
   * Records that a unit stopped reporting.
   *
   * <p>Monitoring and Alerting calls this when it raises a {@code DEVICE_OFFLINE} alert for the unit.</p>
   *
   * @param equipmentId identifier of the equipment
   * @return {@code true} when the unit was online and is now offline
   */
  public boolean markEquipmentOffline(Long equipmentId) {
    if (equipmentId == null) {
      return false;
    }
    return equipmentCommandService.handle(new MarkEquipmentOfflineCommand(equipmentId));
  }

  /**
   * Returns the identifiers of every unit currently shown as connected, whatever its owner.
   *
   * <p>Monitoring and Alerting checks these units for silence: only a connected unit can stop
   * reporting.</p>
   *
   * @return the unit identifiers, by ascending identifier; empty when no unit is online
   */
  @Transactional(readOnly = true)
  public List<Long> fetchOnlineEquipmentIds() {
    return equipmentRepository.findOnline().stream()
        .map(Equipment::getEquipmentId)
        .toList();
  }

  /**
   * Loads a unit whatever its owner, which is what a cross-context caller needs: these callers are
   * asking about equipment they legitimately serve, not about one of their own.
   *
   * @param equipmentId identifier of the equipment
   * @return the unit, or empty when the identifier is absent or unknown
   */
  private Optional<Equipment> findAnyEquipment(Long equipmentId) {
    if (equipmentId == null) {
      return Optional.empty();
    }
    return equipmentRepository.findById(equipmentId);
  }

  /**
   * Translates a unit into its published view.
   *
   * @param equipment the unit
   * @return the view, made of plain values only
   */
  private static EquipmentSummary toSummary(Equipment equipment) {
    return new EquipmentSummary(
        equipment.getEquipmentId(),
        equipment.getName(),
        equipment.getStatus().name(),
        equipment.getSiteId(),
        equipment.getSiteName(),
        equipment.getLastReadingAt());
  }
}
