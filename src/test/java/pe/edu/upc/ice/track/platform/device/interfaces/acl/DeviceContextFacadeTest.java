package pe.edu.upc.ice.track.platform.device.interfaces.acl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.device.application.commandservices.DeviceCommandService;
import pe.edu.upc.ice.track.platform.device.domain.model.valueobjects.DeviceStatus;
import pe.edu.upc.ice.track.platform.device.domain.repositories.DeviceRepository;
import pe.edu.upc.ice.track.platform.device.test.fixtures.DeviceDomainFixtures;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.device.test.fixtures.DeviceDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class DeviceContextFacadeTest {

  @Mock
  private DeviceCommandService deviceCommandService;
  @Mock
  private DeviceRepository deviceRepository;

  @InjectMocks
  private DeviceContextFacade facade;

  @Test
  @DisplayName("recordActivity(Long, Instant) -> delegates to the device command service (AAA)")
  void recordActivityDelegates() {
    // Act
    facade.recordActivity(DEVICE_ID, LAST_SEEN_AT);

    // Assert
    verify(deviceCommandService, times(1)).recordActivity(DEVICE_ID, LAST_SEEN_AT);
  }


  @Test
  @DisplayName("countLinkedDevices(Collection) -> counts paired and revoked devices of the equipment (AAA)")
  void countLinkedDevicesCounts() {
    // Arrange
    var ids = Set.of(EQUIPMENT_ID, OTHER_EQUIPMENT_ID);
    doReturn(3L).when(deviceRepository).countLinkedToEquipmentByStatus(ids, DeviceStatus.PAIRED);
    doReturn(1L).when(deviceRepository).countLinkedToEquipmentByStatus(ids, DeviceStatus.REVOKED);

    // Act
    var counts = facade.countLinkedDevices(ids);

    // Assert
    assertThat(counts).isEqualTo(new LinkedDeviceCounts(3L, 1L));
  }

  @Test
  @DisplayName("countLinkedDevices(Collection) -> zero counts without reading for null ids (AAA)")
  void countLinkedDevicesNullIds() {
    // Act & Assert
    assertThat(facade.countLinkedDevices(null)).isEqualTo(new LinkedDeviceCounts(0L, 0L));
    verifyNoInteractions(deviceRepository);
  }

  @Test
  @DisplayName("countLinkedDevices(Collection) -> zero counts without reading for empty ids (AAA)")
  void countLinkedDevicesEmptyIds() {
    // Act & Assert
    assertThat(facade.countLinkedDevices(List.of())).isEqualTo(new LinkedDeviceCounts(0L, 0L));
    verifyNoInteractions(deviceRepository);
  }


  @Test
  @DisplayName("fetchLinkedDeviceId(Long) -> returns the id of the device linked to the equipment (AAA)")
  void fetchLinkedDeviceIdFound() {
    // Arrange
    when(deviceRepository.findByEquipmentId(EQUIPMENT_ID)).thenReturn(List.of(DeviceDomainFixtures.pairedDevice()));

    // Act & Assert
    assertThat(facade.fetchLinkedDeviceId(EQUIPMENT_ID)).isEqualTo(DEVICE_ID);
  }

  @Test
  @DisplayName("fetchLinkedDeviceId(Long) -> skips devices that are linked to nothing or unsaved (AAA)")
  void fetchLinkedDeviceIdSkipsUnlinkedAndUnsaved() {
    // Arrange
    var unlinked = DeviceDomainFixtures.unpairedDevice();
    var unsaved = DeviceDomainFixtures.deviceBuilder().id(null).paired().build();
    var linked = DeviceDomainFixtures.deviceBuilder().id(OTHER_DEVICE_ID).revoked().build();
    when(deviceRepository.findByEquipmentId(EQUIPMENT_ID)).thenReturn(List.of(unlinked, unsaved, linked));

    // Act & Assert
    assertThat(facade.fetchLinkedDeviceId(EQUIPMENT_ID)).isEqualTo(OTHER_DEVICE_ID);
  }

  @Test
  @DisplayName("fetchLinkedDeviceId(Long) -> 0 when the equipment has no linked device (AAA)")
  void fetchLinkedDeviceIdNone() {
    // Arrange
    when(deviceRepository.findByEquipmentId(EQUIPMENT_ID)).thenReturn(List.of());

    // Act & Assert
    assertThat(facade.fetchLinkedDeviceId(EQUIPMENT_ID)).isZero();
  }

  @Test
  @DisplayName("fetchLinkedDeviceId(Long) -> 0 without reading for a null equipment id (AAA)")
  void fetchLinkedDeviceIdNullEquipment() {
    // Act & Assert
    assertThat(facade.fetchLinkedDeviceId(null)).isZero();
    verifyNoInteractions(deviceRepository);
  }
}
