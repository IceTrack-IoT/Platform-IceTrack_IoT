package pe.edu.upc.ice.track.platform.assets.application.internal.commandservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.acl.AssetsExternalOwnerService;
import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Equipment;
import pe.edu.upc.ice.track.platform.assets.domain.model.events.EquipmentConnectivityChangedEvent;
import pe.edu.upc.ice.track.platform.assets.domain.model.events.TemperatureThresholdUpdatedEvent;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.ConnectivityState;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.EquipmentType;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.StatusEquipment;
import pe.edu.upc.ice.track.platform.assets.domain.repositories.EquipmentRepository;
import pe.edu.upc.ice.track.platform.assets.domain.repositories.SiteRepository;
import pe.edu.upc.ice.track.platform.assets.test.fixtures.AssetsCommandFixtures;
import pe.edu.upc.ice.track.platform.assets.test.fixtures.AssetsDomainFixtures;
import pe.edu.upc.ice.track.platform.shared.test.util.ResultTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.assets.test.fixtures.AssetsDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class EquipmentCommandServiceImplTest {

  @Mock
  private EquipmentRepository equipmentRepository;
  @Mock
  private SiteRepository siteRepository;
  @Mock
  private AssetsExternalOwnerService assetsExternalOwnerService;

  @InjectMocks
  private EquipmentCommandServiceImpl service;

  // ---------- Helpers ----------

  private void stubOwner() {
    when(assetsExternalOwnerService.fetchOwnerIdByUsername(USERNAME)).thenReturn(Optional.of(OWNER_ID));
  }

  private void stubNoOwnerProfile() {
    when(assetsExternalOwnerService.fetchOwnerIdByUsername(USERNAME)).thenReturn(Optional.empty());
  }

  private void stubOwnedSite() {
    when(siteRepository.findById(SITE_ID)).thenReturn(Optional.of(AssetsDomainFixtures.site()));
  }

  /** Stubs the whole ownership chain so that {@code unit} is reachable by the caller. */
  private void stubReachableEquipment(Equipment unit) {
    stubOwner();
    when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(unit));
    stubOwnedSite();
  }

  // ====================================================================================
  // handle(RegisterEquipmentCommand)
  // ====================================================================================

  @Test
  @DisplayName("handle(RegisterEquipmentCommand) -> registers an ACTIVE unit at an owned site and returns it (AAA)")
  void handleRegisterSuccess() {
    // Arrange
    var command = AssetsCommandFixtures.registerEquipmentCommand();
    stubOwner();
    stubOwnedSite();
    when(equipmentRepository.existsByUid(VALID_UID)).thenReturn(false);
    when(equipmentRepository.save(any(Equipment.class))).thenAnswer(inv -> inv.getArgument(0));

    // Act
    var result = service.handle(command);

    // Assert
    var registered = ResultTestUtils.successValue(result);
    assertThat(registered.getSiteId()).isEqualTo(SITE_ID);
    assertThat(registered.getUid()).isEqualTo(VALID_UID);
    assertThat(registered.getName()).isEqualTo(AssetsDomainFixtures.VALID_EQUIPMENT_NAME);
    assertThat(registered.getEquipmentType()).isEqualTo(EquipmentType.REFRIGERATOR);
    assertThat(registered.getStatus()).isEqualTo(StatusEquipment.ACTIVE);
    assertThat(registered.getTemperatureThreshold().minCelsius()).isEqualTo(AssetsDomainFixtures.VALID_MIN_CELSIUS);
    assertThat(registered.getTemperatureThreshold().maxCelsius()).isEqualTo(AssetsDomainFixtures.VALID_MAX_CELSIUS);
    assertThat(registered.getReminderIntervalDays()).isEqualTo(AssetsDomainFixtures.VALID_REMINDER_INTERVAL_DAYS);
    verify(equipmentRepository, times(1)).existsByUid(VALID_UID);
    verify(equipmentRepository, times(1)).save(any(Equipment.class));
    verifyNoMoreInteractions(equipmentRepository);
  }

  @Test
  @DisplayName("handle(RegisterEquipmentCommand) -> persists a unit built from the command values (AAA)")
  void handleRegisterPersistsCommandValues() {
    // Arrange
    var command = AssetsCommandFixtures.registerEquipmentCommand("  UID-7  ", 3.0, 9.0);
    var captor = ArgumentCaptor.forClass(Equipment.class);
    stubOwner();
    stubOwnedSite();
    when(equipmentRepository.save(any(Equipment.class))).thenAnswer(inv -> inv.getArgument(0));

    // Act
    service.handle(command);

    // Assert
    verify(equipmentRepository).save(captor.capture());
    var saved = captor.getValue();
    assertThat(saved.getEquipmentId()).isNull();
    assertThat(saved.getUid()).isEqualTo("UID-7");
    assertThat(saved.getTemperatureThreshold().minCelsius()).isEqualTo(3.0);
    assertThat(saved.getTemperatureThreshold().maxCelsius()).isEqualTo(9.0);
  }

  @Test
  @DisplayName("handle(RegisterEquipmentCommand) -> ACCESS_DENIED when the caller holds no owner profile (AAA)")
  void handleRegisterNoOwnerProfile() {
    // Arrange
    stubNoOwnerProfile();

    // Act
    var result = service.handle(AssetsCommandFixtures.registerEquipmentCommand());

    // Assert
    var error = ResultTestUtils.failureError(result);
    assertThat(error.code()).isEqualTo("ACCESS_DENIED");
    verifyNoInteractions(siteRepository, equipmentRepository);
  }

  @Test
  @DisplayName("handle(RegisterEquipmentCommand) -> SITE_NOT_FOUND when the site does not exist (AAA)")
  void handleRegisterSiteMissing() {
    // Arrange
    stubOwner();
    when(siteRepository.findById(SITE_ID)).thenReturn(Optional.empty());

    // Act
    var result = service.handle(AssetsCommandFixtures.registerEquipmentCommand());

    // Assert
    var error = ResultTestUtils.failureError(result);
    assertThat(error.code()).isEqualTo("SITE_NOT_FOUND");
    verifyNoInteractions(equipmentRepository);
  }

  @Test
  @DisplayName("handle(RegisterEquipmentCommand) -> SITE_NOT_FOUND when the site belongs to another owner (AAA)")
  void handleRegisterSiteOfAnotherOwner() {
    // Arrange
    stubOwner();
    when(siteRepository.findById(SITE_ID))
        .thenReturn(Optional.of(AssetsDomainFixtures.siteBuilder().ownerId(OTHER_OWNER_ID).build()));

    // Act
    var result = service.handle(AssetsCommandFixtures.registerEquipmentCommand());

    // Assert
    var error = ResultTestUtils.failureError(result);
    assertThat(error.code()).isEqualTo("SITE_NOT_FOUND");
    verify(equipmentRepository, never()).existsByUid(any());
    verify(equipmentRepository, never()).save(any(Equipment.class));
  }

  @Test
  @DisplayName("handle(RegisterEquipmentCommand) -> EQUIPMENT_CONFLICT when the uid is already taken (AAA)")
  void handleRegisterDuplicateUid() {
    // Arrange
    stubOwner();
    stubOwnedSite();
    when(equipmentRepository.existsByUid(VALID_UID)).thenReturn(true);

    // Act
    var result = service.handle(AssetsCommandFixtures.registerEquipmentCommand());

    // Assert
    var error = ResultTestUtils.failureError(result);
    assertThat(error.code()).isEqualTo("EQUIPMENT_CONFLICT");
    assertThat(error.details()).contains(VALID_UID);
    verify(equipmentRepository, never()).save(any(Equipment.class));
  }

  @Test
  @DisplayName("handle(RegisterEquipmentCommand) -> VALIDATION_ERROR when the threshold band is inverted (AAA)")
  void handleRegisterInvertedThreshold() {
    // Arrange
    stubOwner();
    stubOwnedSite();

    // Act
    var result = service.handle(AssetsCommandFixtures.registerEquipmentCommand(VALID_UID, 8.0, 2.0));

    // Assert
    var error = ResultTestUtils.failureError(result);
    assertThat(error.code()).isEqualTo("VALIDATION_ERROR");
    assertThat(error.details()).contains("minimum temperature must be lower than maximum");
    verify(equipmentRepository, never()).save(any(Equipment.class));
  }

  @Test
  @DisplayName("handle(RegisterEquipmentCommand) -> VALIDATION_ERROR when the uid is blank (AAA)")
  void handleRegisterBlankUid() {
    // Arrange
    stubOwner();
    stubOwnedSite();

    // Act
    var result = service.handle(AssetsCommandFixtures.registerEquipmentCommand("   ", 2.0, 8.0));

    // Assert
    var error = ResultTestUtils.failureError(result);
    assertThat(error.code()).isEqualTo("VALIDATION_ERROR");
    assertThat(error.details()).contains("uid");
    verify(equipmentRepository, never()).save(any(Equipment.class));
  }

  @Test
  @DisplayName("handle(RegisterEquipmentCommand) -> rethrows DataIntegrityViolationException so the handler answers 409 (AAA)")
  void handleRegisterRethrowsDataIntegrityViolation() {
    // Arrange
    stubOwner();
    stubOwnedSite();
    when(equipmentRepository.save(any(Equipment.class))).thenThrow(new DataIntegrityViolationException("uk_uid"));

    // Act + Assert
    assertThrows(DataIntegrityViolationException.class,
        () -> service.handle(AssetsCommandFixtures.registerEquipmentCommand()));
  }

  @Test
  @DisplayName("handle(RegisterEquipmentCommand) -> rethrows OptimisticLockingFailureException so the handler answers 409 (AAA)")
  void handleRegisterRethrowsOptimisticLock() {
    // Arrange
    stubOwner();
    stubOwnedSite();
    when(equipmentRepository.save(any(Equipment.class))).thenThrow(new OptimisticLockingFailureException("stale site"));

    // Act + Assert
    assertThrows(OptimisticLockingFailureException.class,
        () -> service.handle(AssetsCommandFixtures.registerEquipmentCommand()));
  }

  @Test
  @DisplayName("handle(RegisterEquipmentCommand) -> wraps any other save failure in UNEXPECTED_ERROR (AAA)")
  void handleRegisterUnexpectedFailure() {
    // Arrange
    stubOwner();
    stubOwnedSite();
    when(equipmentRepository.save(any(Equipment.class))).thenThrow(new RuntimeException("db down"));

    // Act
    var result = service.handle(AssetsCommandFixtures.registerEquipmentCommand());

    // Assert
    var error = ResultTestUtils.failureError(result);
    assertThat(error.code()).isEqualTo("UNEXPECTED_ERROR");
    assertThat(error.message()).contains("Equipment registration");
    assertThat(error.details()).isEqualTo("db down");
  }

  // ====================================================================================
  // handle(UpdateEquipmentCommand)
  // ====================================================================================

  @Test
  @DisplayName("handle(UpdateEquipmentCommand) -> replaces name, type and reminder interval of a reachable unit (AAA)")
  void handleUpdateSuccess() {
    // Arrange
    var unit = AssetsDomainFixtures.equipment();
    stubReachableEquipment(unit);
    when(equipmentRepository.save(unit)).thenReturn(unit);

    // Act
    var result = service.handle(AssetsCommandFixtures.updateEquipmentCommand(null));

    // Assert
    var updated = ResultTestUtils.successValue(result);
    assertThat(updated.getName()).isEqualTo(AssetsCommandFixtures.UPDATED_EQUIPMENT_NAME);
    assertThat(updated.getEquipmentType()).isEqualTo(EquipmentType.DISPLAY_CASE);
    assertThat(updated.getReminderIntervalDays()).isEqualTo(AssetsCommandFixtures.UPDATED_REMINDER_INTERVAL_DAYS);
    verify(equipmentRepository, times(1)).save(unit);
  }

  @Test
  @DisplayName("handle(UpdateEquipmentCommand) -> succeeds when If-Match names the current version (AAA)")
  void handleUpdateMatchingVersion() {
    // Arrange
    var unit = AssetsDomainFixtures.equipmentBuilder().version(3L).build();
    stubReachableEquipment(unit);
    when(equipmentRepository.save(unit)).thenReturn(unit);

    // Act
    var result = service.handle(AssetsCommandFixtures.updateEquipmentCommand(3L));

    // Assert
    assertThat(result.isSuccess()).isTrue();
    verify(equipmentRepository, times(1)).save(unit);
  }

  @Test
  @DisplayName("handle(UpdateEquipmentCommand) -> ACCESS_DENIED when the caller holds no owner profile (AAA)")
  void handleUpdateNoOwnerProfile() {
    // Arrange
    stubNoOwnerProfile();

    // Act
    var result = service.handle(AssetsCommandFixtures.updateEquipmentCommand(null));

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("ACCESS_DENIED");
    verifyNoInteractions(equipmentRepository, siteRepository);
  }

  @Test
  @DisplayName("handle(UpdateEquipmentCommand) -> EQUIPMENT_NOT_FOUND when the unit does not exist (AAA)")
  void handleUpdateMissingUnit() {
    // Arrange
    stubOwner();
    when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.empty());

    // Act
    var result = service.handle(AssetsCommandFixtures.updateEquipmentCommand(null));

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("EQUIPMENT_NOT_FOUND");
    verify(equipmentRepository, never()).save(any(Equipment.class));
  }

  @Test
  @DisplayName("handle(UpdateEquipmentCommand) -> EQUIPMENT_NOT_FOUND when the unit sits at another owner's site (AAA)")
  void handleUpdateUnitOfAnotherOwner() {
    // Arrange
    stubOwner();
    when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(AssetsDomainFixtures.equipment()));
    when(siteRepository.findById(SITE_ID))
        .thenReturn(Optional.of(AssetsDomainFixtures.siteBuilder().ownerId(OTHER_OWNER_ID).build()));

    // Act
    var result = service.handle(AssetsCommandFixtures.updateEquipmentCommand(null));

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("EQUIPMENT_NOT_FOUND");
    verify(equipmentRepository, never()).save(any(Equipment.class));
  }

  @Test
  @DisplayName("handle(UpdateEquipmentCommand) -> CONCURRENT_MODIFICATION_CONFLICT when If-Match is stale (AAA)")
  void handleUpdateStaleVersion() {
    // Arrange
    var unit = AssetsDomainFixtures.equipmentBuilder().version(3L).build();
    stubReachableEquipment(unit);

    // Act
    var result = service.handle(AssetsCommandFixtures.updateEquipmentCommand(2L));

    // Assert
    var error = ResultTestUtils.failureError(result);
    assertThat(error.code()).isEqualTo("CONCURRENT_MODIFICATION_CONFLICT");
    assertThat(error.details()).contains("version 2").contains("version is 3");
    verify(equipmentRepository, never()).save(any(Equipment.class));
  }

  @Test
  @DisplayName("handle(UpdateEquipmentCommand) -> VALIDATION_ERROR when the new name is blank (AAA)")
  void handleUpdateBlankName() {
    // Arrange
    var unit = AssetsDomainFixtures.equipment();
    stubReachableEquipment(unit);

    // Act
    var result = service.handle(AssetsCommandFixtures.updateEquipmentCommand("  ", null));

    // Assert
    var error = ResultTestUtils.failureError(result);
    assertThat(error.code()).isEqualTo("VALIDATION_ERROR");
    assertThat(error.details()).contains("name");
    assertThat(unit.getName()).isEqualTo(AssetsDomainFixtures.VALID_EQUIPMENT_NAME);
    verify(equipmentRepository, never()).save(any(Equipment.class));
  }

  @Test
  @DisplayName("handle(UpdateEquipmentCommand) -> rethrows OptimisticLockingFailureException (AAA)")
  void handleUpdateRethrowsOptimisticLock() {
    // Arrange
    var unit = AssetsDomainFixtures.equipment();
    stubReachableEquipment(unit);
    when(equipmentRepository.save(unit)).thenThrow(new OptimisticLockingFailureException("stale"));

    // Act + Assert
    assertThrows(OptimisticLockingFailureException.class,
        () -> service.handle(AssetsCommandFixtures.updateEquipmentCommand(null)));
  }

  @Test
  @DisplayName("handle(UpdateEquipmentCommand) -> wraps any other save failure in UNEXPECTED_ERROR (AAA)")
  void handleUpdateUnexpectedFailure() {
    // Arrange
    var unit = AssetsDomainFixtures.equipment();
    stubReachableEquipment(unit);
    when(equipmentRepository.save(unit)).thenThrow(new RuntimeException("db down"));

    // Act
    var result = service.handle(AssetsCommandFixtures.updateEquipmentCommand(null));

    // Assert
    var error = ResultTestUtils.failureError(result);
    assertThat(error.code()).isEqualTo("UNEXPECTED_ERROR");
    assertThat(error.message()).contains("Equipment update");
  }

  // ====================================================================================
  // handle(ChangeThresholdCommand)
  // ====================================================================================

  @Test
  @DisplayName("handle(ChangeThresholdCommand) -> applies the band, bumps the edge config version and returns the refreshed unit (AAA)")
  void handleChangeThresholdSuccess() {
    // Arrange
    var unit = AssetsDomainFixtures.equipment();
    var refreshed = AssetsDomainFixtures.equipmentBuilder().configVersion(2).build();
    stubOwner();
    stubOwnedSite();
    when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(unit), Optional.of(refreshed));
    when(equipmentRepository.save(unit)).thenReturn(unit);
    when(equipmentRepository.incrementConfigVersion(EQUIPMENT_ID)).thenReturn(Optional.of(2));

    // Act
    var result = service.handle(AssetsCommandFixtures.changeThresholdCommand(3.0, 7.0, null));

    // Assert
    assertThat(ResultTestUtils.successValue(result)).isSameAs(refreshed);
    assertThat(unit.getTemperatureThreshold().minCelsius()).isEqualTo(3.0);
    assertThat(unit.getTemperatureThreshold().maxCelsius()).isEqualTo(7.0);
    assertThat(unit.domainEvents()).hasSize(1).hasOnlyElementsOfType(TemperatureThresholdUpdatedEvent.class);
    verify(equipmentRepository, times(1)).save(unit);
    verify(equipmentRepository, times(1)).incrementConfigVersion(EQUIPMENT_ID);
    verify(equipmentRepository, times(2)).findById(EQUIPMENT_ID);
  }

  @Test
  @DisplayName("handle(ChangeThresholdCommand) -> falls back to the saved unit when the refresh finds nothing (AAA)")
  void handleChangeThresholdRefreshMissing() {
    // Arrange
    var unit = AssetsDomainFixtures.equipment();
    stubOwner();
    stubOwnedSite();
    when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(unit), Optional.empty());
    when(equipmentRepository.save(unit)).thenReturn(unit);
    when(equipmentRepository.incrementConfigVersion(EQUIPMENT_ID)).thenReturn(Optional.empty());

    // Act
    var result = service.handle(AssetsCommandFixtures.changeThresholdCommand(3.0, 7.0, null));

    // Assert
    assertThat(ResultTestUtils.successValue(result)).isSameAs(unit);
  }

  @Test
  @DisplayName("handle(ChangeThresholdCommand) -> VALIDATION_ERROR and no event when the band is inverted (AAA)")
  void handleChangeThresholdInverted() {
    // Arrange
    var unit = AssetsDomainFixtures.equipment();
    stubReachableEquipment(unit);

    // Act
    var result = service.handle(AssetsCommandFixtures.changeThresholdCommand(8.0, 2.0, null));

    // Assert
    var error = ResultTestUtils.failureError(result);
    assertThat(error.code()).isEqualTo("VALIDATION_ERROR");
    assertThat(unit.domainEvents()).isEmpty();
    assertThat(unit.getTemperatureThreshold().minCelsius()).isEqualTo(AssetsDomainFixtures.VALID_MIN_CELSIUS);
    verify(equipmentRepository, never()).save(any(Equipment.class));
    verify(equipmentRepository, never()).incrementConfigVersion(any());
  }

  @Test
  @DisplayName("handle(ChangeThresholdCommand) -> VALIDATION_ERROR when a bound leaves the 0 to 30 C range (AAA)")
  void handleChangeThresholdOutOfBounds() {
    // Arrange
    stubReachableEquipment(AssetsDomainFixtures.equipment());

    // Act
    var result = service.handle(AssetsCommandFixtures.changeThresholdCommand(2.0, 35.0, null));

    // Assert
    var error = ResultTestUtils.failureError(result);
    assertThat(error.code()).isEqualTo("VALIDATION_ERROR");
    assertThat(error.details()).contains("maxCelsius");
    verify(equipmentRepository, never()).incrementConfigVersion(any());
  }

  @Test
  @DisplayName("handle(ChangeThresholdCommand) -> ACCESS_DENIED when the caller holds no owner profile (AAA)")
  void handleChangeThresholdNoOwnerProfile() {
    // Arrange
    stubNoOwnerProfile();

    // Act
    var result = service.handle(AssetsCommandFixtures.changeThresholdCommand(3.0, 7.0, null));

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("ACCESS_DENIED");
    verifyNoInteractions(equipmentRepository);
  }

  @Test
  @DisplayName("handle(ChangeThresholdCommand) -> CONCURRENT_MODIFICATION_CONFLICT when If-Match is stale (AAA)")
  void handleChangeThresholdStaleVersion() {
    // Arrange
    stubReachableEquipment(AssetsDomainFixtures.equipmentBuilder().version(5L).build());

    // Act
    var result = service.handle(AssetsCommandFixtures.changeThresholdCommand(3.0, 7.0, 4L));

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("CONCURRENT_MODIFICATION_CONFLICT");
    verify(equipmentRepository, never()).save(any(Equipment.class));
    verify(equipmentRepository, never()).incrementConfigVersion(any());
  }

  @Test
  @DisplayName("handle(ChangeThresholdCommand) -> rethrows OptimisticLockingFailureException (AAA)")
  void handleChangeThresholdRethrowsOptimisticLock() {
    // Arrange
    var unit = AssetsDomainFixtures.equipment();
    stubReachableEquipment(unit);
    when(equipmentRepository.save(unit)).thenThrow(new OptimisticLockingFailureException("stale"));

    // Act + Assert
    assertThrows(OptimisticLockingFailureException.class,
        () -> service.handle(AssetsCommandFixtures.changeThresholdCommand(3.0, 7.0, null)));
  }

  @Test
  @DisplayName("handle(ChangeThresholdCommand) -> wraps any other failure in UNEXPECTED_ERROR (AAA)")
  void handleChangeThresholdUnexpectedFailure() {
    // Arrange
    var unit = AssetsDomainFixtures.equipment();
    stubReachableEquipment(unit);
    when(equipmentRepository.save(unit)).thenThrow(new RuntimeException("db down"));

    // Act
    var result = service.handle(AssetsCommandFixtures.changeThresholdCommand(3.0, 7.0, null));

    // Assert
    var error = ResultTestUtils.failureError(result);
    assertThat(error.code()).isEqualTo("UNEXPECTED_ERROR");
    assertThat(error.message()).contains("Equipment threshold update");
  }

  // ====================================================================================
  // handle(BumpConfigVersionCommand)
  // ====================================================================================

  @Test
  @DisplayName("handle(BumpConfigVersionCommand) -> returns the new version when the unit exists (AAA)")
  void handleBumpPresent() {
    // Arrange
    when(equipmentRepository.incrementConfigVersion(EQUIPMENT_ID)).thenReturn(Optional.of(4));

    // Act
    Optional<Integer> result = service.handle(AssetsCommandFixtures.bumpConfigVersionCommand());

    // Assert
    assertThat(result).contains(4);
    verify(equipmentRepository, times(1)).incrementConfigVersion(EQUIPMENT_ID);
    verifyNoMoreInteractions(equipmentRepository);
  }

  @Test
  @DisplayName("handle(BumpConfigVersionCommand) -> returns empty when the unit does not exist (AAA)")
  void handleBumpAbsent() {
    // Arrange
    when(equipmentRepository.incrementConfigVersion(EQUIPMENT_ID)).thenReturn(Optional.empty());

    // Act
    Optional<Integer> result = service.handle(AssetsCommandFixtures.bumpConfigVersionCommand());

    // Assert
    assertThat(result).isEmpty();
    verify(equipmentRepository, times(1)).incrementConfigVersion(EQUIPMENT_ID);
    verifyNoMoreInteractions(equipmentRepository);
  }

  // ====================================================================================
  // handle(EnterMaintenanceCommand)
  // ====================================================================================

  @Test
  @DisplayName("handle(EnterMaintenanceCommand) -> false when the unit does not exist (AAA)")
  void handleEnterMaintenanceMissingUnit() {
    // Arrange
    when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.empty());

    // Act
    boolean entered = service.handle(AssetsCommandFixtures.enterMaintenanceCommand());

    // Assert
    assertThat(entered).isFalse();
    verify(equipmentRepository, never()).save(any(Equipment.class));
  }

  @Test
  @DisplayName("handle(EnterMaintenanceCommand) -> moves an ACTIVE unit to MAINTENANCE remembering its status (AAA)")
  void handleEnterMaintenanceActiveUnit() {
    // Arrange
    var unit = AssetsDomainFixtures.equipment();
    when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(unit));

    // Act
    boolean entered = service.handle(AssetsCommandFixtures.enterMaintenanceCommand());

    // Assert
    assertThat(entered).isTrue();
    assertThat(unit.getStatus()).isEqualTo(StatusEquipment.MAINTENANCE);
    assertThat(unit.getStatusBeforeMaintenance()).isEqualTo(StatusEquipment.ACTIVE);
    verify(equipmentRepository, times(1)).save(unit);
  }

  @Test
  @DisplayName("handle(EnterMaintenanceCommand) -> keeps the status but stores the window cover of a REPAIR unit (AAA)")
  void handleEnterMaintenanceKeepsStatusButStoresCover() {
    // Arrange
    var unit = AssetsDomainFixtures.equipmentBuilder().status(StatusEquipment.REPAIR).build();
    when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(unit));

    // Act
    boolean entered = service.handle(AssetsCommandFixtures.enterMaintenanceCommand());

    // Assert
    assertThat(entered).isFalse();
    assertThat(unit.getStatus()).isEqualTo(StatusEquipment.REPAIR);
    assertThat(unit.isUnderMaintenance()).isTrue();
    verify(equipmentRepository, times(1)).save(unit);
  }

  @Test
  @DisplayName("handle(EnterMaintenanceCommand) -> stores nothing when the window already covered the unit (AAA)")
  void handleEnterMaintenanceNothingToStore() {
    // Arrange
    var unit = AssetsDomainFixtures.equipmentBuilder()
        .status(StatusEquipment.REPAIR)
        .underMaintenance(true)
        .build();
    when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(unit));

    // Act
    boolean entered = service.handle(AssetsCommandFixtures.enterMaintenanceCommand());

    // Assert
    assertThat(entered).isFalse();
    verify(equipmentRepository, never()).save(any(Equipment.class));
  }

  // ====================================================================================
  // handle(LeaveMaintenanceCommand)
  // ====================================================================================

  @Test
  @DisplayName("handle(LeaveMaintenanceCommand) -> false when the unit does not exist (AAA)")
  void handleLeaveMaintenanceMissingUnit() {
    // Arrange
    when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.empty());

    // Act
    boolean left = service.handle(AssetsCommandFixtures.leaveMaintenanceCommand());

    // Assert
    assertThat(left).isFalse();
    verify(equipmentRepository, never()).save(any(Equipment.class));
  }

  @Test
  @DisplayName("handle(LeaveMaintenanceCommand) -> returns the unit to the status it held before the window (AAA)")
  void handleLeaveMaintenanceRestoresStatus() {
    // Arrange
    var unit = AssetsDomainFixtures.equipmentBuilder()
        .status(StatusEquipment.MAINTENANCE)
        .statusBeforeMaintenance(StatusEquipment.ACTIVE)
        .underMaintenance(true)
        .build();
    when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(unit));

    // Act
    boolean left = service.handle(AssetsCommandFixtures.leaveMaintenanceCommand());

    // Assert
    assertThat(left).isTrue();
    assertThat(unit.getStatus()).isEqualTo(StatusEquipment.ACTIVE);
    assertThat(unit.getStatusBeforeMaintenance()).isNull();
    verify(equipmentRepository, times(1)).save(unit);
  }

  @Test
  @DisplayName("handle(LeaveMaintenanceCommand) -> keeps the status but stores that the window no longer covers the unit (AAA)")
  void handleLeaveMaintenanceKeepsStatusButStoresCover() {
    // Arrange
    var unit = AssetsDomainFixtures.equipmentBuilder()
        .status(StatusEquipment.REPAIR)
        .underMaintenance(true)
        .build();
    when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(unit));

    // Act
    boolean left = service.handle(AssetsCommandFixtures.leaveMaintenanceCommand());

    // Assert
    assertThat(left).isFalse();
    assertThat(unit.getStatus()).isEqualTo(StatusEquipment.REPAIR);
    assertThat(unit.isUnderMaintenance()).isFalse();
    verify(equipmentRepository, times(1)).save(unit);
  }

  @Test
  @DisplayName("handle(LeaveMaintenanceCommand) -> stores nothing when no window held the unit (AAA)")
  void handleLeaveMaintenanceNothingToStore() {
    // Arrange
    var unit = AssetsDomainFixtures.equipment();
    when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(unit));

    // Act
    boolean left = service.handle(AssetsCommandFixtures.leaveMaintenanceCommand());

    // Assert
    assertThat(left).isFalse();
    verify(equipmentRepository, never()).save(any(Equipment.class));
  }

  // ====================================================================================
  // handle(RecordEquipmentReadingCommand)
  // ====================================================================================

  @Test
  @DisplayName("handle(RecordEquipmentReadingCommand) -> false when the unit does not exist (AAA)")
  void handleRecordReadingMissingUnit() {
    // Arrange
    var recordedAt = Instant.parse("2026-11-09T14:00:00Z");
    when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.empty());

    // Act
    boolean recorded = service.handle(
        AssetsCommandFixtures.recordEquipmentReadingCommand(4.5, recordedAt, recordedAt));

    // Assert
    assertThat(recorded).isFalse();
    verify(equipmentRepository, never()).save(any(Equipment.class));
  }

  @Test
  @DisplayName("handle(RecordEquipmentReadingCommand) -> brings the unit online with the batch temperature (AAA)")
  void handleRecordReadingUpdatesUnit() {
    // Arrange
    var recordedAt = Instant.parse("2026-11-09T14:00:00Z");
    var receivedAt = Instant.parse("2026-11-09T14:00:05Z");
    var unit = AssetsDomainFixtures.equipment();
    when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(unit));

    // Act
    boolean recorded = service.handle(
        AssetsCommandFixtures.recordEquipmentReadingCommand(4.5, recordedAt, receivedAt));

    // Assert
    assertThat(recorded).isTrue();
    assertThat(unit.isOnline()).isTrue();
    assertThat(unit.getLastKnownTemperature()).isEqualTo(4.5);
    assertThat(unit.getLastReadingAt()).isEqualTo(recordedAt);
    assertThat(unit.getLastReceivedAt()).isEqualTo(receivedAt);
    assertThat(unit.domainEvents()).hasSize(1).hasOnlyElementsOfType(EquipmentConnectivityChangedEvent.class);
    verify(equipmentRepository, times(1)).save(unit);
  }

  @Test
  @DisplayName("handle(RecordEquipmentReadingCommand) -> keeps the latest reading when the batch is older but still stores arrival (AAA)")
  void handleRecordReadingOlderBatch() {
    // Arrange
    var latest = Instant.parse("2026-11-09T14:00:00Z");
    var unit = AssetsDomainFixtures.equipmentBuilder()
        .online(true)
        .lastReading(latest, 6.0)
        .lastReceivedAt(latest)
        .build();
    when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(unit));

    // Act
    boolean recorded = service.handle(AssetsCommandFixtures.recordEquipmentReadingCommand(
        1.0, latest.minusSeconds(60), latest.plusSeconds(30)));

    // Assert
    assertThat(recorded).isTrue();
    assertThat(unit.getLastKnownTemperature()).isEqualTo(6.0);
    assertThat(unit.getLastReadingAt()).isEqualTo(latest);
    assertThat(unit.getLastReceivedAt()).isEqualTo(latest.plusSeconds(30));
    verify(equipmentRepository, times(1)).save(unit);
  }

  // ====================================================================================
  // handle(MarkEquipmentOfflineCommand)
  // ====================================================================================

  @Test
  @DisplayName("handle(MarkEquipmentOfflineCommand) -> false when the unit does not exist (AAA)")
  void handleMarkOfflineMissingUnit() {
    // Arrange
    when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.empty());

    // Act
    boolean markedOffline = service.handle(AssetsCommandFixtures.markEquipmentOfflineCommand());

    // Assert
    assertThat(markedOffline).isFalse();
    verify(equipmentRepository, never()).save(any(Equipment.class));
  }

  @Test
  @DisplayName("handle(MarkEquipmentOfflineCommand) -> false and nothing stored when the unit is already offline (AAA)")
  void handleMarkOfflineAlreadyOffline() {
    // Arrange
    when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(AssetsDomainFixtures.equipment()));

    // Act
    boolean markedOffline = service.handle(AssetsCommandFixtures.markEquipmentOfflineCommand());

    // Assert
    assertThat(markedOffline).isFalse();
    verify(equipmentRepository, never()).save(any(Equipment.class));
  }

  @Test
  @DisplayName("handle(MarkEquipmentOfflineCommand) -> marks an online unit offline and stores it (AAA)")
  void handleMarkOfflineOnlineUnit() {
    // Arrange
    var arrival = Instant.parse("2026-11-09T14:00:00Z");
    var unit = AssetsDomainFixtures.equipmentBuilder()
        .online(true)
        .lastReading(arrival, 4.0)
        .lastReceivedAt(arrival)
        .build();
    when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(unit));

    // Act
    boolean markedOffline = service.handle(AssetsCommandFixtures.markEquipmentOfflineCommand());

    // Assert
    assertThat(markedOffline).isTrue();
    assertThat(unit.isOnline()).isFalse();
    assertThat(unit.storedConnectivityState()).isEqualTo(ConnectivityState.SILENT);
    verify(equipmentRepository, times(1)).save(unit);
  }

  // ====================================================================================
  // handle(ChangeStatusCommand)
  // ====================================================================================

  @Test
  @DisplayName("handle(ChangeStatusCommand) -> moves the unit along an allowed transition (AAA)")
  void handleChangeStatusSuccess() {
    // Arrange
    var unit = AssetsDomainFixtures.equipment();
    stubReachableEquipment(unit);
    when(equipmentRepository.save(unit)).thenReturn(unit);

    // Act
    var result = service.handle(AssetsCommandFixtures.changeStatusCommand(StatusEquipment.REPAIR, null));

    // Assert
    assertThat(ResultTestUtils.successValue(result).getStatus()).isEqualTo(StatusEquipment.REPAIR);
    verify(equipmentRepository, times(1)).save(unit);
  }

  @Test
  @DisplayName("handle(ChangeStatusCommand) -> is a no-op success when the unit already holds the target status (AAA)")
  void handleChangeStatusSameStatus() {
    // Arrange
    var unit = AssetsDomainFixtures.equipment();
    stubReachableEquipment(unit);
    when(equipmentRepository.save(unit)).thenReturn(unit);

    // Act
    var result = service.handle(AssetsCommandFixtures.changeStatusCommand(StatusEquipment.ACTIVE, null));

    // Assert
    assertThat(ResultTestUtils.successValue(result).getStatus()).isEqualTo(StatusEquipment.ACTIVE);
    assertThat(unit.domainEvents()).isEmpty();
  }

  @Test
  @DisplayName("handle(ChangeStatusCommand) -> EQUIPMENT_CONFLICT when the transition is not in the matrix (AAA)")
  void handleChangeStatusIllegalTransition() {
    // Arrange
    var unit = AssetsDomainFixtures.equipmentBuilder().status(StatusEquipment.DEACTIVATED).build();
    stubReachableEquipment(unit);

    // Act
    var result = service.handle(AssetsCommandFixtures.changeStatusCommand(StatusEquipment.REPAIR, null));

    // Assert
    var error = ResultTestUtils.failureError(result);
    assertThat(error.code()).isEqualTo("EQUIPMENT_CONFLICT");
    assertThat(error.details()).contains("Illegal equipment status transition").contains("DEACTIVATED");
    assertThat(unit.getStatus()).isEqualTo(StatusEquipment.DEACTIVATED);
    verify(equipmentRepository, never()).save(any(Equipment.class));
  }

  @Test
  @DisplayName("handle(ChangeStatusCommand) -> ACCESS_DENIED when the caller holds no owner profile (AAA)")
  void handleChangeStatusNoOwnerProfile() {
    // Arrange
    stubNoOwnerProfile();

    // Act
    var result = service.handle(AssetsCommandFixtures.changeStatusCommand(StatusEquipment.REPAIR, null));

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("ACCESS_DENIED");
    verifyNoInteractions(equipmentRepository);
  }

  @Test
  @DisplayName("handle(ChangeStatusCommand) -> EQUIPMENT_NOT_FOUND when the unit is out of the caller's reach (AAA)")
  void handleChangeStatusNotReachable() {
    // Arrange
    stubOwner();
    when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.empty());

    // Act
    var result = service.handle(AssetsCommandFixtures.changeStatusCommand(StatusEquipment.REPAIR, null));

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("EQUIPMENT_NOT_FOUND");
  }

  @Test
  @DisplayName("handle(ChangeStatusCommand) -> CONCURRENT_MODIFICATION_CONFLICT when If-Match is stale (AAA)")
  void handleChangeStatusStaleVersion() {
    // Arrange
    stubReachableEquipment(AssetsDomainFixtures.equipmentBuilder().version(9L).build());

    // Act
    var result = service.handle(AssetsCommandFixtures.changeStatusCommand(StatusEquipment.REPAIR, 8L));

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("CONCURRENT_MODIFICATION_CONFLICT");
    verify(equipmentRepository, never()).save(any(Equipment.class));
  }

  @Test
  @DisplayName("handle(ChangeStatusCommand) -> rethrows OptimisticLockingFailureException (AAA)")
  void handleChangeStatusRethrowsOptimisticLock() {
    // Arrange
    var unit = AssetsDomainFixtures.equipment();
    stubReachableEquipment(unit);
    when(equipmentRepository.save(unit)).thenThrow(new OptimisticLockingFailureException("stale"));

    // Act + Assert
    assertThrows(OptimisticLockingFailureException.class,
        () -> service.handle(AssetsCommandFixtures.changeStatusCommand(StatusEquipment.REPAIR, null)));
  }

  @Test
  @DisplayName("handle(ChangeStatusCommand) -> wraps a null target status in UNEXPECTED_ERROR (AAA)")
  void handleChangeStatusNullTarget() {
    // Arrange
    stubReachableEquipment(AssetsDomainFixtures.equipment());

    // Act
    var result = service.handle(AssetsCommandFixtures.changeStatusCommand(null, null));

    // Assert
    var error = ResultTestUtils.failureError(result);
    assertThat(error.code()).isEqualTo("UNEXPECTED_ERROR");
    assertThat(error.message()).contains("Equipment status update");
    verify(equipmentRepository, never()).save(any(Equipment.class));
  }
}
