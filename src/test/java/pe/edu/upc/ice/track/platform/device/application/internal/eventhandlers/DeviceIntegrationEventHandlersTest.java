package pe.edu.upc.ice.track.platform.device.application.internal.eventhandlers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.acl.ExternalAssetManagementServiceForDevice;
import pe.edu.upc.ice.track.platform.device.domain.model.events.DevicePairedEvent;
import pe.edu.upc.ice.track.platform.device.domain.model.events.DeviceRegisteredEvent;
import pe.edu.upc.ice.track.platform.device.domain.model.events.DeviceStatusChangedEvent;
import pe.edu.upc.ice.track.platform.device.domain.model.events.DeviceUnpairedEvent;
import pe.edu.upc.ice.track.platform.device.domain.model.valueobjects.DeviceStatus;
import pe.edu.upc.ice.track.platform.device.interfaces.events.DevicePairedIntegrationEvent;
import pe.edu.upc.ice.track.platform.device.interfaces.events.DeviceRegisteredIntegrationEvent;
import pe.edu.upc.ice.track.platform.device.interfaces.events.DeviceStatusChangedIntegrationEvent;
import pe.edu.upc.ice.track.platform.device.interfaces.events.DeviceUnpairedIntegrationEvent;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.device.test.fixtures.DeviceDomainFixtures.*;


@ExtendWith(MockitoExtension.class)
class DeviceIntegrationEventHandlersTest {

  private static final Instant AT = Instant.parse("2026-01-01T10:00:00Z");

  @Mock
  private ApplicationEventPublisher eventPublisher;
  @Mock
  private ExternalAssetManagementServiceForDevice externalAssetManagementService;

  @Test
  @DisplayName("DevicePairedEventHandler.on(DevicePairedEvent) -> publishes the matching integration event (AAA)")
  void pairedPublishesIntegrationEvent() {
    // Arrange
    var handler = new DevicePairedEventHandler(eventPublisher);
    var captor = ArgumentCaptor.forClass(Object.class);

    // Act
    handler.on(new DevicePairedEvent(DEVICE_ID, EQUIPMENT_ID, AT));

    // Assert
    verify(eventPublisher, times(1)).publishEvent(captor.capture());
    assertThat(captor.getValue()).isEqualTo(new DevicePairedIntegrationEvent(DEVICE_ID, EQUIPMENT_ID, AT));
  }


  @Test
  @DisplayName("DeviceRegisteredEventHandler.on(DeviceRegisteredEvent) -> publishes the matching integration event (AAA)")
  void registeredPublishesIntegrationEvent() {
    // Arrange
    var handler = new DeviceRegisteredEventHandler(eventPublisher);
    var captor = ArgumentCaptor.forClass(Object.class);

    // Act
    handler.on(new DeviceRegisteredEvent(DEVICE_ID, VALID_DEVICE_UID, AT));

    // Assert
    verify(eventPublisher, times(1)).publishEvent(captor.capture());
    assertThat(captor.getValue()).isEqualTo(new DeviceRegisteredIntegrationEvent(DEVICE_ID, VALID_DEVICE_UID, AT));
  }


  @Test
  @DisplayName("DeviceUnpairedEventHandler.on(DeviceUnpairedEvent) -> publishes the matching integration event (AAA)")
  void unpairedPublishesIntegrationEvent() {
    // Arrange
    var handler = new DeviceUnpairedEventHandler(eventPublisher);
    var captor = ArgumentCaptor.forClass(Object.class);

    // Act
    handler.on(new DeviceUnpairedEvent(DEVICE_ID, EQUIPMENT_ID, AT));

    // Assert
    verify(eventPublisher, times(1)).publishEvent(captor.capture());
    assertThat(captor.getValue()).isEqualTo(new DeviceUnpairedIntegrationEvent(DEVICE_ID, EQUIPMENT_ID, AT));
  }


  @Test
  @DisplayName("DeviceStatusChangedEventHandler.on(DeviceStatusChangedEvent) -> publishes an integration event carrying the owner (AAA)")
  void statusChangedPublishesIntegrationEvent() {
    // Arrange
    var handler = new DeviceStatusChangedEventHandler(eventPublisher, externalAssetManagementService);
    var captor = ArgumentCaptor.forClass(Object.class);
    when(externalAssetManagementService.fetchOwnerId(EQUIPMENT_ID)).thenReturn(Optional.of(OWNER_ID));

    // Act
    handler.on(new DeviceStatusChangedEvent(
        DEVICE_ID, EQUIPMENT_ID, EQUIPMENT_ID, DeviceStatus.PAIRED, DeviceStatus.REVOKED, AT));

    // Assert
    verify(eventPublisher, times(1)).publishEvent(captor.capture());
    assertThat(captor.getValue()).isEqualTo(new DeviceStatusChangedIntegrationEvent(
        DEVICE_ID, OWNER_ID, EQUIPMENT_ID, "PAIRED", "REVOKED", AT));
  }

  @Test
  @DisplayName("DeviceStatusChangedEventHandler.on(DeviceStatusChangedEvent) -> publishes nothing when the equipment has no owner (AAA)")
  void statusChangedWithoutOwner() {
    // Arrange
    var handler = new DeviceStatusChangedEventHandler(eventPublisher, externalAssetManagementService);
    when(externalAssetManagementService.fetchOwnerId(EQUIPMENT_ID)).thenReturn(Optional.empty());

    // Act
    handler.on(new DeviceStatusChangedEvent(
        DEVICE_ID, EQUIPMENT_ID, null, DeviceStatus.PAIRED, DeviceStatus.UNPAIRED, AT));

    // Assert
    verifyNoInteractions(eventPublisher);
  }
}
