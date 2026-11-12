package pe.edu.upc.ice.track.platform.iam.application.internal.outboundservices.acl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.profiles.interfaces.acl.ProfilesContextFacade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.iam.test.fixtures.IamDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class ExternalProfileServiceTest {

  @Mock
  private ProfilesContextFacade profilesContextFacade;

  @InjectMocks
  private ExternalProfileService service;

  @Test
  @DisplayName("createOwnerProfile(...) -> forwards every value to the profiles facade and returns the profile id (AAA)")
  void createOwnerProfileDelegates() {
    // Arrange
    var d = contactDetails();
    when(profilesContextFacade.createOwnerProfile(USER_ID, FULL_NAME, EMAIL, d.phone(), d.street(), d.number(),
        d.city(), d.postalCode(), d.country(), RUC)).thenReturn(PROFILE_ID);

    // Act
    var id = service.createOwnerProfile(USER_ID, FULL_NAME, EMAIL, d.phone(), d.street(), d.number(), d.city(),
        d.postalCode(), d.country(), RUC);

    // Assert
    assertThat(id).isEqualTo(PROFILE_ID);
  }

  @Test
  @DisplayName("createTechnicianProfile(...) -> forwards every value to the profiles facade and returns the profile id (AAA)")
  void createTechnicianProfileDelegates() {
    // Arrange
    var d = contactDetails();
    when(profilesContextFacade.createTechnicianProfile(USER_ID, FULL_NAME, EMAIL, d.phone(), d.street(),
        d.number(), d.city(), d.postalCode(), d.country(), SPECIALITY, CERTIFICATION_NUMBER)).thenReturn(PROFILE_ID);

    // Act
    var id = service.createTechnicianProfile(USER_ID, FULL_NAME, EMAIL, d.phone(), d.street(), d.number(),
        d.city(), d.postalCode(), d.country(), SPECIALITY, CERTIFICATION_NUMBER);

    // Assert
    assertThat(id).isEqualTo(PROFILE_ID);
  }

  @Test
  @DisplayName("createOwnerProfile(...) -> lets a profiles validation failure propagate (AAA)")
  void createOwnerProfilePropagatesFailure() {
    // Arrange
    var d = contactDetails();
    when(profilesContextFacade.createOwnerProfile(USER_ID, FULL_NAME, EMAIL, d.phone(), d.street(), d.number(),
        d.city(), d.postalCode(), d.country(), RUC)).thenThrow(new IllegalArgumentException("bad phone"));

    // Act & Assert
    org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
        () -> service.createOwnerProfile(USER_ID, FULL_NAME, EMAIL, d.phone(), d.street(), d.number(), d.city(),
            d.postalCode(), d.country(), RUC));
  }

  @Test
  @DisplayName("fetchEmailByUserId(Long) -> returns the e-mail when the profile has one (AAA)")
  void fetchEmailReturnsValue() {
    // Arrange
    when(profilesContextFacade.fetchEmailByUserId(USER_ID)).thenReturn(EMAIL);

    // Act & Assert
    assertThat(service.fetchEmailByUserId(USER_ID)).contains(EMAIL);
  }

  @Test
  @DisplayName("fetchEmailByUserId(Long) -> returns empty for a null or blank e-mail (AAA)")
  void fetchEmailReturnsEmptyWhenAbsent() {
    // Arrange
    when(profilesContextFacade.fetchEmailByUserId(USER_ID)).thenReturn(null);
    doReturn("  ").when(profilesContextFacade).fetchEmailByUserId(2L);

    // Act & Assert
    assertThat(service.fetchEmailByUserId(USER_ID)).isEmpty();
    assertThat(service.fetchEmailByUserId(2L)).isEmpty();
  }

  @Test
  @DisplayName("fetchUserIdByEmail(String) -> returns the user id when found (AAA)")
  void fetchUserIdReturnsValue() {
    // Arrange
    when(profilesContextFacade.fetchUserIdByEmail(EMAIL)).thenReturn(USER_ID);

    // Act
    var result = service.fetchUserIdByEmail(EMAIL);

    // Assert
    assertThat(result).contains(USER_ID);
    verify(profilesContextFacade, times(1)).fetchUserIdByEmail(EMAIL);
  }

  @Test
  @DisplayName("fetchUserIdByEmail(String) -> treats null and 0 as 'not found' (AAA)")
  void fetchUserIdReturnsEmptyForNullOrZero() {
    // Arrange
    when(profilesContextFacade.fetchUserIdByEmail("none@example.com")).thenReturn(null);
    doReturn(0L).when(profilesContextFacade).fetchUserIdByEmail("zero@example.com");

    // Act & Assert
    assertThat(service.fetchUserIdByEmail("none@example.com")).isEmpty();
    assertThat(service.fetchUserIdByEmail("zero@example.com")).isEmpty();
  }
}
