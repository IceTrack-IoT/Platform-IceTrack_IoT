package pe.edu.upc.ice.track.platform.iam.domain.model.commands;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.ClientChannel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static pe.edu.upc.ice.track.platform.iam.test.fixtures.IamCommandFixtures.*;
import static pe.edu.upc.ice.track.platform.iam.test.fixtures.IamDomainFixtures.*;

class IamCommandsTest {

  @Test
  @DisplayName("SignUpOwnerCommand(...) -> trims and lower-cases the email (AAA)")
  void signUpOwnerNormalizesEmail() {
    // Act
    var command = new SignUpOwnerCommand(USERNAME, PASSWORD, "  JPerez@Example.COM ", FULL_NAME, contactDetails(), RUC);

    // Assert
    assertThat(command.email()).isEqualTo("jperez@example.com");
  }

  @ParameterizedTest(name = "[{index}] username=''{0}''")
  @NullAndEmptySource
  @ValueSource(strings = {" "})
  @DisplayName("SignUpOwnerCommand(...) -> rejects a blank username (AAA)")
  void signUpOwnerRejectsBlankUsername(String username) {
    var ex = assertThrows(IllegalArgumentException.class,
        () -> new SignUpOwnerCommand(username, PASSWORD, EMAIL, FULL_NAME, contactDetails(), RUC));
    assertThat(ex).hasMessage("username must not be null or blank");
  }

  @ParameterizedTest(name = "[{index}] password=''{0}''")
  @NullAndEmptySource
  @DisplayName("SignUpOwnerCommand(...) -> rejects a blank password (AAA)")
  void signUpOwnerRejectsBlankPassword(String password) {
    var ex = assertThrows(IllegalArgumentException.class,
        () -> new SignUpOwnerCommand(USERNAME, password, EMAIL, FULL_NAME, contactDetails(), RUC));
    assertThat(ex).hasMessage("password must not be null or blank");
  }

  @ParameterizedTest(name = "[{index}] email=''{0}''")
  @NullAndEmptySource
  @DisplayName("SignUpOwnerCommand(...) -> rejects a blank email (AAA)")
  void signUpOwnerRejectsBlankEmail(String email) {
    var ex = assertThrows(IllegalArgumentException.class,
        () -> new SignUpOwnerCommand(USERNAME, PASSWORD, email, FULL_NAME, contactDetails(), RUC));
    assertThat(ex).hasMessage("email must not be null or blank");
  }

  @ParameterizedTest(name = "[{index}] fullName=''{0}''")
  @NullAndEmptySource
  @DisplayName("SignUpOwnerCommand(...) -> rejects a blank full name (AAA)")
  void signUpOwnerRejectsBlankFullName(String fullName) {
    var ex = assertThrows(IllegalArgumentException.class,
        () -> new SignUpOwnerCommand(USERNAME, PASSWORD, EMAIL, fullName, contactDetails(), RUC));
    assertThat(ex).hasMessage("fullName must not be null or blank");
  }

  @Test
  @DisplayName("SignUpOwnerCommand(...) -> rejects null contact details and null ruc (AAA)")
  void signUpOwnerRejectsNullDetailsAndRuc() {
    assertThrows(IllegalArgumentException.class,
        () -> new SignUpOwnerCommand(USERNAME, PASSWORD, EMAIL, FULL_NAME, null, RUC));
    assertThrows(IllegalArgumentException.class,
        () -> new SignUpOwnerCommand(USERNAME, PASSWORD, EMAIL, FULL_NAME, contactDetails(), null));
  }

  @Test
  @DisplayName("SignUpTechnicianCommand(...) -> trims and lower-cases the email (AAA)")
  void signUpTechnicianNormalizesEmail() {
    var command = new SignUpTechnicianCommand(USERNAME, PASSWORD, " TECH@Example.com", FULL_NAME,
        contactDetails(), SPECIALITY, CERTIFICATION_NUMBER);

    assertThat(command.email()).isEqualTo("tech@example.com");
  }

