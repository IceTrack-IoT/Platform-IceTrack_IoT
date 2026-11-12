package pe.edu.upc.ice.track.platform.assets.application.internal.queryservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.acl.AssetsExternalMonitoringService;
import pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.acl.AssetsExternalOwnerService;
import pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.acl.AssetsExternalServiceRequestService;
import pe.edu.upc.ice.track.platform.assets.domain.model.queries.GetEquipmentByOwnerQuery;
import pe.edu.upc.ice.track.platform.assets.domain.model.queries.GetEquipmentConnectivityQuery;
import pe.edu.upc.ice.track.platform.assets.domain.model.queries.GetEquipmentDirectoryQuery;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.ConnectivityState;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.EquipmentPage;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.EquipmentType;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.StatusEquipment;
import pe.edu.upc.ice.track.platform.assets.domain.repositories.EquipmentRepository;
import pe.edu.upc.ice.track.platform.assets.domain.repositories.SiteRepository;
import pe.edu.upc.ice.track.platform.assets.test.fixtures.AssetsCommandFixtures;
import pe.edu.upc.ice.track.platform.assets.test.fixtures.AssetsDomainFixtures;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.CallerRole;
import pe.edu.upc.ice.track.platform.shared.test.util.ResultTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static pe.edu.upc.ice.track.platform.assets.test.fixtures.AssetsDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class EquipmentQueryServiceImplTest {

  @Mock
  private EquipmentRepository equipmentRepository;
  @Mock
  private SiteRepository siteRepository;
  @Mock
  private AssetsExternalOwnerService assetsExternalOwnerService;
  @Mock
  private AssetsExternalServiceRequestService assetsExternalServiceRequestService;
  @Mock
  private AssetsExternalMonitoringService assetsExternalMonitoringService;

  @InjectMocks
  private EquipmentQueryServiceImpl service;

  private void stubOwner() {
    when(assetsExternalOwnerService.fetchOwnerIdByUsername(USERNAME)).thenReturn(Optional.of(OWNER_ID));
  }

  private void stubNoOwnerProfile() {
    when(assetsExternalOwnerService.fetchOwnerIdByUsername(USERNAME)).thenReturn(Optional.empty());
  }

  private void stubOwnedSite() {
    when(siteRepository.findById(SITE_ID)).thenReturn(Optional.of(AssetsDomainFixtures.site()));
  }

  private void stubForeignSite() {
    when(siteRepository.findById(SITE_ID))
        .thenReturn(Optional.of(AssetsDomainFixtures.siteBuilder().ownerId(OTHER_OWNER_ID).build()));
  }

  @Test
  @DisplayName("handle(GetEquipmentByIdQuery) -> returns the unit when its site belongs to the owner (AAA)")
  void handleByIdOwnerOwnsUnit() {
    // Arrange
    var unit = AssetsDomainFixtures.equipment();
    stubOwner();
    when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(unit));
    stubOwnedSite();

    // Act
    var result = service.handle(AssetsCommandFixtures.equipmentByIdQuery(CallerRole.OWNER));

    // Assert
    assertThat(result).containsSame(unit);
    verifyNoInteractions(assetsExternalServiceRequestService);
  }

  @Test
  @DisplayName("handle(GetEquipmentByIdQuery) -> returns empty when the unit's site belongs to another owner (AAA)")
  void handleByIdForeignUnit() {
    // Arrange
    stubOwner();
    when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(AssetsDomainFixtures.equipment()));
    stubForeignSite();

    // Act
    var result = service.handle(AssetsCommandFixtures.equipmentByIdQuery(CallerRole.OWNER));

    // Assert
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("handle(GetEquipmentByIdQuery) -> returns empty when the unit does not exist (AAA)")
  void handleByIdMissingUnit() {
    // Arrange
    stubOwner();
    when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.empty());

    // Act
    var result = service.handle(AssetsCommandFixtures.equipmentByIdQuery(CallerRole.OWNER));

    // Assert
    assertThat(result).isEmpty();
    verifyNoInteractions(siteRepository);
  }

  @Test
  @DisplayName("handle(GetEquipmentByIdQuery) -> returns empty and never reads the unit when the caller has no owner profile (AAA)")
  void handleByIdNoOwnerProfile() {
    // Arrange
    stubNoOwnerProfile();

    // Act
    var result = service.handle(AssetsCommandFixtures.equipmentByIdQuery(CallerRole.OWNER));

    // Assert
    assertThat(result).isEmpty();
    verifyNoInteractions(equipmentRepository, siteRepository);
  }

  @Test
  @DisplayName("handle(GetEquipmentByIdQuery) -> returns the unit to a technician assigned to it (AAA)")
  void handleByIdTechnicianAssigned() {
    // Arrange
    var unit = AssetsDomainFixtures.equipment();
    when(assetsExternalServiceRequestService.isTechnicianAssignedToEquipment(TECHNICIAN_USERNAME, EQUIPMENT_ID))
        .thenReturn(true);
    when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(unit));

    // Act
    var result = service.handle(AssetsCommandFixtures.equipmentByIdQuery(CallerRole.TECHNICIAN));

    // Assert
    assertThat(result).containsSame(unit);
    verifyNoInteractions(assetsExternalOwnerService, siteRepository);
  }

  @Test
  @DisplayName("handle(GetEquipmentByIdQuery) -> returns empty and never reads the unit for an unassigned technician (AAA)")
  void handleByIdTechnicianNotAssigned() {
    // Arrange
    when(assetsExternalServiceRequestService.isTechnicianAssignedToEquipment(TECHNICIAN_USERNAME, EQUIPMENT_ID))
        .thenReturn(false);

    // Act
    var result = service.handle(AssetsCommandFixtures.equipmentByIdQuery(CallerRole.TECHNICIAN));

    // Assert
    assertThat(result).isEmpty();
    verifyNoInteractions(equipmentRepository, assetsExternalOwnerService);
  }

  // ====================================================================================
  // handle(GetEquipmentBySiteQuery)
  // ====================================================================================

  @Test
  @DisplayName("handle(GetEquipmentBySiteQuery) -> returns the units of an owned site (AAA)")
  void handleBySiteOwnerOwnsSite() {
    // Arrange
    var units = List.of(AssetsDomainFixtures.equipment());
    stubOwner();
    stubOwnedSite();
    when(equipmentRepository.findBySiteId(SITE_ID)).thenReturn(units);

    // Act
    var result = service.handle(AssetsCommandFixtures.equipmentBySiteQuery(CallerRole.OWNER));

    // Assert
    assertThat(result).isEqualTo(units);
  }

  @Test
  @DisplayName("handle(GetEquipmentBySiteQuery) -> returns an empty list for a site of another owner (AAA)")
  void handleBySiteForeignSite() {
    // Arrange
    stubOwner();
    stubForeignSite();

    // Act
    var result = service.handle(AssetsCommandFixtures.equipmentBySiteQuery(CallerRole.OWNER));

    // Assert
    assertThat(result).isEmpty();
    verifyNoInteractions(equipmentRepository);
  }

  @Test
  @DisplayName("handle(GetEquipmentBySiteQuery) -> returns an empty list when the caller has no owner profile (AAA)")
  void handleBySiteNoOwnerProfile() {
    // Arrange
    stubNoOwnerProfile();

    // Act
    var result = service.handle(AssetsCommandFixtures.equipmentBySiteQuery(CallerRole.OWNER));

    // Assert
    assertThat(result).isEmpty();
    verifyNoInteractions(siteRepository, equipmentRepository);
  }

  @Test
  @DisplayName("handle(GetEquipmentBySiteQuery) -> returns only the units a technician is assigned to (AAA)")
  void handleBySiteTechnicianOnlyAssignedUnits() {
    // Arrange
    var assigned = AssetsDomainFixtures.equipment();
    var notAssigned = AssetsDomainFixtures.equipmentBuilder().id(OTHER_EQUIPMENT_ID).uid("ESP32-0002").build();
    when(assetsExternalServiceRequestService.isTechnicianAssignedToSite(TECHNICIAN_USERNAME, SITE_ID)).thenReturn(true);
    when(equipmentRepository.findBySiteId(SITE_ID)).thenReturn(List.of(assigned, notAssigned));
    when(assetsExternalServiceRequestService.isTechnicianAssignedToEquipment(TECHNICIAN_USERNAME, EQUIPMENT_ID))
        .thenReturn(true);

    // Act
    var result = service.handle(AssetsCommandFixtures.equipmentBySiteQuery(CallerRole.TECHNICIAN));

    // Assert
    assertThat(result).containsExactly(assigned);
  }

  @Test
  @DisplayName("handle(GetEquipmentBySiteQuery) -> returns an empty list for a technician not assigned to the site (AAA)")
  void handleBySiteTechnicianNotAssignedToSite() {
    // Arrange
    when(assetsExternalServiceRequestService.isTechnicianAssignedToSite(TECHNICIAN_USERNAME, SITE_ID)).thenReturn(false);

    // Act
    var result = service.handle(AssetsCommandFixtures.equipmentBySiteQuery(CallerRole.TECHNICIAN));

    // Assert
    assertThat(result).isEmpty();
    verifyNoInteractions(equipmentRepository, assetsExternalOwnerService);
  }

  // ====================================================================================
  // handle(GetEquipmentByOwnerQuery)
  // ====================================================================================

  @Test
  @DisplayName("handle(GetEquipmentByOwnerQuery) -> delegates a paged query to the repository with the owner and filters (AAA)")
  void handleByOwnerPaged() {
    // Arrange
    var query = new GetEquipmentByOwnerQuery(
        USERNAME, "  fridge  ", SITE_ID, StatusEquipment.ACTIVE, EquipmentType.REFRIGERATOR, 2, 10);
    var page = new EquipmentPage(List.of(AssetsDomainFixtures.equipment()), 2, 10, 21);
    stubOwner();
    when(equipmentRepository.findPageByOwnerId(
        OWNER_ID, SITE_ID, StatusEquipment.ACTIVE, EquipmentType.REFRIGERATOR, "fridge", 2, 10)).thenReturn(page);

    // Act
    var result = service.handle(query);

    // Assert
    assertThat(result).isSameAs(page);
  }

  @Test
  @DisplayName("handle(GetEquipmentByOwnerQuery) -> answers an unpaged query with the first 500 rows as a single page (AAA)")
  void handleByOwnerUnpaged() {
    // Arrange
    var unit = AssetsDomainFixtures.equipment();
    stubOwner();
    when(equipmentRepository.findPageByOwnerId(OWNER_ID, null, null, null, null, 0, 500))
        .thenReturn(new EquipmentPage(List.of(unit), 0, 500, 730));

    // Act
    var result = service.handle(GetEquipmentByOwnerQuery.all(USERNAME));

    // Assert
    assertThat(result.content()).containsExactly(unit);
    assertThat(result.page()).isZero();
    assertThat(result.totalElements()).isEqualTo(730);
  }

  @Test
  @DisplayName("handle(GetEquipmentByOwnerQuery) -> returns an empty requested page when a paged caller has no owner profile (AAA)")
  void handleByOwnerNoProfilePaged() {
    // Arrange
    stubNoOwnerProfile();
    var query = new GetEquipmentByOwnerQuery(USERNAME, null, null, null, null, 3, 20);

    // Act
    var result = service.handle(query);

    // Assert
    assertThat(result.isEmpty()).isTrue();
    assertThat(result.page()).isEqualTo(3);
    assertThat(result.size()).isEqualTo(20);
    assertThat(result.totalElements()).isZero();
    verifyNoInteractions(equipmentRepository);
  }

  @Test
  @DisplayName("handle(GetEquipmentByOwnerQuery) -> returns an empty unpaged page when an unpaged caller has no owner profile (AAA)")
  void handleByOwnerNoProfileUnpaged() {
    // Arrange
    stubNoOwnerProfile();

    // Act
    var result = service.handle(GetEquipmentByOwnerQuery.all(USERNAME));

    // Assert
    assertThat(result.isEmpty()).isTrue();
    assertThat(result.page()).isZero();
    assertThat(result.totalElements()).isZero();
    verifyNoInteractions(equipmentRepository);
  }

  // ====================================================================================
  // handle(GetEquipmentDirectoryQuery)
  // ====================================================================================

  @Test
  @DisplayName("handle(GetEquipmentDirectoryQuery) -> lists the whole directory of the owner when no site is named (AAA)")
  void handleDirectoryWithoutSite() {
    // Arrange
    var statuses = Set.of(StatusEquipment.ACTIVE);
    var units = List.of(AssetsDomainFixtures.equipment());
    stubOwner();
    when(equipmentRepository.findAllByOwnerId(OWNER_ID, null, statuses)).thenReturn(units);

    // Act
    var result = service.handle(new GetEquipmentDirectoryQuery(USERNAME, null, statuses));

    // Assert
    assertThat(ResultTestUtils.successValue(result)).isEqualTo(units);
    verifyNoInteractions(siteRepository);
  }

  @Test
  @DisplayName("handle(GetEquipmentDirectoryQuery) -> lists the units of an owned site when the filter names it (AAA)")
  void handleDirectoryWithOwnedSite() {
    // Arrange
    var units = List.of(AssetsDomainFixtures.equipment());
    stubOwner();
    stubOwnedSite();
    when(equipmentRepository.findAllByOwnerId(OWNER_ID, SITE_ID, Set.of())).thenReturn(units);

    // Act
    var result = service.handle(new GetEquipmentDirectoryQuery(USERNAME, SITE_ID, null));

    // Assert
    assertThat(ResultTestUtils.successValue(result)).isEqualTo(units);
  }

  @Test
  @DisplayName("handle(GetEquipmentDirectoryQuery) -> SITE_NOT_FOUND when the filter names a foreign site (AAA)")
  void handleDirectoryForeignSite() {
    // Arrange
    stubOwner();
    stubForeignSite();

    // Act
    var result = service.handle(new GetEquipmentDirectoryQuery(USERNAME, SITE_ID, null));

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("SITE_NOT_FOUND");
    verifyNoInteractions(equipmentRepository);
  }

  @Test
  @DisplayName("handle(GetEquipmentDirectoryQuery) -> SITE_NOT_FOUND when the filter names a site that does not exist (AAA)")
  void handleDirectoryMissingSite() {
    // Arrange
    stubOwner();
    when(siteRepository.findById(OTHER_SITE_ID)).thenReturn(Optional.empty());

    // Act
    var result = service.handle(new GetEquipmentDirectoryQuery(USERNAME, OTHER_SITE_ID, null));

    // Assert
    var error = ResultTestUtils.failureError(result);
    assertThat(error.code()).isEqualTo("SITE_NOT_FOUND");
    assertThat(error.message()).contains(String.valueOf(OTHER_SITE_ID));
    verifyNoInteractions(equipmentRepository);
  }

  @Test
  @DisplayName("handle(GetEquipmentDirectoryQuery) -> SITE_NOT_FOUND when a site is named by a caller with no owner profile (AAA)")
  void handleDirectorySiteFilterNoOwnerProfile() {
    // Arrange
    stubNoOwnerProfile();

    // Act
    var result = service.handle(new GetEquipmentDirectoryQuery(USERNAME, SITE_ID, null));

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("SITE_NOT_FOUND");
    verifyNoInteractions(siteRepository, equipmentRepository);
  }

  @Test
  @DisplayName("handle(GetEquipmentDirectoryQuery) -> succeeds with an empty directory when the caller has no owner profile (AAA)")
  void handleDirectoryNoOwnerProfile() {
    // Arrange
    stubNoOwnerProfile();

    // Act
    var result = service.handle(new GetEquipmentDirectoryQuery(USERNAME, null, null));

    // Assert
    assertThat(ResultTestUtils.successValue(result)).isEmpty();
    verifyNoInteractions(equipmentRepository);
  }

  // ====================================================================================
  // handle(GetEquipmentConnectivityQuery)
  // ====================================================================================

  @Test
  @DisplayName("handle(GetEquipmentConnectivityQuery) -> returns an empty map without calling monitoring when there are no units (AAA)")
  void handleConnectivityNoUnits() {
    // Arrange
    var query = new GetEquipmentConnectivityQuery(null);

    // Act
    var result = service.handle(query);

    // Assert
    assertThat(result).isEmpty();
    verifyNoInteractions(assetsExternalMonitoringService);
  }

  @Test
  @DisplayName("handle(GetEquipmentConnectivityQuery) -> ignores units that were never persisted (AAA)")
  void handleConnectivityIgnoresUnitsWithoutId() {
    // Arrange
    var unsaved = AssetsDomainFixtures.equipmentBuilder().id(null).build();

    // Act
    var result = service.handle(new GetEquipmentConnectivityQuery(List.of(unsaved)));

    // Assert
    assertThat(result).isEmpty();
    verifyNoInteractions(assetsExternalMonitoringService);
  }

  @Test
  @DisplayName("handle(GetEquipmentConnectivityQuery) -> evaluates each unit with its own offline window (AAA)")
  void handleConnectivityUsesPerUnitWindow() {
    // Arrange
    var recent = AssetsDomainFixtures.equipmentBuilder().lastReceivedAt(Instant.now()).build();
    var stale = AssetsDomainFixtures.equipmentBuilder().id(OTHER_EQUIPMENT_ID).uid("ESP32-0002")
        .lastReceivedAt(Instant.now().minus(Duration.ofHours(1))).build();
    when(assetsExternalMonitoringService.fetchOfflineSilence(Set.of(EQUIPMENT_ID, OTHER_EQUIPMENT_ID)))
        .thenReturn(Map.of(EQUIPMENT_ID, Duration.ofMinutes(10), OTHER_EQUIPMENT_ID, Duration.ofMinutes(10)));

    // Act
    var result = service.handle(new GetEquipmentConnectivityQuery(List.of(recent, stale)));

    // Assert
    assertThat(result.get(EQUIPMENT_ID).state()).isEqualTo(ConnectivityState.REPORTING);
    assertThat(result.get(EQUIPMENT_ID).silentAfter()).isEqualTo(Duration.ofMinutes(10));
    assertThat(result.get(OTHER_EQUIPMENT_ID).state()).isEqualTo(ConnectivityState.SILENT);
  }

  @Test
  @DisplayName("handle(GetEquipmentConnectivityQuery) -> falls back to the three minute window for a unit monitoring did not answer for (AAA)")
  void handleConnectivityDefaultWindow() {
    // Arrange
    var unit = AssetsDomainFixtures.equipmentBuilder().lastReceivedAt(Instant.now().minus(Duration.ofMinutes(5))).build();
    when(assetsExternalMonitoringService.fetchOfflineSilence(Set.of(EQUIPMENT_ID))).thenReturn(Map.of());

    // Act
    var result = service.handle(new GetEquipmentConnectivityQuery(List.of(unit)));

    // Assert
    assertThat(result.get(EQUIPMENT_ID).silentAfter()).isEqualTo(Duration.ofMinutes(3));
    assertThat(result.get(EQUIPMENT_ID).state()).isEqualTo(ConnectivityState.SILENT);
  }

  @Test
  @DisplayName("handle(GetEquipmentConnectivityQuery) -> reports NEVER_REPORTED for a unit that sent nothing (AAA)")
  void handleConnectivityNeverReported() {
    // Arrange
    var unit = AssetsDomainFixtures.equipment();
    when(assetsExternalMonitoringService.fetchOfflineSilence(Set.of(EQUIPMENT_ID)))
        .thenReturn(Map.of(EQUIPMENT_ID, Duration.ofMinutes(3)));

    // Act
    var result = service.handle(new GetEquipmentConnectivityQuery(List.of(unit)));

    // Assert
    assertThat(result.get(EQUIPMENT_ID).state()).isEqualTo(ConnectivityState.NEVER_REPORTED);
    assertThat(result.get(EQUIPMENT_ID).lastReceivedAt()).isNull();
  }

  @Test
  @DisplayName("handle(GetEquipmentConnectivityQuery) -> returns an unmodifiable map (AAA)")
  void handleConnectivityReturnsUnmodifiableMap() {
    // Arrange
    when(assetsExternalMonitoringService.fetchOfflineSilence(Set.of(EQUIPMENT_ID))).thenReturn(Map.of());

    // Act
    var result = service.handle(new GetEquipmentConnectivityQuery(List.of(AssetsDomainFixtures.equipment())));

    // Assert
    assertThrows(UnsupportedOperationException.class, () -> result.put(1L, null));
  }
}
