package pe.edu.upc.ice.track.platform.monitoring.application.internal.commandservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.accesspolicies.EquipmentAccessPolicy;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.AlertPolicy;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.SeedDefaultAlertPolicyCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.UpdateAlertPolicyCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertPolicyRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.monitoring.test.fixtures.MonitoringCommandFixtures.updateAlertPolicyCommand;
import static pe.edu.upc.ice.track.platform.monitoring.test.fixtures.MonitoringDomainFixtures.*;
import static pe.edu.upc.ice.track.platform.shared.test.util.ResultTestUtils.failureError;
import static pe.edu.upc.ice.track.platform.shared.test.util.ResultTestUtils.successValue;

@ExtendWith(MockitoExtension.class)
class AlertPolicyCommandServiceImplTest {

  @Mock
  private AlertPolicyRepository alertPolicyRepository;
  @Mock
  private EquipmentAccessPolicy equipmentAccessPolicy;

  @InjectMocks
  private AlertPolicyCommandServiceImpl service;

  @Test
  @DisplayName("handle(UpdateAlertPolicyCommand) -> a null equipment id is denied: the default policy is managed by configuration (AAA)")
  void platformDefaultCannotBeEdited() {
    // Arrange
    var command = new UpdateAlertPolicyCommand(null, USERNAME, 5, 1.0, 3);

    // Act
    var result = service.handle(command);

    // Assert
    assertThat(failureError(result).code()).isEqualTo("ACCESS_DENIED");
    verifyNoInteractions(alertPolicyRepository, equipmentAccessPolicy);
  }

  @Test
  @DisplayName("handle(UpdateAlertPolicyCommand) -> equipment the caller cannot manage yields EQUIPMENT_NOT_FOUND (AAA)")
  void foreignEquipment() {
    // Arrange
    when(equipmentAccessPolicy.canManage(EQUIPMENT_ID, USERNAME)).thenReturn(false);

    // Act
    var result = service.handle(updateAlertPolicyCommand());

    // Assert
    assertThat(failureError(result).code()).isEqualTo("EQUIPMENT_NOT_FOUND");
    verifyNoInteractions(alertPolicyRepository);
  }

  @Test
  @DisplayName("handle(UpdateAlertPolicyCommand) -> creates a per-equipment policy when none exists (AAA)")
  void createsPolicy() {
    // Arrange
    when(equipmentAccessPolicy.canManage(EQUIPMENT_ID, USERNAME)).thenReturn(true);
    when(alertPolicyRepository.findByEquipmentId(EQUIPMENT_ID)).thenReturn(Optional.empty());
    when(alertPolicyRepository.save(any(AlertPolicy.class))).thenAnswer(inv -> inv.getArgument(0));

    // Act
    var result = service.handle(updateAlertPolicyCommand());

    // Assert
    var policy = successValue(result);
    assertThat(policy.getEquipmentId().equipmentId()).isEqualTo(EQUIPMENT_ID);
    assertThat(policy.getSustainedExcursionMinutes()).isEqualTo(5);
    assertThat(policy.getHysteresisMargin().celsius()).isEqualTo(1.5);
    assertThat(policy.getMissedSyncWindowsForOffline()).isEqualTo(4);
    assertThat(policy.domainEvents()).hasSize(1);
  }

  @Test
  @DisplayName("handle(UpdateAlertPolicyCommand) -> updates the existing policy in place (AAA)")
  void updatesExistingPolicy() {
    // Arrange
    var existing = persistedEquipmentPolicy();
    when(equipmentAccessPolicy.canManage(EQUIPMENT_ID, USERNAME)).thenReturn(true);
    when(alertPolicyRepository.findByEquipmentId(EQUIPMENT_ID)).thenReturn(Optional.of(existing));
    when(alertPolicyRepository.save(existing)).thenReturn(existing);
    var command = new UpdateAlertPolicyCommand(EQUIPMENT_ID, USERNAME, 9, 2.5, 7);

    // Act
    var result = service.handle(command);

    // Assert
    var policy = successValue(result);
    assertThat(policy).isSameAs(existing);
    assertThat(policy.getSustainedExcursionMinutes()).isEqualTo(9);
    assertThat(policy.getHysteresisMargin().celsius()).isEqualTo(2.5);
    assertThat(policy.getMissedSyncWindowsForOffline()).isEqualTo(7);
    verify(alertPolicyRepository, times(1)).save(existing);
  }

  @Test
  @DisplayName("handle(UpdateAlertPolicyCommand) -> an implausible hysteresis yields a VALIDATION_ERROR and nothing is saved (AAA)")
  void invalidHysteresis() {
    // Arrange
    when(equipmentAccessPolicy.canManage(EQUIPMENT_ID, USERNAME)).thenReturn(true);
    when(alertPolicyRepository.findByEquipmentId(EQUIPMENT_ID)).thenReturn(Optional.empty());
    var command = new UpdateAlertPolicyCommand(EQUIPMENT_ID, USERNAME, 5, 500.0, 3);

    // Act
    var result = service.handle(command);

    // Assert
    var error = failureError(result);
    assertThat(error.code()).isEqualTo("VALIDATION_ERROR");
    assertThat(error.message()).isEqualTo("Validation failed: AlertPolicy");
    verify(alertPolicyRepository, never()).save(any(AlertPolicy.class));
  }

  @Test
  @DisplayName("handle(SeedDefaultAlertPolicyCommand) -> saves the platform default when none exists (AAA)")
  void seedsDefault() {
    // Arrange
    when(alertPolicyRepository.findDefaultPolicy()).thenReturn(Optional.empty());

    // Act
    service.handle(new SeedDefaultAlertPolicyCommand());

    // Assert
    var captor = ArgumentCaptor.forClass(AlertPolicy.class);
    verify(alertPolicyRepository, times(1)).save(captor.capture());
    assertThat(captor.getValue().isPlatformDefault()).isTrue();
  }

  @Test
  @DisplayName("handle(SeedDefaultAlertPolicyCommand) -> is idempotent when the default already exists (AAA)")
  void seedIsIdempotent() {
    // Arrange
    when(alertPolicyRepository.findDefaultPolicy()).thenReturn(Optional.of(AlertPolicy.platformDefault()));

    // Act
    service.handle(new SeedDefaultAlertPolicyCommand());

    // Assert
    verify(alertPolicyRepository, never()).save(any(AlertPolicy.class));
  }
}
