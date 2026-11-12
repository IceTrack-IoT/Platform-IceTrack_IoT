package pe.edu.upc.ice.track.platform.assets.interfaces.acl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.assets.application.commandservices.EquipmentCommandService;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.MarkEquipmentOfflineCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.RecordEquipmentReadingCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.EquipmentPage;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.StatusEquipment;
import pe.edu.upc.ice.track.platform.assets.domain.repositories.EquipmentRepository;
import pe.edu.upc.ice.track.platform.assets.domain.repositories.SiteRepository;
import pe.edu.upc.ice.track.platform.assets.test.fixtures.AssetsDomainFixtures;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.assets.test.fixtures.AssetsDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class AssetContextFacadeTest {

  @Mock
  private EquipmentRepository equipmentRepository;
  @Mock
  private SiteRepository siteRepository;
  @Mock
  private EquipmentCommandService equipmentCommandService;

  @InjectMocks
  private AssetContextFacade facade;

  @Test
  @DisplayName("getEquipmentThreshold(Long) -> returns the upper bound of the unit's threshold (AAA)")
  void getEquipmentThresholdSuccess() {
    // Arrange
    when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(AssetsDomainFixtures.equipment()));

    // Act
    var result = facade.getEquipmentThreshold(EQUIPMENT_ID);

    // Assert
    assertThat(result).contains(AssetsDomainFixtures.VALID_MAX_CELSIUS);
  }

  @Test
  @DisplayName("getEquipmentThreshold(Long) -> returns empty for a null id without reading the repository (AAA)")
  void getEquipmentThresholdNullId() {
    // Act
    var result = facade.getEquipmentThreshold(null);

    // Assert
    assertThat(result).isEmpty();
    verifyNoInteractions(equipmentRepository);
  }

  @Test
  @DisplayName("getEquipmentThreshold(Long) -> returns empty when the unit does not exist (AAA)")
  void getEquipmentThresholdMissing() {
    // Arrange
    when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.empty());

    // Act
    var result = facade.getEquipmentThreshold(EQUIPMENT_ID);

    // Assert
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("getEquipmentThresholdRange(Long) -> returns [min, max] of the unit's threshold (AAA)")
  void getEquipmentThresholdRangeSuccess() {
    // Arrange
    when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(AssetsDomainFixtures.equipment()));

    // Act
    var result = facade.getEquipmentThresholdRange(EQUIPMENT_ID);

    // Assert
    assertThat(result).isPresent();
    assertThat(result.get()).containsExactly(AssetsDomainFixtures.VALID_MIN_CELSIUS, AssetsDomainFixtures.VALID_MAX_CELSIUS);
  }

  @Test
  @DisplayName("getEquipmentThresholdRange(Long) -> returns empty when the unit does not exist (AAA)")
  void getEquipmentThresholdRangeMissing() {
    // Arrange
    when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.empty());

    // Act & Assert
    assertThat(facade.getEquipmentThresholdRange(EQUIPMENT_ID)).isEmpty();
  }

  @Test
  @DisplayName("getEquipmentConfigVersion(Long) -> delegates to the repository projection (AAA)")
  void getEquipmentConfigVersionDelegates() {
    // Arrange
    when(equipmentRepository.findConfigVersion(EQUIPMENT_ID)).thenReturn(Optional.of(4));

    // Act
    var result = facade.getEquipmentConfigVersion(EQUIPMENT_ID);

    // Assert
    assertThat(result).contains(4);
    verify(equipmentRepository, times(1)).findConfigVersion(EQUIPMENT_ID);
  }

  @Test
  @DisplayName("getEquipmentOwnerId(Long) -> returns the owner of the unit's site (AAA)")
  void getEquipmentOwnerIdSuccess() {
    // Arrange
    when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(AssetsDomainFixtures.equipment()));
    when(siteRepository.findById(SITE_ID)).thenReturn(Optional.of(AssetsDomainFixtures.site()));

    // Act
    var result = facade.getEquipmentOwnerId(EQUIPMENT_ID);

    // Assert
    assertThat(result).contains(OWNER_ID);
  }

  @Test
  @DisplayName("getEquipmentOwnerId(Long) -> returns empty when the unit does not exist (AAA)")
  void getEquipmentOwnerIdMissingUnit() {
    // Arrange
    when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.empty());

    // Act
    var result = facade.getEquipmentOwnerId(EQUIPMENT_ID);

    // Assert
    assertThat(result).isEmpty();
    verifyNoInteractions(siteRepository);
  }

  @Test
  @DisplayName("getEquipmentOwnerId(Long) -> returns empty when the unit's site does not exist (AAA)")
  void getEquipmentOwnerIdMissingSite() {
    // Arrange
    when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(AssetsDomainFixtures.equipment()));
    when(siteRepository.findById(SITE_ID)).thenReturn(Optional.empty());

    // Act & Assert
    assertThat(facade.getEquipmentOwnerId(EQUIPMENT_ID)).isEmpty();
  }

  @Test
  @DisplayName("existsEquipmentForOwner(Long, Long) -> true when the unit's site belongs to the owner (AAA)")
  void existsEquipmentForOwnerTrue() {
    // Arrange
    when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(AssetsDomainFixtures.equipment()));
    when(siteRepository.findById(SITE_ID)).thenReturn(Optional.of(AssetsDomainFixtures.site()));

    // Act & Assert
    assertThat(facade.existsEquipmentForOwner(EQUIPMENT_ID, OWNER_ID)).isTrue();
  }

  @Test
  @DisplayName("existsEquipmentForOwner(Long, Long) -> false when the site belongs to another owner (AAA)")
  void existsEquipmentForOwnerForeign() {
    // Arrange
    when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(AssetsDomainFixtures.equipment()));
    when(siteRepository.findById(SITE_ID)).thenReturn(Optional.of(AssetsDomainFixtures.site()));

    // Act & Assert
    assertThat(facade.existsEquipmentForOwner(EQUIPMENT_ID, OTHER_OWNER_ID)).isFalse();
  }

  @Test
  @DisplayName("existsEquipmentForOwner(Long, Long) -> false for null arguments without reading anything (AAA)")
  void existsEquipmentForOwnerNullArguments() {
    // Act & Assert
    assertThat(facade.existsEquipmentForOwner(null, OWNER_ID)).isFalse();
    assertThat(facade.existsEquipmentForOwner(EQUIPMENT_ID, null)).isFalse();
    verifyNoInteractions(equipmentRepository, siteRepository);
  }

  @Test
  @DisplayName("existsSiteForOwner(Long, Long) -> true when the site belongs to the owner (AAA)")
  void existsSiteForOwnerTrue() {
    // Arrange
    when(siteRepository.findById(SITE_ID)).thenReturn(Optional.of(AssetsDomainFixtures.site()));

    // Act & Assert
    assertThat(facade.existsSiteForOwner(SITE_ID, OWNER_ID)).isTrue();
  }

  @Test
  @DisplayName("existsSiteForOwner(Long, Long) -> false when the site belongs to another owner (AAA)")
  void existsSiteForOwnerForeign() {
    // Arrange
    when(siteRepository.findById(SITE_ID)).thenReturn(Optional.of(AssetsDomainFixtures.site()));

    // Act & Assert
    assertThat(facade.existsSiteForOwner(SITE_ID, OTHER_OWNER_ID)).isFalse();
  }

  @Test
  @DisplayName("existsSiteForOwner(Long, Long) -> false for null arguments without reading the repository (AAA)")
  void existsSiteForOwnerNullArguments() {
    // Act & Assert
    assertThat(facade.existsSiteForOwner(null, OWNER_ID)).isFalse();
    assertThat(facade.existsSiteForOwner(SITE_ID, null)).isFalse();
    verifyNoInteractions(siteRepository);
  }

  @Test
  @DisplayName("getSiteIdByEquipment(Long) -> returns the site the unit is installed at (AAA)")
  void getSiteIdByEquipmentSuccess() {
    // Arrange
    when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(AssetsDomainFixtures.equipment()));

    // Act & Assert
    assertThat(facade.getSiteIdByEquipment(EQUIPMENT_ID)).contains(SITE_ID);
  }

  @Test
  @DisplayName("fetchEquipmentIdsBySite(Long) -> returns the ids of the units of the site (AAA)")
  void fetchEquipmentIdsBySiteSuccess() {
    // Arrange
    var other = AssetsDomainFixtures.equipmentBuilder().id(OTHER_EQUIPMENT_ID).uid("ESP32-0002").build();
    when(equipmentRepository.findBySiteId(SITE_ID)).thenReturn(List.of(AssetsDomainFixtures.equipment(), other));

    // Act & Assert
    assertThat(facade.fetchEquipmentIdsBySite(SITE_ID)).containsExactly(EQUIPMENT_ID, OTHER_EQUIPMENT_ID);
  }

  @Test
  @DisplayName("fetchEquipmentIdsBySite(Long) -> returns an empty list for a null site without reading (AAA)")
  void fetchEquipmentIdsBySiteNull() {
    // Act & Assert
    assertThat(facade.fetchEquipmentIdsBySite(null)).isEmpty();
    verifyNoInteractions(equipmentRepository);
  }

  @Test
  @DisplayName("fetchEquipmentSummariesByOwner(Long, Long) -> maps every unit of the owner to a summary (AAA)")
  void fetchSummariesByOwnerSuccess() {
    // Arrange
    var lastReadingAt = Instant.parse("2026-01-01T10:00:00Z");
    var unit = AssetsDomainFixtures.equipmentBuilder().lastReading(lastReadingAt, 5.0).build();
    unit.attachSiteName("Sede Central");
    when(equipmentRepository.findAllByOwnerId(OWNER_ID, SITE_ID, Set.of())).thenReturn(List.of(unit));

    // Act
    var result = facade.fetchEquipmentSummariesByOwner(OWNER_ID, SITE_ID);

    // Assert
    assertThat(result).containsExactly(
        new EquipmentSummary(EQUIPMENT_ID, AssetsDomainFixtures.VALID_EQUIPMENT_NAME, "ACTIVE", SITE_ID,
            "Sede Central", lastReadingAt));
  }

  @Test
  @DisplayName("fetchEquipmentSummariesByOwner(Long, Long) -> returns an empty list for a null owner without reading (AAA)")
  void fetchSummariesByOwnerNullOwner() {
    // Act & Assert
    assertThat(facade.fetchEquipmentSummariesByOwner(null, SITE_ID)).isEmpty();
    verifyNoInteractions(equipmentRepository);
  }

  @Test
  @DisplayName("fetchEquipmentSummary(Long) -> maps the unit to a summary (AAA)")
  void fetchSummarySuccess() {
    // Arrange
    when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(AssetsDomainFixtures.equipment()));

    // Act
    var result = facade.fetchEquipmentSummary(EQUIPMENT_ID);

    // Assert
    assertThat(result).isPresent();
    assertThat(result.get().equipmentId()).isEqualTo(EQUIPMENT_ID);
    assertThat(result.get().status()).isEqualTo("ACTIVE");
    assertThat(result.get().siteName()).isNull();
  }

  @Test
  @DisplayName("fetchEquipmentSummary(Long) -> returns empty for a null id (AAA)")
  void fetchSummaryNullId() {
    // Act & Assert
    assertThat(facade.fetchEquipmentSummary(null)).isEmpty();
    verifyNoInteractions(equipmentRepository);
  }

  @Test
  @DisplayName("fetchSiteNamesByOwner(Long) -> maps site ids to names, skipping unsaved sites (AAA)")
  void fetchSiteNamesByOwnerSuccess() {
    // Arrange
    var unsaved = AssetsDomainFixtures.siteBuilder().id(null).build();
    when(siteRepository.findByOwnerId(OWNER_ID)).thenReturn(List.of(AssetsDomainFixtures.site(), unsaved));

    // Act
    var result = facade.fetchSiteNamesByOwner(OWNER_ID);

    // Assert
    assertThat(result).containsOnly(java.util.Map.entry(SITE_ID, AssetsDomainFixtures.VALID_SITE_NAME));
  }

  @Test
  @DisplayName("fetchSiteNamesByOwner(Long) -> returns an empty map for a null owner (AAA)")
  void fetchSiteNamesByOwnerNull() {
    // Act & Assert
    assertThat(facade.fetchSiteNamesByOwner(null)).isEmpty();
    verifyNoInteractions(siteRepository);
  }

  @Test
  @DisplayName("countSitesByOwner(Long) -> counts the sites of the owner (AAA)")
  void countSitesByOwnerSuccess() {
    // Arrange
    when(siteRepository.findByOwnerId(OWNER_ID))
        .thenReturn(List.of(AssetsDomainFixtures.site(), AssetsDomainFixtures.siteBuilder().id(9L).build()));

    // Act & Assert
    assertThat(facade.countSitesByOwner(OWNER_ID)).isEqualTo(2);
  }

  @Test
  @DisplayName("countSitesByOwner(Long) -> returns 0 for a null owner (AAA)")
  void countSitesByOwnerNull() {
    // Act & Assert
    assertThat(facade.countSitesByOwner(null)).isZero();
    verifyNoInteractions(siteRepository);
  }

  @Test
  @DisplayName("countEquipmentByOwner(Long) -> reads the total of a one row page (AAA)")
  void countEquipmentByOwnerSuccess() {
    // Arrange
    when(equipmentRepository.findPageByOwnerId(OWNER_ID, null, null, null, null, 0, 1))
        .thenReturn(new EquipmentPage(List.of(AssetsDomainFixtures.equipment()), 0, 1, 37));

    // Act & Assert
    assertThat(facade.countEquipmentByOwner(OWNER_ID)).isEqualTo(37L);
  }

  @Test
  @DisplayName("countEquipmentByOwner(Long) -> returns 0 for a null owner (AAA)")
  void countEquipmentByOwnerNull() {
    // Act & Assert
    assertThat(facade.countEquipmentByOwner(null)).isZero();
    verifyNoInteractions(equipmentRepository);
  }

  @Test
  @DisplayName("fetchEquipmentStatusNames() -> lists every StatusEquipment name (AAA)")
  void fetchEquipmentStatusNames() {
    // Act
    var result = facade.fetchEquipmentStatusNames();

    // Assert
    assertThat(result).containsExactlyInAnyOrder(
        java.util.Arrays.stream(StatusEquipment.values()).map(Enum::name).toArray(String[]::new));
  }

  @Test
  @DisplayName("getEquipmentIdByUid(String) -> returns the id of the unit with that uid (AAA)")
  void getEquipmentIdByUidSuccess() {
    // Arrange
    when(equipmentRepository.findByUid(VALID_UID)).thenReturn(Optional.of(AssetsDomainFixtures.equipment()));

    // Act & Assert
    assertThat(facade.getEquipmentIdByUid(VALID_UID)).contains(EQUIPMENT_ID);
  }

  @ParameterizedTest(name = "[{index}] uid={0}")
  @MethodSource("pe.edu.upc.ice.track.platform.shared.test.fixtures.CommonCommandFixtures#invalidBlankStrings")
  @DisplayName("getEquipmentIdByUid(String) -> returns empty for a blank uid without reading (AAA)")
  void getEquipmentIdByUidBlank(String uid) {
    // Act & Assert
    assertThat(facade.getEquipmentIdByUid(uid)).isEmpty();
    verifyNoInteractions(equipmentRepository);
  }

  @Test
  @DisplayName("recordEquipmentReading(Long, Double, Instant, Instant) -> delegates a RecordEquipmentReadingCommand (AAA)")
  void recordEquipmentReadingDelegates() {
    // Arrange
    var recordedAt = Instant.parse("2026-01-01T10:00:00Z");
    var receivedAt = Instant.parse("2026-01-01T10:00:02Z");
    var captor = ArgumentCaptor.forClass(RecordEquipmentReadingCommand.class);
    when(equipmentCommandService.handle(any(RecordEquipmentReadingCommand.class))).thenReturn(true);

    // Act
    var result = facade.recordEquipmentReading(EQUIPMENT_ID, 4.5, recordedAt, receivedAt);

    // Assert
    assertThat(result).isTrue();
    verify(equipmentCommandService).handle(captor.capture());
    assertThat(captor.getValue()).isEqualTo(new RecordEquipmentReadingCommand(EQUIPMENT_ID, 4.5, recordedAt, receivedAt));
  }

  @Test
  @DisplayName("recordEquipmentReading(Long, Double, Instant, Instant) -> false without delegating when the id or recordedAt is null (AAA)")
  void recordEquipmentReadingInvalidArguments() {
    // Act & Assert
    assertThat(facade.recordEquipmentReading(null, 4.5, Instant.now(), Instant.now())).isFalse();
    assertThat(facade.recordEquipmentReading(EQUIPMENT_ID, 4.5, null, Instant.now())).isFalse();
    verifyNoInteractions(equipmentCommandService);
  }

  @Test
  @DisplayName("markEquipmentOffline(Long) -> delegates a MarkEquipmentOfflineCommand and returns its answer (AAA)")
  void markEquipmentOfflineDelegates() {
    // Arrange
    when(equipmentCommandService.handle(new MarkEquipmentOfflineCommand(EQUIPMENT_ID))).thenReturn(true);

    // Act & Assert
    assertThat(facade.markEquipmentOffline(EQUIPMENT_ID)).isTrue();
  }

  @Test
  @DisplayName("markEquipmentOffline(Long) -> false without delegating for a null id (AAA)")
  void markEquipmentOfflineNullId() {
    // Act & Assert
    assertThat(facade.markEquipmentOffline(null)).isFalse();
    verify(equipmentCommandService, never()).handle(any(MarkEquipmentOfflineCommand.class));
  }

  @Test
  @DisplayName("fetchOnlineEquipmentIds() -> returns the ids of the units the repository reports online (AAA)")
  void fetchOnlineEquipmentIds() {
    // Arrange
    when(equipmentRepository.findOnline()).thenReturn(List.of(AssetsDomainFixtures.equipmentBuilder().online(true).build()));

    // Act & Assert
    assertThat(facade.fetchOnlineEquipmentIds()).containsExactly(EQUIPMENT_ID);
  }
}
