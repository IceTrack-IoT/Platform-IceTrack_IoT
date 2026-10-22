package pe.edu.upc.ice.track.platform.assets.interfaces.acl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Equipment;
import pe.edu.upc.ice.track.platform.assets.domain.repositories.EquipmentRepository;
import pe.edu.upc.ice.track.platform.assets.domain.repositories.SiteRepository;

import java.util.Optional;

/**
 * ACL facade that exposes Assets Management capabilities to other bounded contexts.
 *
 * <p>This class <em>is</em> the published contract of the Anti-Corruption Layer of the
 * {@code assets} context, and it is upstream: nothing here calls another context, so Monitoring
 * and Alerting or Device Management can ask about equipment without this context needing them in
 * return. Every parameter and return type is a primitive, a boxed primitive or a
 * {@link String} - no {@code Site} or
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
 * unit whatever its owner or already holds the owner identifier to check against.</p>
 */
@Service
@Slf4j
public class AssetContextFacade {

  private final EquipmentRepository equipmentRepository;
  private final SiteRepository siteRepository;

  /**
   * Constructor
   *
   * @param equipmentRepository The {@link EquipmentRepository} instance
   * @param siteRepository      The {@link SiteRepository} instance, used to resolve the owner of a
   *                            unit's site
   */
  public AssetContextFacade(EquipmentRepository equipmentRepository, SiteRepository siteRepository) {
    this.equipmentRepository = equipmentRepository;
    this.siteRepository = siteRepository;
  }

  /**
   * Returns the highest temperature a unit may reach, in Celsius.
   *
   * <p><strong>Signature note.</strong> The specification for this method asks for a single
   * {@code Optional<Double>} even though a threshold has two bounds. The signature is kept
   * verbatim and resolves to the <em>maximum</em> bound, because a single scalar is what an alarm
   * rule needs to raise a "too warm" alert, and because changing it would silently change the
   * meaning for every existing caller.</p>
   *
   * @param equipmentId identifier of the equipment
   * @return the maximum acceptable temperature in Celsius, or empty when the unit does not exist
   */
  @Transactional(readOnly = true)
  public Optional<Double> getEquipmentThreshold(Long equipmentId) {
    return findAnyEquipment(equipmentId).map(equipment -> equipment.getTemperatureThreshold().maxCelsius());
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
}
