package pe.edu.upc.ice.track.platform.assets.application.internal.eventhandlers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import pe.edu.upc.ice.track.platform.assets.domain.model.events.EquipmentConnectivityChangedEvent;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.ConnectivityState;
import pe.edu.upc.ice.track.platform.assets.domain.repositories.SiteRepository;
import pe.edu.upc.ice.track.platform.assets.interfaces.events.EquipmentConnectivityChangedIntegrationEvent;
import pe.edu.upc.ice.track.platform.assets.test.fixtures.AssetsDomainFixtures;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.assets.test.fixtures.AssetsDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class EquipmentConnectivityChangedEventHandlerTest {

  @Mock
  private ApplicationEventPublisher eventPublisher;
  @Mock
  private SiteRepository siteRepository;

  @InjectMocks
  private EquipmentConnectivityChangedEventHandler handler;

  private EquipmentConnectivityChangedEvent event(Instant lastReceivedAt, Instant changedAt) {
    return new EquipmentConnectivityChangedEvent(
        EQUIPMENT_ID, SITE_ID, ConnectivityState.REPORTING, ConnectivityState.SILENT, lastReceivedAt, true, changedAt);
  }

  @Test
  @DisplayName("on(EquipmentConnectivityChangedEvent) -> publishes an integration event carrying the site owner (AAA)")
  void onPublishesIntegrationEvent() {
    // Arrange
    var lastReceivedAt = Instant.parse("2026-01-01T10:00:00Z");
    var changedAt = Instant.parse("2026-01-01T10:05:00Z");
    var captor = ArgumentCaptor.forClass(Object.class);
    when(siteRepository.findById(SITE_ID)).thenReturn(Optional.of(AssetsDomainFixtures.site()));

    // Act
    handler.on(event(lastReceivedAt, changedAt));

    // Assert
    verify(eventPublisher, times(1)).publishEvent(captor.capture());
    assertThat(captor.getValue()).isInstanceOf(EquipmentConnectivityChangedIntegrationEvent.class);
    var published = (EquipmentConnectivityChangedIntegrationEvent) captor.getValue();
    assertThat(published.equipmentId()).isEqualTo(EQUIPMENT_ID);
    assertThat(published.ownerProfileId()).isEqualTo(OWNER_ID);
    assertThat(published.previousState()).isEqualTo("REPORTING");
    assertThat(published.state()).isEqualTo("SILENT");
    assertThat(published.lastReceivedAt()).isEqualTo(lastReceivedAt);
    assertThat(published.underMaintenance()).isTrue();
    assertThat(published.changedAt()).isEqualTo(changedAt);
  }

  @Test
  @DisplayName("on(EquipmentConnectivityChangedEvent) -> publishes nothing when the site no longer exists (AAA)")
  void onSiteNotFound() {
    // Arrange
    when(siteRepository.findById(SITE_ID)).thenReturn(Optional.empty());

    // Act
    handler.on(event(null, Instant.now()));

    // Assert
    verifyNoInteractions(eventPublisher);
  }
}
