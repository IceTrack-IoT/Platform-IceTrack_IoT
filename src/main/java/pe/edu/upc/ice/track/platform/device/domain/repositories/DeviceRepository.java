package pe.edu.upc.ice.track.platform.device.domain.repositories;

import pe.edu.upc.ice.track.platform.device.domain.model.aggregates.Device;
import pe.edu.upc.ice.track.platform.device.domain.model.valueobjects.DevicePage;
import pe.edu.upc.ice.track.platform.device.domain.model.valueobjects.DeviceStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Persistence port for the Device Management aggregate. */
public interface DeviceRepository {
  Optional<Device> findById(Long deviceId);

  Optional<Device> findByDeviceUid(String deviceUid);

  /** Finds the board holding the given hex SHA-256 API key digest, through a unique index. */
  Optional<Device> findByApiKeyDigest(String apiKeyDigest);

  List<Device> findByEquipmentId(Long equipmentId);

  /**
   * Finds every board linked to one of the given units, ordered by board uid.
   *
   * <p>A board has no owner of its own: it belongs to whoever owns the unit it is linked to, and
   * units live in another context. The caller resolves the units of the owner first and passes
   * their identifiers here, so this is the scope of the listing and the filters can only narrow
   * it.</p>
   *
   * @param equipmentIds identifiers of the units that scope the listing; empty matches nothing
   * @param statuses     restrict to these statuses; {@code null} or empty does not restrict
   * @param search       case-insensitive text matched against the board uid or model, may be
   *                     {@code null}
   * @return the matching boards
   */
  List<Device> findAllLinkedToEquipment(Collection<Long> equipmentIds, Set<DeviceStatus> statuses, String search);

  /**
   * Finds one page of the boards linked to one of the given units, ordered by board uid, together
   * with the number of boards matching across every page.
   *
   * @param equipmentIds identifiers of the units that scope the listing; empty matches nothing
   * @param statuses     restrict to these statuses; {@code null} or empty does not restrict
   * @param search       case-insensitive text matched against the board uid or model, may be
   *                     {@code null}
   * @param page         zero-based page index; not negative
   * @param size         maximum number of boards the page may hold; strictly positive
   * @return the boards of that page and the total count
   */
  DevicePage findPageLinkedToEquipment(
      Collection<Long> equipmentIds, Set<DeviceStatus> statuses, String search, int page, int size);

  /**
   * Counts the boards in a status that are linked to one of the given units.
   *
   * @param equipmentIds identifiers of the units; empty counts nothing
   * @param status       the status to count; required
   * @return the number of matching boards
   */
  long countLinkedToEquipmentByStatus(Collection<Long> equipmentIds, DeviceStatus status);

  /** Tells whether any board, paired or revoked, is currently linked to the equipment. */
  boolean existsLinkedToEquipment(Long equipmentId);

  Device save(Device device);
}
