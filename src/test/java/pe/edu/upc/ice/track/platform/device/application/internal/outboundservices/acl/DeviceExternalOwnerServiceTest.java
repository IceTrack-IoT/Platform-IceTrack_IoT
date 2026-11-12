package pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.acl;

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
import static pe.edu.upc.ice.track.platform.device.test.fixtures.DeviceDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class DeviceExternalOwnerServiceTest {

  @Mock
  private IamContextFacade iamContextFacade;
  @Mock
  private ProfilesContextFacade profilesContextFacade;

  @InjectMocks
  private DeviceExternalOwnerService service;

  @Test
  @DisplayName("fetchOwnerIdByUsername(String) -> resolves the user in IAM and then its owner profile (AAA)")
  void fetchOwnerIdSuccess() {
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
  @DisplayName("fetchOwnerIdByUsername(String) -> empty without calling any context for a blank username (AAA)")
  void fetchOwnerIdBlankUsername(String username) {
    // Act
    var result = service.fetchOwnerIdByUsername(username);

    // Assert
    assertThat(result).isEmpty();
    verifyNoInteractions(iamContextFacade, profilesContextFacade);
  }

  @Test
  @DisplayName("fetchOwnerIdByUsername(String) -> empty without calling profiles when IAM answers the 0 sentinel (AAA)")
  void fetchOwnerIdUnknownUserZero() {
    // Arrange
    when(iamContextFacade.fetchUserIdByUsername(USERNAME)).thenReturn(0L);

    // Act
    var result = service.fetchOwnerIdByUsername(USERNAME);

    // Assert
    assertThat(result).isEmpty();
    verifyNoInteractions(profilesContextFacade);
  }

  @Test
  @DisplayName("fetchOwnerIdByUsername(String) -> empty without calling profiles when IAM answers null (AAA)")
  void fetchOwnerIdUnknownUserNull() {
    // Arrange
    when(iamContextFacade.fetchUserIdByUsername(USERNAME)).thenReturn(null);

    // Act
    var result = service.fetchOwnerIdByUsername(USERNAME);

    // Assert
    assertThat(result).isEmpty();
    verifyNoInteractions(profilesContextFacade);
  }

  @Test
  @DisplayName("fetchOwnerIdByUsername(String) -> empty when profiles answers the 0 sentinel (AAA)")
  void fetchOwnerIdNoOwnerProfileZero() {
    // Arrange
    when(iamContextFacade.fetchUserIdByUsername(USERNAME)).thenReturn(USER_ID);
    when(profilesContextFacade.fetchOwnerIdByUserId(USER_ID)).thenReturn(0L);

    // Act & Assert
    assertThat(service.fetchOwnerIdByUsername(USERNAME)).isEmpty();
  }

  @Test
  @DisplayName("fetchOwnerIdByUsername(String) -> empty when profiles answers null (AAA)")
  void fetchOwnerIdNoOwnerProfileNull() {
    // Arrange
    when(iamContextFacade.fetchUserIdByUsername(USERNAME)).thenReturn(USER_ID);
    when(profilesContextFacade.fetchOwnerIdByUserId(USER_ID)).thenReturn(null);

    // Act & Assert
    assertThat(service.fetchOwnerIdByUsername(USERNAME)).isEmpty();
  }
}
