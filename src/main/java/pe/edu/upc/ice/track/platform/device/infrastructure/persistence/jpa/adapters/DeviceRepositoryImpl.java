package pe.edu.upc.ice.track.platform.device.infrastructure.persistence.jpa.adapters;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.device.domain.model.aggregates.Device;
import pe.edu.upc.ice.track.platform.device.domain.model.valueobjects.DevicePage;
import pe.edu.upc.ice.track.platform.device.domain.model.valueobjects.DeviceStatus;
import pe.edu.upc.ice.track.platform.device.domain.repositories.DeviceRepository;
import pe.edu.upc.ice.track.platform.device.infrastructure.persistence.jpa.assemblers.DevicePersistenceAssembler;
import pe.edu.upc.ice.track.platform.device.infrastructure.persistence.jpa.repositories.DevicePersistenceRepository;
import pe.edu.upc.ice.track.platform.device.infrastructure.persistence.jpa.specifications.DevicePersistenceSpecifications;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Adapter that implements the Device repository port through Spring Data JPA. */
@Repository
public class DeviceRepositoryImpl implements DeviceRepository {

  /**
   * By board uid, then by identifier: the uid is unique, and the identifier keeps the order total
   * even if that ever stopped holding, since a listing paginated without a total order silently
   * drops and repeats rows from one page to the next.
   */
  private static final Sort INVENTORY_ORDER = Sort.by(Sort.Order.asc("deviceUid"), Sort.Order.asc("id"));

  private final DevicePersistenceRepository devicePersistenceRepository;
  private final ApplicationEventPublisher eventPublisher;

  public DeviceRepositoryImpl(
      DevicePersistenceRepository devicePersistenceRepository, ApplicationEventPublisher eventPublisher) {
    this.devicePersistenceRepository = devicePersistenceRepository;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public Optional<Device> findById(Long deviceId) {
    if (deviceId == null) {
      return Optional.empty();
    }
    return devicePersistenceRepository.findById(deviceId)
        .map(DevicePersistenceAssembler::toDomainFromPersistence);
  }

  @Override
  public Optional<Device> findByDeviceUid(String deviceUid) {
    if (deviceUid == null || deviceUid.isBlank()) {
      return Optional.empty();
    }
    return devicePersistenceRepository.findByDeviceUid(deviceUid.trim())
        .map(DevicePersistenceAssembler::toDomainFromPersistence);
  }

  @Override
  public Optional<Device> findByApiKeyDigest(String apiKeyDigest) {
    if (apiKeyDigest == null || apiKeyDigest.isBlank()) {
      return Optional.empty();
    }
    return devicePersistenceRepository.findByApiKeyDigest(apiKeyDigest)
        .map(DevicePersistenceAssembler::toDomainFromPersistence);
  }

  @Override
  public List<Device> findByEquipmentId(Long equipmentId) {
    if (equipmentId == null) {
      return List.of();
    }
    return devicePersistenceRepository.findByPairedEquipmentIdOrderByIdAsc(equipmentId).stream()
        .map(DevicePersistenceAssembler::toDomainFromPersistence)
        .toList();
  }

  @Override
  public List<Device> findAllLinkedToEquipment(
      Collection<Long> equipmentIds, Set<DeviceStatus> statuses, String search) {
    if (equipmentIds == null || equipmentIds.isEmpty()) {
      return List.of();
    }
    var specification = DevicePersistenceSpecifications.linkedToEquipment(equipmentIds, statuses, search);
    return devicePersistenceRepository.findAll(specification, INVENTORY_ORDER).stream()
        .map(DevicePersistenceAssembler::toDomainFromPersistence)
        .toList();
  }

  /**
   * {@inheritDoc}
   *
   * <p>The rows and the count come from the same specification, so the total can never disagree
   * with the filters the rows were selected by. A page whose first row lies beyond what a JDBC
   * offset can address is necessarily past the end, so it is answered empty, with the real total.</p>
   */
  @Override
  public DevicePage findPageLinkedToEquipment(
      Collection<Long> equipmentIds, Set<DeviceStatus> statuses, String search, int page, int size) {
    if (equipmentIds == null || equipmentIds.isEmpty()) {
      return DevicePage.empty(page, size);
    }
    var specification = DevicePersistenceSpecifications.linkedToEquipment(equipmentIds, statuses, search);
    if ((long) page * size > Integer.MAX_VALUE) {
      return new DevicePage(List.of(), page, size, devicePersistenceRepository.count(specification));
    }
    var slice = devicePersistenceRepository.findAll(specification, PageRequest.of(page, size, INVENTORY_ORDER));
    var content = slice.getContent().stream()
        .map(DevicePersistenceAssembler::toDomainFromPersistence)
        .toList();
    return new DevicePage(content, page, size, slice.getTotalElements());
  }

  @Override
  public long countLinkedToEquipmentByStatus(Collection<Long> equipmentIds, DeviceStatus status) {
    if (equipmentIds == null || equipmentIds.isEmpty() || status == null) {
      return 0L;
    }
    return devicePersistenceRepository.countByPairedEquipmentIdInAndStatus(equipmentIds, status);
  }

  @Override
  public boolean existsLinkedToEquipment(Long equipmentId) {
    return equipmentId != null && devicePersistenceRepository.existsByPairedEquipmentId(equipmentId);
  }

  @Override
  public Device save(Device device) {
    var isNew = device.getDeviceId() == null;
    var saved = devicePersistenceRepository.save(DevicePersistenceAssembler.toPersistenceFromDomain(device));
    var savedDevice = DevicePersistenceAssembler.toDomainFromPersistence(saved);
    publishPendingEvents(device);
    if (isNew) {
      savedDevice.onRegistered();
      publishPendingEvents(savedDevice);
    }
    return savedDevice;
  }

  private void publishPendingEvents(Device device) {
    device.domainEvents().forEach(eventPublisher::publishEvent);
    device.clearDomainEvents();
  }
}
