package pe.edu.upc.ice.track.platform.assets.application.internal.eventhandlers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.assets.domain.model.events.EquipmentRegisteredEvent;
import pe.edu.upc.ice.track.platform.assets.domain.repositories.SiteRepository;
import pe.edu.upc.ice.track.platform.assets.test.fixtures.AssetsDomainFixtures;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EquipmentRegisteredEventHandlerTest {

  @Mock
  private SiteRepository siteRepository;

  @InjectMocks
  private EquipmentRegisteredEventHandler handler;

  @Test
  @DisplayName("on(EquipmentRegisteredEvent) -> increments the equipment count of the site and saves it (AAA)")
  void onIncrementsEquipmentCount() {
    // Arrange
    var site = AssetsDomainFixtures.siteBuilder().equipmentCount(2).build();
    var event = EquipmentRegisteredEvent.from(AssetsDomainFixtures.equipment());
    when(siteRepository.findById(AssetsDomainFixtures.SITE_ID)).thenReturn(Optional.of(site));

    // Act
    handler.on(event);

    // Assert
    assertThat(site.getEquipmentCount()).isEqualTo(3);
    verify(siteRepository, times(1)).save(site);
  }

  @Test
  @DisplayName("on(EquipmentRegisteredEvent) -> saves nothing when the site no longer exists (AAA)")
  void onSiteNotFound() {
    // Arrange
    var event = EquipmentRegisteredEvent.from(AssetsDomainFixtures.equipment());
    when(siteRepository.findById(AssetsDomainFixtures.SITE_ID)).thenReturn(Optional.empty());

    // Act
    handler.on(event);

    // Assert
    verify(siteRepository, never()).save(any());
  }
}