  @Test
  @DisplayName("SignUpTechnicianCommand(...) -> rejects null contact details (AAA)")
  void signUpTechnicianRejectsNullDetails() {
    assertThrows(IllegalArgumentException.class, () -> new SignUpTechnicianCommand(USERNAME, PASSWORD, EMAIL,
        FULL_NAME, null, SPECIALITY, CERTIFICATION_NUMBER));
  }

  @ParameterizedTest(name = "[{index}] speciality=''{0}''")
  @NullAndEmptySource
  @DisplayName("SignUpTechnicianCommand(...) -> rejects a blank speciality (AAA)")
  void signUpTechnicianRejectsBlankSpeciality(String speciality) {
    var ex = assertThrows(IllegalArgumentException.class, () -> new SignUpTechnicianCommand(USERNAME, PASSWORD,
        EMAIL, FULL_NAME, contactDetails(), speciality, CERTIFICATION_NUMBER));
    assertThat(ex).hasMessage("speciality must not be null or blank");
  }

  @ParameterizedTest(name = "[{index}] certificationNumber=''{0}''")
  @NullAndEmptySource
  @DisplayName("SignUpTechnicianCommand(...) -> rejects a blank certification number (AAA)")
  void signUpTechnicianRejectsBlankCertification(String certification) {
    var ex = assertThrows(IllegalArgumentException.class, () -> new SignUpTechnicianCommand(USERNAME, PASSWORD,
        EMAIL, FULL_NAME, contactDetails(), SPECIALITY, certification));
    assertThat(ex).hasMessage("certificationNumber must not be null or blank");
  }

  @Test
  @DisplayName("Sign-up fixtures -> build valid commands (AAA)")
  void signUpFixturesAreValid() {
    assertDoesNotThrow(() -> signUpOwnerCommand());
    assertDoesNotThrow(() -> signUpTechnicianCommand());
  }

  @ParameterizedTest(name = "[{index}] username=''{0}''")
  @NullAndEmptySource
  @ValueSource(strings = {" "})
  @DisplayName("SignInByLocalCommand(...) -> rejects a blank username (AAA)")
  void signInLocalRejectsBlankUsername(String username) {
    assertThrows(IllegalArgumentException.class,
        () -> new SignInByLocalCommand(username, PASSWORD, ClientChannel.WEB));
  }

  @Test
  @DisplayName("SignInByLocalCommand(...) -> rejects a null channel but tolerates a null password (AAA)")
  void signInLocalChannelAndPassword() {
    assertThrows(IllegalArgumentException.class, () -> new SignInByLocalCommand(USERNAME, PASSWORD, null));
    assertDoesNotThrow(() -> new SignInByLocalCommand(USERNAME, null, ClientChannel.WEB));
  }

  @ParameterizedTest(name = "[{index}] idToken=''{0}''")
  @NullAndEmptySource
  @ValueSource(strings = {" "})
  @DisplayName("SignInByGoogleCommand(...) -> rejects a blank id token (AAA)")
  void signInGoogleRejectsBlankToken(String token) {
    var ex = assertThrows(IllegalArgumentException.class, () -> new SignInByGoogleCommand(token, ClientChannel.WEB));
    assertThat(ex).hasMessage("Google id_token must not be null or blank");
  }

  @Test
  @DisplayName("SignInByGoogleCommand -> rejects a null channel (AAA)")
  void signInGoogleRejectsNullChannel() {
    var ex = assertThrows(IllegalArgumentException.class, () -> new SignInByGoogleCommand(GOOGLE_ID_TOKEN, null));
    assertThat(ex).hasMessage("channel must not be null");
  }

  @Test
  @DisplayName("LinkGoogleAccountCommand -> rejects blank token and null channel (AAA)")
  void linkGoogleValidations() {
    assertThrows(IllegalArgumentException.class, () -> new LinkGoogleAccountCommand(" ", PASSWORD, ClientChannel.WEB));
    assertThrows(IllegalArgumentException.class, () -> new LinkGoogleAccountCommand(GOOGLE_ID_TOKEN, PASSWORD, null));
    assertDoesNotThrow(() -> new LinkGoogleAccountCommand(GOOGLE_ID_TOKEN, null, ClientChannel.WEB));
  }

