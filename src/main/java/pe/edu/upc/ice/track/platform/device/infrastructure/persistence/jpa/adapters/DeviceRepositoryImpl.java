package pe.edu.upc.ice.track.platform.device.infrastructure.persistence.jpa.adapters;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.device.domain.model.aggregates.Device;
import pe.edu.upc.ice.track.platform.device.domain.repositories.DeviceRepository;
import pe.edu.upc.ice.track.platform.device.infrastructure.persistence.jpa.assemblers.DevicePersistenceAssembler;
import pe.edu.upc.ice.track.platform.device.infrastructure.persistence.jpa.repositories.DevicePersistenceRepository;

import java.util.List;
import java.util.Optional;

/** Adapter that implements the Device repository port through Spring Data JPA. */
@Repository
public class DeviceRepositoryImpl implements DeviceRepository {

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
  public List<Device> findByEquipmentId(Long equipmentId) {
    if (equipmentId == null) {
      return List.of();
    }
    return devicePersistenceRepository.findByPairedEquipmentIdOrderByIdAsc(equipmentId).stream()
        .map(DevicePersistenceAssembler::toDomainFromPersistence)
        .toList();
  }

  @Override
  public boolean existsByDeviceUid(String deviceUid) {
    return deviceUid != null && !deviceUid.isBlank()
        && devicePersistenceRepository.existsByDeviceUid(deviceUid.trim());
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
