package pe.edu.upc.ice.track.platform.monitoring.application.internal.commandservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.accesspolicies.EquipmentAccessPolicy;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl.ExternalProfilesServiceForMonitoring;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.Alert;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.DismissAlertCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertStatus;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.monitoring.test.fixtures.MonitoringCommandFixtures.*;
import static pe.edu.upc.ice.track.platform.monitoring.test.fixtures.MonitoringDomainFixtures.*;
import static pe.edu.upc.ice.track.platform.shared.test.util.ResultTestUtils.failureError;
import static pe.edu.upc.ice.track.platform.shared.test.util.ResultTestUtils.successValue;

@ExtendWith(MockitoExtension.class)
class AlertCommandServiceImplTest {

  @Mock
  private AlertRepository alertRepository;
  @Mock
  private EquipmentAccessPolicy equipmentAccessPolicy;
  @Mock
  private ExternalProfilesServiceForMonitoring externalProfilesService;

  @InjectMocks
  private AlertCommandServiceImpl service;

  private void givenManageableAlert(Alert alert) {
    when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(alert));
    when(equipmentAccessPolicy.canManage(EQUIPMENT_ID, USERNAME)).thenReturn(true);
    when(externalProfilesService.fetchUserIdByUsername(USERNAME)).thenReturn(Optional.of(USER_ID));
  }

  @Test
  @DisplayName("handle(AcknowledgeAlertCommand) -> acknowledges the alert and saves it (AAA)")
  void acknowledgeSucceeds() {
    // Arrange
    var alert = persistedTemperatureAlert();
    givenManageableAlert(alert);
    when(alertRepository.save(alert)).thenReturn(alert);

    // Act
    var result = service.handle(acknowledgeAlertCommand());

    // Assert
    var saved = successValue(result);
    assertThat(saved.getStatus()).isEqualTo(AlertStatus.ACKNOWLEDGED);
    assertThat(saved.getAcknowledgedBy()).isEqualTo(USER_ID);
    verify(alertRepository, times(1)).save(alert);
  }

  @Test
  @DisplayName("handle(AcknowledgeAlertCommand) -> an alert already acknowledged yields INVALID_STATUS_TRANSITION and is not saved (AAA)")
  void acknowledgeInvalidTransition() {
    // Arrange
    var alert = persistedTemperatureAlert();
    alert.acknowledge(USER_ID);
    givenManageableAlert(alert);

    // Act
    var result = service.handle(acknowledgeAlertCommand());

    // Assert
    var error = failureError(result);
    assertThat(error.code()).isEqualTo("INVALID_STATUS_TRANSITION");
    assertThat(error.details()).isEqualTo("Only an OPEN alert can be acknowledged, was ACKNOWLEDGED");
    verify(alertRepository, never()).save(any(Alert.class));
  }

  @Test
  @DisplayName("handle(ResolveAlertCommand) -> resolves the alert and saves it (AAA)")
  void resolveSucceeds() {
    // Arrange
    var alert = persistedTemperatureAlert();
    givenManageableAlert(alert);
    when(alertRepository.save(alert)).thenReturn(alert);

    // Act
    var result = service.handle(resolveAlertCommand());

    // Assert
    assertThat(successValue(result).getStatus()).isEqualTo(AlertStatus.RESOLVED);
    assertThat(alert.getResolvedBy()).isEqualTo(USER_ID);
  }

  @Test
  @DisplayName("handle(ResolveAlertCommand) -> a closed alert yields INVALID_STATUS_TRANSITION (AAA)")
  void resolveClosedAlert() {
    // Arrange
    var alert = persistedTemperatureAlert();
    alert.resolve(USER_ID);
    givenManageableAlert(alert);

    // Act
    var result = service.handle(resolveAlertCommand());

    // Assert
    assertThat(failureError(result).code()).isEqualTo("INVALID_STATUS_TRANSITION");
    verify(alertRepository, never()).save(any(Alert.class));
  }

  @Test
  @DisplayName("handle(DismissAlertCommand) -> dismisses with the reason and evidence and saves it (AAA)")
  void dismissSucceeds() {
    // Arrange
    var alert = persistedTemperatureAlert();
    givenManageableAlert(alert);
    when(alertRepository.save(alert)).thenReturn(alert);

    // Act
    var result = service.handle(dismissAlertCommand());

    // Assert
    var saved = successValue(result);
    assertThat(saved.getStatus()).isEqualTo(AlertStatus.DISMISSED);
    assertThat(saved.getDismissReason()).isEqualTo("Sensor drift");
    assertThat(saved.getDismissEvidence()).isEqualTo("photo-001");
    assertThat(saved.getDismissedBy()).isEqualTo(USER_ID);
  }

  @Test
  @DisplayName("handle(DismissAlertCommand) -> an over-long reason yields a VALIDATION_ERROR and is not saved (AAA)")
  void dismissValidationError() {
    // Arrange
    var alert = persistedTemperatureAlert();
    givenManageableAlert(alert);
    var command = new DismissAlertCommand(ALERT_ID, USERNAME, "r".repeat(Alert.MAX_DISMISS_REASON_LENGTH + 1), null);

    // Act
    var result = service.handle(command);

    // Assert
    var error = failureError(result);
    assertThat(error.code()).isEqualTo("VALIDATION_ERROR");
    assertThat(error.details()).isEqualTo("The dismissal reason must not exceed 500 characters");
    verify(alertRepository, never()).save(any(Alert.class));
  }


  @Test
  @DisplayName("handle(AcknowledgeAlertCommand) -> an unknown alert yields ALERT_NOT_FOUND (AAA)")
  void unknownAlert() {
    // Arrange
    when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.empty());
    when(externalProfilesService.fetchUserIdByUsername(USERNAME)).thenReturn(Optional.of(USER_ID));

    // Act
    var result = service.handle(acknowledgeAlertCommand());

    // Assert
    assertThat(failureError(result).code()).isEqualTo("ALERT_NOT_FOUND");
    verify(alertRepository, never()).save(any(Alert.class));
  }

  @Test
  @DisplayName("handle(AcknowledgeAlertCommand) -> an alert on equipment the caller cannot manage is reported as not found (AAA)")
  void foreignAlertIsHidden() {
    // Arrange
    var alert = persistedTemperatureAlert();
    when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(alert));
    when(equipmentAccessPolicy.canManage(EQUIPMENT_ID, USERNAME)).thenReturn(false);
    when(externalProfilesService.fetchUserIdByUsername(USERNAME)).thenReturn(Optional.of(USER_ID));

    // Act
    var result = service.handle(acknowledgeAlertCommand());

    // Assert
    var error = failureError(result);
    assertThat(error.code()).isEqualTo("ALERT_NOT_FOUND");
    assertThat(alert.getStatus()).isEqualTo(AlertStatus.OPEN);
    verify(alertRepository, never()).save(any(Alert.class));
  }

  @Test
  @DisplayName("handle(AcknowledgeAlertCommand) -> a caller without a user account yields ALERT_NOT_FOUND (AAA)")
  void unknownActor() {
    // Arrange
    var alert = persistedTemperatureAlert();
    when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(alert));
    when(equipmentAccessPolicy.canManage(EQUIPMENT_ID, USERNAME)).thenReturn(true);
    when(externalProfilesService.fetchUserIdByUsername(USERNAME)).thenReturn(Optional.empty());

    // Act
    var result = service.handle(acknowledgeAlertCommand());

    // Assert
    assertThat(failureError(result).code()).isEqualTo("ALERT_NOT_FOUND");
    assertThat(alert.getStatus()).isEqualTo(AlertStatus.OPEN);
    verify(alertRepository, never()).save(any(Alert.class));
  }

  @Test
  @DisplayName("handle(AcknowledgeAlertCommand) -> a null alert id is not looked up and yields ALERT_NOT_FOUND (AAA)")
  void nullAlertId() {
    // Arrange
    when(externalProfilesService.fetchUserIdByUsername(USERNAME)).thenReturn(Optional.of(USER_ID));
    var command = new pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.AcknowledgeAlertCommand(null, USERNAME);

    // Act
    var result = service.handle(command);

    // Assert
    assertThat(failureError(result).code()).isEqualTo("ALERT_NOT_FOUND");
    verifyNoMoreInteractions(alertRepository);
  }
}
