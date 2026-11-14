package pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.acl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.assets.interfaces.acl.AssetContextFacade;
import pe.edu.upc.ice.track.platform.assets.interfaces.acl.EquipmentSummary;
import pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.acl.ExternalAssetManagementServiceForDevice.PairedEquipment;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.device.test.fixtures.DeviceDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class ExternalAssetManagementServiceForDeviceTest {

  @Mock
  private AssetContextFacade assetContextFacade;

  @InjectMocks
  private ExternalAssetManagementServiceForDevice service;


  @Test
  @DisplayName("existsEquipmentForOwner(Long, Long) -> delegates to the assets facade (AAA)")
  void existsEquipmentForOwnerDelegates() {
    // Arrange
    when(assetContextFacade.existsEquipmentForOwner(EQUIPMENT_ID, OWNER_ID)).thenReturn(true);

    // Act & Assert
    assertThat(service.existsEquipmentForOwner(EQUIPMENT_ID, OWNER_ID)).isTrue();
  }

  @Test
  @DisplayName("existsEquipmentForOwner(Long, Long) -> false without calling assets when an argument is null (AAA)")
  void existsEquipmentForOwnerNullArguments() {
    // Act & Assert
    assertThat(service.existsEquipmentForOwner(null, OWNER_ID)).isFalse();
    assertThat(service.existsEquipmentForOwner(EQUIPMENT_ID, null)).isFalse();
    verifyNoInteractions(assetContextFacade);
  }


  @Test
  @DisplayName("existsSiteForOwner(Long, Long) -> delegates to the assets facade (AAA)")
  void existsSiteForOwnerDelegates() {
    // Arrange
    when(assetContextFacade.existsSiteForOwner(SITE_ID, OWNER_ID)).thenReturn(true);

    // Act & Assert
    assertThat(service.existsSiteForOwner(SITE_ID, OWNER_ID)).isTrue();
  }

  @Test
  @DisplayName("existsSiteForOwner(Long, Long) -> false without calling assets when an argument is null (AAA)")
  void existsSiteForOwnerNullArguments() {
    // Act & Assert
    assertThat(service.existsSiteForOwner(null, OWNER_ID)).isFalse();
    assertThat(service.existsSiteForOwner(SITE_ID, null)).isFalse();
    verifyNoInteractions(assetContextFacade);
  }


  @Test
  @DisplayName("fetchOwnerId(Long) -> returns the owner the assets facade resolves (AAA)")
  void fetchOwnerIdDelegates() {
    // Arrange
    when(assetContextFacade.getEquipmentOwnerId(EQUIPMENT_ID)).thenReturn(Optional.of(OWNER_ID));

    // Act & Assert
    assertThat(service.fetchOwnerId(EQUIPMENT_ID)).contains(OWNER_ID);
  }

  @Test
  @DisplayName("fetchOwnerId(Long) -> empty without calling assets for a null equipment id (AAA)")
  void fetchOwnerIdNull() {
    // Act & Assert
    assertThat(service.fetchOwnerId(null)).isEmpty();
    verifyNoInteractions(assetContextFacade);
  }

  @Test
  @DisplayName("fetchConfigVersion(Long) -> delegates to the assets facade (AAA)")
  void fetchConfigVersionDelegates() {
    // Arrange
    when(assetContextFacade.getEquipmentConfigVersion(EQUIPMENT_ID)).thenReturn(Optional.of(7));

    // Act & Assert
    assertThat(service.fetchConfigVersion(EQUIPMENT_ID)).contains(7);
  }

  @Test
  @DisplayName("fetchThresholdRange(Long) -> delegates to the assets facade (AAA)")
  void fetchThresholdRangeDelegates() {
    // Arrange
    when(assetContextFacade.getEquipmentThresholdRange(EQUIPMENT_ID)).thenReturn(Optional.of(new double[] {2.0, 8.0}));

    // Act
    var result = service.fetchThresholdRange(EQUIPMENT_ID);

    // Assert
    assertThat(result).isPresent();
    assertThat(result.get()).containsExactly(2.0, 8.0);
  }


  @Test
  @DisplayName("fetchEquipmentOwnedBy(Long, Long) -> maps the summaries to paired equipment (AAA)")
  void fetchEquipmentOwnedByMapsSummaries() {
    // Arrange
    when(assetContextFacade.fetchEquipmentSummariesByOwner(OWNER_ID, SITE_ID)).thenReturn(List.of(
        new EquipmentSummary(EQUIPMENT_ID, "Refrigerador 1", "ACTIVE", SITE_ID, "Sede Central", null),
        new EquipmentSummary(OTHER_EQUIPMENT_ID, "Cámara 2", "MAINTENANCE", SITE_ID, "Sede Central", null)));

    // Act
    var result = service.fetchEquipmentOwnedBy(OWNER_ID, SITE_ID);

    // Assert
    assertThat(result).containsExactly(
        new PairedEquipment(EQUIPMENT_ID, "Refrigerador 1", SITE_ID),
        new PairedEquipment(OTHER_EQUIPMENT_ID, "Cámara 2", SITE_ID));
  }

  @Test
  @DisplayName("fetchEquipmentOwnedBy(Long, Long) -> empty list without calling assets for a null owner (AAA)")
  void fetchEquipmentOwnedByNullOwner() {
    // Act & Assert
    assertThat(service.fetchEquipmentOwnedBy(null, SITE_ID)).isEmpty();
    verifyNoInteractions(assetContextFacade);
  }

  // ---------- fetchPairedEquipment ----------

  @Test
  @DisplayName("fetchPairedEquipment(Long) -> maps the summary to paired equipment (AAA)")
  void fetchPairedEquipmentMapsSummary() {
    // Arrange
    when(assetContextFacade.fetchEquipmentSummary(EQUIPMENT_ID)).thenReturn(Optional.of(
        new EquipmentSummary(EQUIPMENT_ID, "Refrigerador 1", "ACTIVE", SITE_ID, "Sede Central", null)));

    // Act
    var result = service.fetchPairedEquipment(EQUIPMENT_ID);

    // Assert
    assertThat(result).contains(new PairedEquipment(EQUIPMENT_ID, "Refrigerador 1", SITE_ID));
    verify(assetContextFacade, times(1)).fetchEquipmentSummary(EQUIPMENT_ID);
  }

  @Test
  @DisplayName("fetchPairedEquipment(Long) -> empty when the equipment is unknown (AAA)")
  void fetchPairedEquipmentUnknown() {
    // Arrange
    when(assetContextFacade.fetchEquipmentSummary(EQUIPMENT_ID)).thenReturn(Optional.empty());

    // Act & Assert
    assertThat(service.fetchPairedEquipment(EQUIPMENT_ID)).isEmpty();
  }

  @Test
  @DisplayName("fetchPairedEquipment(Long) -> empty without calling assets for a null id (AAA)")
  void fetchPairedEquipmentNull() {
    // Act & Assert
    assertThat(service.fetchPairedEquipment(null)).isEmpty();
    verifyNoInteractions(assetContextFacade);
  }
}
