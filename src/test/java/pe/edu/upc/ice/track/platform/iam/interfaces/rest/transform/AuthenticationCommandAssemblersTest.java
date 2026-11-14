package pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.ClientChannel;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static pe.edu.upc.ice.track.platform.iam.test.fixtures.IamDomainFixtures.*;

class AuthenticationCommandAssemblersTest {

  @ParameterizedTest(name = "[{index}] {0}")
  @EnumSource(ClientChannelResource.class)
  @DisplayName("ClientChannelResource.toDomain() -> maps to the domain channel with the same name (AAA)")
  void channelResourceMapsToDomain(ClientChannelResource resource) {
    assertThat(resource.toDomain().name()).isEqualTo(resource.name());
  }

  @Test
  @DisplayName("ClientChannelResource -> covers every domain channel (AAA)")
  void channelResourceCoversDomain() {
    assertThat(ClientChannelResource.values()).extracting(Enum::name)
        .containsExactlyInAnyOrder(java.util.Arrays.stream(ClientChannel.values()).map(Enum::name).toArray(String[]::new));
  }

  @Test
  @DisplayName("SignInByLocalCommandFromResourceAssembler.toCommandFromResource(SignInWithLocalResource) -> copies credentials and channel (AAA)")
  void signInLocal() {
    // Arrange
    var resource = new SignInWithLocalResource(USERNAME, PASSWORD, ClientChannelResource.MOBILE);

    // Act
    var command = SignInByLocalCommandFromResourceAssembler.toCommandFromResource(resource);

    // Assert
    assertThat(command.username()).isEqualTo(USERNAME);
    assertThat(command.password()).isEqualTo(PASSWORD);
    assertThat(command.channel()).isEqualTo(ClientChannel.MOBILE);
  }

  @Test
  @DisplayName("SignInByLocalCommandFromResourceAssembler.toCommandFromResource(SignInWithLocalResource) -> a missing channel fails (AAA)")
  void signInLocalWithoutChannel() {
    var resource = new SignInWithLocalResource(USERNAME, PASSWORD, null);

    assertThrows(NullPointerException.class,
        () -> SignInByLocalCommandFromResourceAssembler.toCommandFromResource(resource));
  }

  @Test
  @DisplayName("SignInByGoogleCommandFromResourceAssembler.toCommandFromResource(SignInWithGoogleResource) -> copies token and channel (AAA)")
  void signInGoogle() {
    // Act
    var command = SignInByGoogleCommandFromResourceAssembler.toCommandFromResource(
        new SignInWithGoogleResource(GOOGLE_ID_TOKEN, ClientChannelResource.WEB));

    // Assert
    assertThat(command.idToken()).isEqualTo(GOOGLE_ID_TOKEN);
    assertThat(command.channel()).isEqualTo(ClientChannel.WEB);
  }

  @Test
  @DisplayName("LinkGoogleAccountCommandFromResourceAssembler.toCommandFromResource(LinkGoogleAccountResource) -> copies token, password and channel (AAA)")
  void linkGoogle() {
    // Act
    var command = LinkGoogleAccountCommandFromResourceAssembler.toCommandFromResource(
        new LinkGoogleAccountResource(GOOGLE_ID_TOKEN, PASSWORD, ClientChannelResource.WEB));

    // Assert
    assertThat(command.idToken()).isEqualTo(GOOGLE_ID_TOKEN);
    assertThat(command.password()).isEqualTo(PASSWORD);
    assertThat(command.channel()).isEqualTo(ClientChannel.WEB);
  }


  @Test
  @DisplayName("RefreshTokenCommandFromResourceAssembler.toCommandFromResource(RefreshTokenResource) -> maps the stated channel (AAA)")
  void refreshWithChannel() {
    // Act
    var command = RefreshTokenCommandFromResourceAssembler.toCommandFromResource(
        new RefreshTokenResource(RAW_REFRESH_TOKEN, ClientChannelResource.MOBILE));

    // Assert
    assertThat(command.refreshToken()).isEqualTo(RAW_REFRESH_TOKEN);
    assertThat(command.channel()).isEqualTo(ClientChannel.MOBILE);
  }

  @Test
  @DisplayName("RefreshTokenCommandFromResourceAssembler.toCommandFromResource(RefreshTokenResource) -> keeps a null channel as null (AAA)")
  void refreshWithoutChannel() {
    // Act
    var command = RefreshTokenCommandFromResourceAssembler.toCommandFromResource(
        new RefreshTokenResource(RAW_REFRESH_TOKEN, null));

    // Assert
    assertThat(command.channel()).isNull();
  }

  @Test
  @DisplayName("SignOutCommandFromResourceAssembler.toCommandFromResource(RefreshTokenResource) -> carries the refresh token (AAA)")
  void signOut() {
    // Act
    var command = SignOutCommandFromResourceAssembler.toCommandFromResource(
        new RefreshTokenResource(RAW_REFRESH_TOKEN, null));

    // Assert
    assertThat(command.refreshToken()).isEqualTo(RAW_REFRESH_TOKEN);
  }

