package pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.acl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.iam.interfaces.acl.IamContextFacade;
import pe.edu.upc.ice.track.platform.profiles.interfaces.acl.ProfilesContextFacade;
import pe.edu.upc.ice.track.platform.service.request.interfaces.acl.ServiceRequestContextFacade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.assets.test.fixtures.AssetsDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class AssetsExternalServiceRequestServiceTest {

  @Mock
  private IamContextFacade iamContextFacade;
  @Mock
  private ProfilesContextFacade profilesContextFacade;
  @Mock
  private ServiceRequestContextFacade serviceRequestContextFacade;

  @InjectMocks
  private AssetsExternalServiceRequestService service;

  private void stubTechnician() {
    when(iamContextFacade.fetchUserIdByUsername(TECHNICIAN_USERNAME)).thenReturn(USER_ID);
    when(profilesContextFacade.fetchTechnicianIdByUserId(USER_ID)).thenReturn(TECHNICIAN_ID);
  }

  @Test
  @DisplayName("isTechnicianAssignedToEquipment(String, Long) -> true when the technician has an active assignment on the unit (AAA)")
  void assignedToEquipmentTrue() {
    // Arrange
    stubTechnician();
    when(serviceRequestContextFacade.hasActiveAssignmentOnEquipment(TECHNICIAN_ID, EQUIPMENT_ID)).thenReturn(true);

    // Act
    var result = service.isTechnicianAssignedToEquipment(TECHNICIAN_USERNAME, EQUIPMENT_ID);

    // Assert
    assertThat(result).isTrue();
    verify(serviceRequestContextFacade, times(1)).hasActiveAssignmentOnEquipment(TECHNICIAN_ID, EQUIPMENT_ID);
  }

  @Test
  @DisplayName("isTechnicianAssignedToEquipment(String, Long) -> false when the technician has no active assignment on the unit (AAA)")
  void assignedToEquipmentFalse() {
    // Arrange
    stubTechnician();
    when(serviceRequestContextFacade.hasActiveAssignmentOnEquipment(TECHNICIAN_ID, EQUIPMENT_ID)).thenReturn(false);

    // Act
    var result = service.isTechnicianAssignedToEquipment(TECHNICIAN_USERNAME, EQUIPMENT_ID);

    // Assert
    assertThat(result).isFalse();
  }

  @ParameterizedTest(name = "[{index}] username={0}")
  @MethodSource("pe.edu.upc.ice.track.platform.shared.test.fixtures.CommonCommandFixtures#invalidBlankStrings")
  @DisplayName("isTechnicianAssignedToEquipment(String, Long) -> false without calling any context for a blank username (AAA)")
  void assignedToEquipmentBlankUsername(String username) {
    // Act
    var result = service.isTechnicianAssignedToEquipment(username, EQUIPMENT_ID);

    // Assert
    assertThat(result).isFalse();
    verifyNoInteractions(iamContextFacade, profilesContextFacade, serviceRequestContextFacade);
  }

  @Test
  @DisplayName("isTechnicianAssignedToEquipment(String, Long) -> false when IAM does not know the username (AAA)")
  void assignedToEquipmentUnknownUser() {
    // Arrange
    when(iamContextFacade.fetchUserIdByUsername(TECHNICIAN_USERNAME)).thenReturn(0L);

    // Act
    var result = service.isTechnicianAssignedToEquipment(TECHNICIAN_USERNAME, EQUIPMENT_ID);

    // Assert
    assertThat(result).isFalse();
    verifyNoInteractions(profilesContextFacade, serviceRequestContextFacade);
  }

  @Test
  @DisplayName("isTechnicianAssignedToEquipment(String, Long) -> false when the user holds no technician profile (AAA)")
  void assignedToEquipmentNoTechnicianProfile() {
    // Arrange
    when(iamContextFacade.fetchUserIdByUsername(TECHNICIAN_USERNAME)).thenReturn(USER_ID);
    when(profilesContextFacade.fetchTechnicianIdByUserId(USER_ID)).thenReturn(0L);

    // Act
    var result = service.isTechnicianAssignedToEquipment(TECHNICIAN_USERNAME, EQUIPMENT_ID);

    // Assert
    assertThat(result).isFalse();
    verifyNoInteractions(serviceRequestContextFacade);
  }

  // ====================================================================================
  // isTechnicianAssignedToSite(String, Long)
  // ====================================================================================

  @Test
  @DisplayName("isTechnicianAssignedToSite(String, Long) -> true when the technician has an active assignment at the site (AAA)")
  void assignedToSiteTrue() {
    // Arrange
    stubTechnician();
    when(serviceRequestContextFacade.hasActiveAssignmentAtSite(TECHNICIAN_ID, SITE_ID)).thenReturn(true);

    // Act
    var result = service.isTechnicianAssignedToSite(TECHNICIAN_USERNAME, SITE_ID);

    // Assert
    assertThat(result).isTrue();
    verify(serviceRequestContextFacade, times(1)).hasActiveAssignmentAtSite(TECHNICIAN_ID, SITE_ID);
  }

  @Test
  @DisplayName("isTechnicianAssignedToSite(String, Long) -> false when the technician has no active assignment at the site (AAA)")
  void assignedToSiteFalse() {
    // Arrange
    stubTechnician();
    when(serviceRequestContextFacade.hasActiveAssignmentAtSite(TECHNICIAN_ID, SITE_ID)).thenReturn(false);

    // Act
    var result = service.isTechnicianAssignedToSite(TECHNICIAN_USERNAME, SITE_ID);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("isTechnicianAssignedToSite(String, Long) -> false when the username is null (AAA)")
  void assignedToSiteNullUsername() {
    // Act
    var result = service.isTechnicianAssignedToSite(null, SITE_ID);

    // Assert
    assertThat(result).isFalse();
    verifyNoInteractions(iamContextFacade, profilesContextFacade, serviceRequestContextFacade);
  }

  @Test
  @DisplayName("isTechnicianAssignedToSite(String, Long) -> false when IAM answers null for the username (AAA)")
  void assignedToSiteIamNull() {
    // Arrange
    when(iamContextFacade.fetchUserIdByUsername(TECHNICIAN_USERNAME)).thenReturn(null);

    // Act
    var result = service.isTechnicianAssignedToSite(TECHNICIAN_USERNAME, SITE_ID);

    // Assert
    assertThat(result).isFalse();
    verifyNoInteractions(profilesContextFacade, serviceRequestContextFacade);
  }

  @Test
  @DisplayName("isTechnicianAssignedToSite(String, Long) -> false when profiles answers null for the technician (AAA)")
  void assignedToSiteProfilesNull() {
    // Arrange
    when(iamContextFacade.fetchUserIdByUsername(TECHNICIAN_USERNAME)).thenReturn(USER_ID);
    when(profilesContextFacade.fetchTechnicianIdByUserId(USER_ID)).thenReturn(null);

    // Act
    var result = service.isTechnicianAssignedToSite(TECHNICIAN_USERNAME, SITE_ID);

    // Assert
    assertThat(result).isFalse();
    verifyNoInteractions(serviceRequestContextFacade);
  }
}
