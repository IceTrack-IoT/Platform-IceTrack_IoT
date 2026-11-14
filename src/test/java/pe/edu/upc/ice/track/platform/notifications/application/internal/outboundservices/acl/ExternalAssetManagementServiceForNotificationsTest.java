package pe.edu.upc.ice.track.platform.notifications.application.internal.outboundservices.acl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.assets.interfaces.acl.AssetContextFacade;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.notifications.test.fixtures.NotificationsDomainFixtures.EQUIPMENT_ID;
import static pe.edu.upc.ice.track.platform.notifications.test.fixtures.NotificationsDomainFixtures.OWNER_ID;

@ExtendWith(MockitoExtension.class)
class ExternalAssetManagementServiceForNotificationsTest {

  @Mock
  private AssetContextFacade assetContextFacade;

  @InjectMocks
  private ExternalAssetManagementServiceForNotifications service;

  @Test
  @DisplayName("fetchEquipmentOwnerId(Long) -> delegates to the facade (AAA)")
  void ownerDelegates() {
    // Arrange
    when(assetContextFacade.getEquipmentOwnerId(EQUIPMENT_ID)).thenReturn(Optional.of(OWNER_ID));

    // Act
    var result = service.fetchEquipmentOwnerId(EQUIPMENT_ID);

    // Assert
    assertThat(result).contains(OWNER_ID);
    verify(assetContextFacade, times(1)).getEquipmentOwnerId(EQUIPMENT_ID);
  }

  @Test
  @DisplayName("fetchEquipmentOwnerId(Long) unknown equipment -> empty (AAA)")
  void ownerUnknown() {
    // Arrange
    when(assetContextFacade.getEquipmentOwnerId(EQUIPMENT_ID)).thenReturn(Optional.empty());

    // Act & Assert
    assertThat(service.fetchEquipmentOwnerId(EQUIPMENT_ID)).isEmpty();
  }

  @Test
  @DisplayName("fetchEquipmentName(null) -> empty without calling the facade (AAA)")
  void nameNullId() {
    // Act
    var result = service.fetchEquipmentName(null);

    // Assert
    assertThat(result).isEmpty();
    verifyNoInteractions(assetContextFacade);
  }

  @Test
  @DisplayName("fetchEquipmentName(Long) -> name taken from the equipment summary (AAA)")
  void nameFromSummary() {
    // Arrange
    when(assetContextFacade.fetchEquipmentSummary(EQUIPMENT_ID))
        .thenReturn(Optional.of(new pe.edu.upc.ice.track.platform.assets.interfaces.acl.EquipmentSummary(
            EQUIPMENT_ID, "Cold Room #01", "ACTIVE", 5L, "Main Site", null)));

    // Act & Assert
    assertThat(service.fetchEquipmentName(EQUIPMENT_ID)).contains("Cold Room #01");
  }

  @Test
  @DisplayName("fetchEquipmentName(Long) unknown equipment -> empty (AAA)")
  void nameUnknown() {
    // Arrange
    when(assetContextFacade.fetchEquipmentSummary(EQUIPMENT_ID)).thenReturn(Optional.empty());

    // Act & Assert
    assertThat(service.fetchEquipmentName(EQUIPMENT_ID)).isEmpty();
  }

  @Test
  @DisplayName("fetchThresholdRange(Long) -> delegates to the facade (AAA)")
  void thresholdDelegates() {
    // Arrange
    var range = new double[] {2.0, 8.0};
    when(assetContextFacade.getEquipmentThresholdRange(EQUIPMENT_ID)).thenReturn(Optional.of(range));

    // Act & Assert
    assertThat(service.fetchThresholdRange(EQUIPMENT_ID)).containsSame(range);
  }
}