  @Test
  @DisplayName("SignOutCommandFromResourceAssembler.toCommandFromResource(RefreshTokenResource) -> a missing body fails command validation (AAA)")
  void signOutWithoutBody() {
    var ex = assertThrows(IllegalArgumentException.class,
        () -> SignOutCommandFromResourceAssembler.toCommandFromResource(null));
    assertThat(ex).hasMessage("refresh_token must not be null or blank");
  }


  @Test
  @DisplayName("SignUpOwnerCommandFromResourceAssembler.toCommandFromResource(SignUpOwnerResource) -> builds the contact details and command (AAA)")
  void signUpOwner() {
    // Arrange
    var d = contactDetails();
    var resource = new SignUpOwnerResource(USERNAME, PASSWORD, EMAIL, FULL_NAME, d.phone(), d.street(), d.number(),
        d.city(), d.postalCode(), d.country(), RUC);

    // Act
    var command = SignUpOwnerCommandFromResourceAssembler.toCommandFromResource(resource);

    // Assert
    assertThat(command.username()).isEqualTo(USERNAME);
    assertThat(command.password()).isEqualTo(PASSWORD);
    assertThat(command.email()).isEqualTo(EMAIL);
    assertThat(command.fullName()).isEqualTo(FULL_NAME);
    assertThat(command.contactDetails()).isEqualTo(d);
    assertThat(command.ruc()).isEqualTo(RUC);
  }

  @Test
  @DisplayName("SignUpOwnerCommandFromResourceAssembler.toCommandFromResource(SignUpOwnerResource) -> invalid contact data fails in the domain (AAA)")
  void signUpOwnerInvalidContact() {
    // Arrange
    var resource = new SignUpOwnerResource(USERNAME, PASSWORD, EMAIL, FULL_NAME, " ", "Av. Lima", "1", "Lima",
        "15001", "Peru", RUC);

    // Act & Assert
    assertThrows(IllegalArgumentException.class,
        () -> SignUpOwnerCommandFromResourceAssembler.toCommandFromResource(resource));
  }

  @Test
  @DisplayName("SignUpTechnicianCommandFromResourceAssembler.toCommandFromResource(SignUpTechnicianResource) -> builds the contact details and command (AAA)")
  void signUpTechnician() {
    // Arrange
    var d = contactDetails();
    var resource = new SignUpTechnicianResource(USERNAME, PASSWORD, EMAIL, FULL_NAME, d.phone(), d.street(),
        d.number(), d.city(), d.postalCode(), d.country(), SPECIALITY, CERTIFICATION_NUMBER);

    // Act
    var command = SignUpTechnicianCommandFromResourceAssembler.toCommandFromResource(resource);

    // Assert
    assertThat(command.username()).isEqualTo(USERNAME);
    assertThat(command.email()).isEqualTo(EMAIL);
    assertThat(command.contactDetails()).isEqualTo(d);
    assertThat(command.speciality()).isEqualTo(SPECIALITY);
    assertThat(command.certificationNumber()).isEqualTo(CERTIFICATION_NUMBER);
  }


  @Test
  @DisplayName("CompleteGoogleOwnerRegistrationCommandFromResourceAssembler.toCommandFromResource(...) -> builds the contact details and command (AAA)")
  void completeGoogleOwner() {
    // Arrange
    var d = contactDetails();
    var resource = new CompleteGoogleOwnerRegistrationResource(GOOGLE_ID_TOKEN, USERNAME, d.phone(), d.street(),
        d.number(), d.city(), d.postalCode(), d.country(), RUC);

    // Act
    var command = CompleteGoogleOwnerRegistrationCommandFromResourceAssembler.toCommandFromResource(resource);

    // Assert
    assertThat(command.idToken()).isEqualTo(GOOGLE_ID_TOKEN);
    assertThat(command.username()).isEqualTo(USERNAME);
    assertThat(command.contactDetails()).isEqualTo(d);
    assertThat(command.ruc()).isEqualTo(RUC);
  }

  @Test
  @DisplayName("CompleteGoogleTechnicianRegistrationCommandFromResourceAssembler.toCommandFromResource(...) -> builds the contact details and command (AAA)")
  void completeGoogleTechnician() {
    // Arrange
    var d = contactDetails();
    var resource = new CompleteGoogleTechnicianRegistrationResource(GOOGLE_ID_TOKEN, USERNAME, d.phone(),
        d.street(), d.number(), d.city(), d.postalCode(), d.country(), SPECIALITY, CERTIFICATION_NUMBER);

    // Act
    var command = CompleteGoogleTechnicianRegistrationCommandFromResourceAssembler.toCommandFromResource(resource);

    // Assert
    assertThat(command.idToken()).isEqualTo(GOOGLE_ID_TOKEN);
    assertThat(command.username()).isEqualTo(USERNAME);
    assertThat(command.contactDetails()).isEqualTo(d);
    assertThat(command.speciality()).isEqualTo(SPECIALITY);
    assertThat(command.certificationNumber()).isEqualTo(CERTIFICATION_NUMBER);
  }
}
