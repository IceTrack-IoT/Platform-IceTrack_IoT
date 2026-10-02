package pe.edu.upc.ice.track.platform.assets.interfaces.acl;

import java.util.Optional;

/**
 * ACL facade that exposes Assets Management capabilities to other bounded contexts.
 *
 * <p>This interface <em>is</em> the published contract of the Anti-Corruption Layer of the
 * {@code assets} context, and it is upstream: nothing here calls another context, so Monitoring
 * and Alerting or Device Management can ask about equipment without this context needing them in
 * return. Every parameter and return type is a primitive, a boxed primitive, a
 * {@link String} or the {@link EquipmentThresholdDto} record below - no {@code Site} or
 * {@code Equipment} aggregate, value object, command, repository or JPA entity appears in its
 * signature. Callers depend on this interface alone and must never reach into
 * {@code assets.domain.*}, {@code assets.application.*} or {@code assets.infrastructure.*}.</p>
 *
 * <p>Every method answers empty rather than throwing when the equipment does not exist, because
 * callers use these methods to decide whether to continue with their own work.</p>
 */
public interface AssetContextFacade {

  /**
   * Returns the highest temperature a unit may reach, in Celsius.
   *
   * <p><strong>Signature note.</strong> The specification for this method asks for a single
   * {@code Optional<Double>} even though a threshold has two bounds. The signature is kept
   * verbatim and resolves to the <em>maximum</em> bound, because a single scalar is what an alarm
   * rule needs to raise a "too warm" alert, and because changing it would silently change the
   * meaning for every existing caller. Callers that must evaluate a full band - which is what
   * Monitoring and Alerting actually does when it decides whether a reading is an excursion -
   * use {@link #getEquipmentThresholdRange(Long)} instead, and should prefer it.</p>
   *
   * @param equipmentId identifier of the equipment
   * @return the maximum acceptable temperature in Celsius, or empty when the unit does not exist
   */
  Optional<Double> getEquipmentThreshold(Long equipmentId);

  /**
   * Returns the full acceptable temperature band of a unit.
   *
   * <p>Both bounds, so a caller can decide whether a reading is inside the band rather than only
   * whether it is too warm.</p>
   *
   * @param equipmentId identifier of the equipment
   * @return the band, or empty when the unit does not exist
   */
  Optional<EquipmentThresholdDto> getEquipmentThresholdRange(Long equipmentId);

  /**
   * Tells whether a unit exists and belongs to a given owner.
   *
   * <p>Device Management calls this before pairing a device with a unit: pairing somebody else's
   * unit would hand that owner a device without consent.</p>
   *
   * @param equipmentId identifier of the equipment
   * @param ownerId     identifier of the owner expected to own it
   * @return {@code true} only when the unit exists and its site belongs to that owner
   */
  boolean existsEquipmentForOwner(Long equipmentId, Long ownerId);

  /**
   * Returns the site a unit is installed at.
   *
   * @param equipmentId identifier of the equipment
   * @return the site identifier, or empty when the unit does not exist
   */
  Optional<Long> getSiteIdByEquipment(Long equipmentId);

  /**
   * Returns the identifier of a unit, given the uid printed on the device.
   *
   * <p>Device Management calls this when a freshly provisioned board reports the uid it was
   * flashed with, which is the only thing it knows about the unit at that point.</p>
   *
   * @param uid the unit's own identifier, unique across the platform
   * @return the unit identifier, or empty when no unit carries that uid
   */
  Optional<Long> getEquipmentIdByUid(String uid);
}