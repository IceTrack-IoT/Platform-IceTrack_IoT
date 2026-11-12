package pe.edu.upc.ice.track.platform.notifications.application.internal.outboundservices.acl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.iam.interfaces.acl.IamContextFacade;
import pe.edu.upc.ice.track.platform.profiles.interfaces.acl.ProfilesContextFacade;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.notifications.test.fixtures.NotificationsDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class ExternalProfilesServiceForNotificationsTest {

  private static final Long USER_ID = 9L;

  @Mock
  private IamContextFacade iamContextFacade;
  @Mock
  private ProfilesContextFacade profilesContextFacade;

  @InjectMocks
  private ExternalProfilesServiceForNotifications service;

  @Test
  @DisplayName("fetchRecipientUserIdByUsername(null or blank) -> empty without any lookup (AAA)")
  void blankUsername() {
    // Act & Assert
    assertThat(service.fetchRecipientUserIdByUsername(null)).isEmpty();
    assertThat(service.fetchRecipientUserIdByUsername("  ")).isEmpty();
    verifyNoInteractions(iamContextFacade, profilesContextFacade);
  }

  @Test
  @DisplayName("fetchRecipientUserIdByUsername(unknown account) -> empty (AAA)")
  void unknownAccount() {
    // Arrange
    when(iamContextFacade.fetchUserIdByUsername(USERNAME)).thenReturn(0L);

    // Act & Assert
    assertThat(service.fetchRecipientUserIdByUsername(USERNAME)).isEmpty();
    verifyNoInteractions(profilesContextFacade);
  }

  @Test
  @DisplayName("fetchRecipientUserIdByUsername(null user id) -> empty (AAA)")
  void nullUserId() {
    // Arrange
    when(iamContextFacade.fetchUserIdByUsername(USERNAME)).thenReturn(null);

    // Act & Assert
    assertThat(service.fetchRecipientUserIdByUsername(USERNAME)).isEmpty();
  }

  @Test
  @DisplayName("fetchRecipientUserIdByUsername(owner account) -> owner profile id, technician not consulted (AAA)")
  void ownerAccount() {
    // Arrange
    when(iamContextFacade.fetchUserIdByUsername(USERNAME)).thenReturn(USER_ID);
    when(profilesContextFacade.fetchOwnerIdByUserId(USER_ID)).thenReturn(OWNER_ID);

    // Act & Assert
    assertThat(service.fetchRecipientUserIdByUsername(USERNAME)).contains(OWNER_ID);
    verify(profilesContextFacade, never()).fetchTechnicianIdByUserId(USER_ID);
  }

  @Test
  @DisplayName("fetchRecipientUserIdByUsername(technician account) -> technician profile id (AAA)")
  void technicianAccount() {
    // Arrange
    when(iamContextFacade.fetchUserIdByUsername(USERNAME)).thenReturn(USER_ID);
    when(profilesContextFacade.fetchOwnerIdByUserId(USER_ID)).thenReturn(0L);
    when(profilesContextFacade.fetchTechnicianIdByUserId(USER_ID)).thenReturn(TECHNICIAN_ID);

    // Act & Assert
    assertThat(service.fetchRecipientUserIdByUsername(USERNAME)).contains(TECHNICIAN_ID);
  }

  @Test
  @DisplayName("fetchRecipientUserIdByUsername(account without profile) -> empty (AAA)")
  void accountWithoutProfile() {
    // Arrange
    when(iamContextFacade.fetchUserIdByUsername(USERNAME)).thenReturn(USER_ID);
    when(profilesContextFacade.fetchOwnerIdByUserId(USER_ID)).thenReturn(null);
    when(profilesContextFacade.fetchTechnicianIdByUserId(USER_ID)).thenReturn(0L);

    // Act & Assert
    assertThat(service.fetchRecipientUserIdByUsername(USERNAME)).isEmpty();
  }

  @Test
  @DisplayName("fetchTechnicianFullName(null) -> empty without lookup (AAA)")
  void fullNameNull() {
    // Act & Assert
    assertThat(service.fetchTechnicianFullName(null)).isEmpty();
    verifyNoInteractions(profilesContextFacade);
  }

  @Test
  @DisplayName("fetchTechnicianFullName(Long) known technician -> the full name (AAA)")
  void fullNameKnown() {
    // Arrange
    when(profilesContextFacade.fetchTechnicianFullNames(List.of(TECHNICIAN_ID)))
        .thenReturn(Map.of(TECHNICIAN_ID, "Ana Torres"));

    // Act & Assert
    assertThat(service.fetchTechnicianFullName(TECHNICIAN_ID)).contains("Ana Torres");
  }

  @Test
  @DisplayName("fetchTechnicianFullName(Long) unknown technician -> empty (AAA)")
  void fullNameUnknown() {
    // Arrange
    when(profilesContextFacade.fetchTechnicianFullNames(List.of(TECHNICIAN_ID))).thenReturn(Map.of());

    // Act & Assert
    assertThat(service.fetchTechnicianFullName(TECHNICIAN_ID)).isEmpty();
  }
}
