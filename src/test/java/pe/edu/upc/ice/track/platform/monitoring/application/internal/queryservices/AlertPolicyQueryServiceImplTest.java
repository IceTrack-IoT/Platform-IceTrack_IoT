package pe.edu.upc.ice.track.platform.monitoring.application.internal.queryservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.accesspolicies.EquipmentAccessPolicy;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl.ExternalAssetManagementService;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl.ExternalProfilesServiceForMonitoring;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl.MonitoredEquipment;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.AlertPolicy;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetAlertPolicyByEquipmentQuery;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetApplicableAlertPoliciesQuery;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.ApplicableAlertPolicy;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertPolicyRepository;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.CallerRole;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.monitoring.test.fixtures.MonitoringDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class AlertPolicyQueryServiceImplTest {

  @Mock
  private AlertPolicyRepository alertPolicyRepository;
  @Mock
  private EquipmentAccessPolicy equipmentAccessPolicy;
  @Mock
  private ExternalProfilesServiceForMonitoring externalProfilesService;
  @Mock
  private ExternalAssetManagementService externalAssetManagementService;

  @InjectMocks
  private AlertPolicyQueryServiceImpl service;

  private static MonitoredEquipment unit(Long equipmentId) {
    return new MonitoredEquipment(equipmentId, "ONLINE", T0);
  }

  private void givenOwnedEquipment(Long... equipmentIds) {
    when(externalProfilesService.fetchOwnerIdByUsername(USERNAME)).thenReturn(Optional.of(OWNER_PROFILE_ID));
    when(externalAssetManagementService.fetchEquipmentOwnedBy(OWNER_PROFILE_ID, null))
        .thenReturn(java.util.Arrays.stream(equipmentIds).map(AlertPolicyQueryServiceImplTest::unit).toList());
  }

  @Test
  @DisplayName("handle(GetAlertPolicyByEquipmentQuery) readable -> the applicable policy (AAA)")
  void getByEquipmentReturnsApplicablePolicy() {
    // Arrange
    var policy = persistedEquipmentPolicy();
    when(equipmentAccessPolicy.canRead(EQUIPMENT_ID, USERNAME, CallerRole.OWNER)).thenReturn(true);
    when(alertPolicyRepository.findApplicableTo(EQUIPMENT_ID)).thenReturn(Optional.of(policy));

    // Act
    var result = service.handle(new GetAlertPolicyByEquipmentQuery(EQUIPMENT_ID, USERNAME, CallerRole.OWNER));

    // Assert
    assertThat(result).containsSame(policy);
  }

  @Test
  @DisplayName("handle(GetAlertPolicyByEquipmentQuery) unreadable -> empty without reading policies (AAA)")
  void getByEquipmentDenied() {
    // Arrange
    when(equipmentAccessPolicy.canRead(EQUIPMENT_ID, USERNAME, CallerRole.OWNER)).thenReturn(false);

    // Act
    var result = service.handle(new GetAlertPolicyByEquipmentQuery(EQUIPMENT_ID, USERNAME, CallerRole.OWNER));

    // Assert
    assertThat(result).isEmpty();
    verifyNoInteractions(alertPolicyRepository);
  }

  @Test
  @DisplayName("handle(GetApplicableAlertPoliciesQuery) without ids -> every owned unit, own policy or default (AAA)")
  void getApplicableForAllOwnedEquipment() {
    // Arrange
    var own = persistedEquipmentPolicy();
    var platformDefault = AlertPolicy.platformDefault();
    givenOwnedEquipment(OTHER_EQUIPMENT_ID, EQUIPMENT_ID);
    when(alertPolicyRepository.findByEquipmentIdIn(List.of(EQUIPMENT_ID, OTHER_EQUIPMENT_ID)))
        .thenReturn(List.of(own));
    when(alertPolicyRepository.findDefaultPolicy()).thenReturn(Optional.of(platformDefault));

    // Act
    var result = service.handle(new GetApplicableAlertPoliciesQuery(USERNAME, List.of()));

    // Assert
    assertThat(result).extracting(ApplicableAlertPolicy::equipmentId)
        .containsExactly(EQUIPMENT_ID, OTHER_EQUIPMENT_ID);
    assertThat(result.get(0).policy()).isSameAs(own);
    assertThat(result.get(1).policy()).isSameAs(platformDefault);
  }

  @Test
  @DisplayName("handle(GetApplicableAlertPoliciesQuery) with ids -> ignores units the caller does not own (AAA)")
  void getApplicableIgnoresForeignEquipment() {
    // Arrange
    givenOwnedEquipment(EQUIPMENT_ID);
    when(alertPolicyRepository.findByEquipmentIdIn(List.of(EQUIPMENT_ID))).thenReturn(List.of());
    when(alertPolicyRepository.findDefaultPolicy()).thenReturn(Optional.empty());

    // Act
    var result = service.handle(new GetApplicableAlertPoliciesQuery(USERNAME, List.of(EQUIPMENT_ID, 999L)));

    // Assert
    assertThat(result).hasSize(1);
    assertThat(result.get(0).equipmentId()).isEqualTo(EQUIPMENT_ID);
    assertThat(result.get(0).policy().isPlatformDefault()).isTrue();
  }

  @Test
  @DisplayName("handle(GetApplicableAlertPoliciesQuery) several policies for one unit -> keeps the first (AAA)")
  void getApplicableKeepsFirstPolicyOfUnit() {
    // Arrange
    var first = persistedEquipmentPolicy();
    var second = persistedEquipmentPolicy();
    givenOwnedEquipment(EQUIPMENT_ID);
    when(alertPolicyRepository.findByEquipmentIdIn(List.of(EQUIPMENT_ID))).thenReturn(List.of(first, second));
    when(alertPolicyRepository.findDefaultPolicy()).thenReturn(Optional.empty());

    // Act
    var result = service.handle(new GetApplicableAlertPoliciesQuery(USERNAME, List.of()));

    // Assert
    assertThat(result).hasSize(1);
    assertThat(result.get(0).policy()).isSameAs(first);
  }

  @Test
  @DisplayName("handle(GetApplicableAlertPoliciesQuery) nothing in scope -> empty list without reading policies (AAA)")
  void getApplicableEmptyScope() {
    // Arrange
    when(externalProfilesService.fetchOwnerIdByUsername(USERNAME)).thenReturn(Optional.empty());
    when(externalAssetManagementService.fetchEquipmentOwnedBy(null, null)).thenReturn(List.of());

    // Act
    var result = service.handle(new GetApplicableAlertPoliciesQuery(USERNAME, List.of(EQUIPMENT_ID)));

    // Assert
    assertThat(result).isEmpty();
    verify(alertPolicyRepository, never()).findByEquipmentIdIn(org.mockito.ArgumentMatchers.any());
    verify(alertPolicyRepository, never()).findDefaultPolicy();
  }
}