  @ParameterizedTest(name = "[{index}] refreshToken=''{0}''")
  @NullAndEmptySource
  @ValueSource(strings = {" "})
  @DisplayName("RefreshTokenCommand -> rejects a blank refresh token (AAA)")
  void refreshRejectsBlankToken(String token) {
    var ex = assertThrows(IllegalArgumentException.class, () -> new RefreshTokenCommand(token, ClientChannel.WEB));
    assertThat(ex).hasMessage("refresh_token must not be null or blank");
  }

  @Test
  @DisplayName("RefreshTokenCommand -> accepts a null channel (AAA)")
  void refreshAcceptsNullChannel() {
    assertThat(new RefreshTokenCommand(RAW_REFRESH_TOKEN, null).channel()).isNull();
  }

  @ParameterizedTest(name = "[{index}] refreshToken=''{0}''")
  @NullAndEmptySource
  @ValueSource(strings = {" "})
  @DisplayName("SignOutCommand -> rejects a blank refresh token (AAA)")
  void signOutRejectsBlankToken(String token) {
    var ex = assertThrows(IllegalArgumentException.class, () -> new SignOutCommand(token));
    assertThat(ex).hasMessage("refresh_token must not be null or blank");
  }

  @ParameterizedTest(name = "[{index}] idToken=''{0}''")
  @NullAndEmptySource
  @DisplayName("CompleteGoogleOwnerRegistrationCommand -> rejects a blank id token (AAA)")
  void completeOwnerRejectsBlankToken(String token) {
    assertThrows(IllegalArgumentException.class,
        () -> new CompleteGoogleOwnerRegistrationCommand(token, USERNAME, contactDetails(), RUC));
  }

  @Test
  @DisplayName("CompleteGoogleOwnerRegistrationCommand -> rejects blank username, null details and null ruc (AAA)")
  void completeOwnerOtherValidations() {
    assertThrows(IllegalArgumentException.class,
        () -> new CompleteGoogleOwnerRegistrationCommand(GOOGLE_ID_TOKEN, " ", contactDetails(), RUC));
    assertThrows(IllegalArgumentException.class,
        () -> new CompleteGoogleOwnerRegistrationCommand(GOOGLE_ID_TOKEN, USERNAME, null, RUC));
    assertThrows(IllegalArgumentException.class,
        () -> new CompleteGoogleOwnerRegistrationCommand(GOOGLE_ID_TOKEN, USERNAME, contactDetails(), null));
    assertDoesNotThrow(() -> completeGoogleOwnerRegistrationCommand());
  }

  @Test
  @DisplayName("CompleteGoogleTechnicianRegistrationCommand -> validates every mandatory field (AAA)")
  void completeTechnicianValidations() {
    assertThrows(IllegalArgumentException.class, () -> new CompleteGoogleTechnicianRegistrationCommand(
        null, USERNAME, contactDetails(), SPECIALITY, CERTIFICATION_NUMBER));
    assertThrows(IllegalArgumentException.class, () -> new CompleteGoogleTechnicianRegistrationCommand(
        GOOGLE_ID_TOKEN, "", contactDetails(), SPECIALITY, CERTIFICATION_NUMBER));
    assertThrows(IllegalArgumentException.class, () -> new CompleteGoogleTechnicianRegistrationCommand(
        GOOGLE_ID_TOKEN, USERNAME, null, SPECIALITY, CERTIFICATION_NUMBER));
    assertThrows(IllegalArgumentException.class, () -> new CompleteGoogleTechnicianRegistrationCommand(
        GOOGLE_ID_TOKEN, USERNAME, contactDetails(), " ", CERTIFICATION_NUMBER));
    assertThrows(IllegalArgumentException.class, () -> new CompleteGoogleTechnicianRegistrationCommand(
        GOOGLE_ID_TOKEN, USERNAME, contactDetails(), SPECIALITY, null));
  }
}
