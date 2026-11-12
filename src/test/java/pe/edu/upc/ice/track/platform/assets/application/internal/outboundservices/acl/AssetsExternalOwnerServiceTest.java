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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.assets.test.fixtures.AssetsDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class AssetsExternalOwnerServiceTest {

  @Mock
  private IamContextFacade iamContextFacade;
  @Mock
  private ProfilesContextFacade profilesContextFacade;

  @InjectMocks
  private AssetsExternalOwnerService service;

  @Test
  @DisplayName("fetchOwnerIdByUserId(Long) -> returns the owner id the profiles context resolves (AAA)")
  void fetchOwnerIdByUserIdSuccess() {
    // Arrange
    when(profilesContextFacade.fetchOwnerIdByUserId(USER_ID)).thenReturn(OWNER_ID);

    // Act
    var result = service.fetchOwnerIdByUserId(USER_ID);

    // Assert
    assertThat(result).contains(OWNER_ID);
    verify(profilesContextFacade, times(1)).fetchOwnerIdByUserId(USER_ID);
    verifyNoMoreInteractions(profilesContextFacade);
  }

  @Test
  @DisplayName("fetchOwnerIdByUserId(Long) -> returns empty without calling profiles when the user id is null (AAA)")
  void fetchOwnerIdByUserIdNullUser() {
    // Act
    var result = service.fetchOwnerIdByUserId(null);

    // Assert
    assertThat(result).isEmpty();
    verifyNoInteractions(profilesContextFacade);
  }

  @Test
  @DisplayName("fetchOwnerIdByUserId(Long) -> returns empty when profiles answers the 0 sentinel (AAA)")
  void fetchOwnerIdByUserIdZeroSentinel() {
    // Arrange
    when(profilesContextFacade.fetchOwnerIdByUserId(USER_ID)).thenReturn(0L);

    // Act
    var result = service.fetchOwnerIdByUserId(USER_ID);

    // Assert
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("fetchOwnerIdByUserId(Long) -> returns empty when profiles answers null (AAA)")
  void fetchOwnerIdByUserIdNullAnswer() {
    // Arrange
    when(profilesContextFacade.fetchOwnerIdByUserId(USER_ID)).thenReturn(null);

    // Act
    var result = service.fetchOwnerIdByUserId(USER_ID);

    // Assert
    assertThat(result).isEmpty();
  }

  // ====================================================================================
  // fetchOwnerIdByUsername(String)
  // ====================================================================================

  @Test
  @DisplayName("fetchOwnerIdByUsername(String) -> resolves the user in IAM and then its owner profile (AAA)")
  void fetchOwnerIdByUsernameSuccess() {
    // Arrange
    when(iamContextFacade.fetchUserIdByUsername(USERNAME)).thenReturn(USER_ID);
    when(profilesContextFacade.fetchOwnerIdByUserId(USER_ID)).thenReturn(OWNER_ID);

    // Act
    var result = service.fetchOwnerIdByUsername(USERNAME);

    // Assert
    assertThat(result).contains(OWNER_ID);
    verify(iamContextFacade, times(1)).fetchUserIdByUsername(USERNAME);
    verify(profilesContextFacade, times(1)).fetchOwnerIdByUserId(USER_ID);
    verifyNoMoreInteractions(iamContextFacade, profilesContextFacade);
  }

  @ParameterizedTest(name = "[{index}] username={0}")
  @MethodSource("pe.edu.upc.ice.track.platform.shared.test.fixtures.CommonCommandFixtures#invalidBlankStrings")
  @DisplayName("fetchOwnerIdByUsername(String) -> returns empty without calling any context for a blank username (AAA)")
  void fetchOwnerIdByUsernameBlank(String username) {
    // Act
    var result = service.fetchOwnerIdByUsername(username);

    // Assert
    assertThat(result).isEmpty();
    verifyNoInteractions(iamContextFacade, profilesContextFacade);
  }

  @Test
  @DisplayName("fetchOwnerIdByUsername(String) -> returns empty without calling profiles when IAM answers the 0 sentinel (AAA)")
  void fetchOwnerIdByUsernameUnknownUserZero() {
    // Arrange
    when(iamContextFacade.fetchUserIdByUsername(USERNAME)).thenReturn(0L);

    // Act
    var result = service.fetchOwnerIdByUsername(USERNAME);

    // Assert
    assertThat(result).isEmpty();
    verifyNoInteractions(profilesContextFacade);
  }

  @Test
  @DisplayName("fetchOwnerIdByUsername(String) -> returns empty without calling profiles when IAM answers null (AAA)")
  void fetchOwnerIdByUsernameUnknownUserNull() {
    // Arrange
    when(iamContextFacade.fetchUserIdByUsername(USERNAME)).thenReturn(null);

    // Act
    var result = service.fetchOwnerIdByUsername(USERNAME);

    // Assert
    assertThat(result).isEmpty();
    verifyNoInteractions(profilesContextFacade);
  }

  @Test
  @DisplayName("fetchOwnerIdByUsername(String) -> returns empty when the user holds no owner profile (AAA)")
  void fetchOwnerIdByUsernameNoOwnerProfile() {
    // Arrange
    when(iamContextFacade.fetchUserIdByUsername(USERNAME)).thenReturn(USER_ID);
    when(profilesContextFacade.fetchOwnerIdByUserId(USER_ID)).thenReturn(0L);

    // Act
    var result = service.fetchOwnerIdByUsername(USERNAME);

    // Assert
    assertThat(result).isEmpty();
  }
}
