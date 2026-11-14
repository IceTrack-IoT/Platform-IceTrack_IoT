package pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import pe.edu.upc.ice.track.platform.iam.domain.exceptions.RefreshTokenException;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.AuthErrorCode;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static pe.edu.upc.ice.track.platform.iam.test.fixtures.IamDomainFixtures.*;

class UserAndRoleResourceAssemblersTest {

  @Test
  @DisplayName("UserResourceFromEntityAssembler.toResourceFromEntity(User) -> exposes id, username, role and provider only (AAA)")
  void userResource() {
    // Act
    var resource = UserResourceFromEntityAssembler.toResourceFromEntity(googleOwner());

    // Assert
    assertThat(resource.id()).isEqualTo(USER_ID);
    assertThat(resource.username()).isEqualTo(USERNAME);
    assertThat(resource.role()).isEqualTo("OWNER_ROLE");
    assertThat(resource.provider()).isEqualTo("GOOGLE");
  }

  @Test
  @DisplayName("CurrentUserResourceFromEntityAssembler.toResourceFromEntity(User, String) -> adds the e-mail to the user data (AAA)")
  void currentUserResource() {
    // Act
    var resource = CurrentUserResourceFromEntityAssembler.toResourceFromEntity(localTechnician(), EMAIL);

    // Assert
    assertThat(resource.id()).isEqualTo(USER_ID);
    assertThat(resource.username()).isEqualTo(USERNAME);
    assertThat(resource.email()).isEqualTo(EMAIL);
    assertThat(resource.role()).isEqualTo("TECHNICIAN_ROLE");
    assertThat(resource.provider()).isEqualTo("LOCAL");
  }

  @Test
  @DisplayName("CurrentUserResourceFromEntityAssembler.toResourceFromEntity(User, String) -> keeps a null e-mail (AAA)")
  void currentUserResourceWithoutEmail() {
    assertThat(CurrentUserResourceFromEntityAssembler.toResourceFromEntity(localOwner(), null).email()).isNull();
  }

  @Test
  @DisplayName("AuthenticatedUserResourceFromEntityAssembler.toResourceFromEntity(User, SessionTokens) -> maps user data and both tokens (AAA)")
  void authenticatedUserResource() {
    // Arrange
    var tokens = sessionTokens();

    // Act
    var resource = AuthenticatedUserResourceFromEntityAssembler.toResourceFromEntity(localOwner(), tokens);

    // Assert
    assertThat(resource.id()).isEqualTo(USER_ID);
    assertThat(resource.username()).isEqualTo(USERNAME);
    assertThat(resource.role()).isEqualTo("OWNER_ROLE");
    assertThat(resource.provider()).isEqualTo("LOCAL");
    assertThat(resource.token()).isEqualTo(tokens.accessToken());
    assertThat(resource.refreshToken()).isEqualTo(tokens.refreshToken());
  }

  @Test
  @DisplayName("RoleResourceFromEntityAssembler.toResourceFromEntity(Role) -> exposes id and role name (AAA)")
  void roleResource() {
    // Act
    var resource = RoleResourceFromEntityAssembler.toResourceFromEntity(ownerRole());

    // Assert
    assertThat(resource.id()).isEqualTo(ROLE_ID);
    assertThat(resource.name()).isEqualTo("OWNER_ROLE");
  }

  @Test
  @DisplayName("AuthErrorResourceFromExceptionAssembler.toResourceFromException(...) -> maps status, error code and default message (AAA)")
  void authErrorResource() {
    // Arrange
    var before = Instant.now();
    var exception = new RefreshTokenException(AuthErrorCode.REFRESH_TOKEN_REPLAY_DETECTED);

    // Act
    var resource = AuthErrorResourceFromExceptionAssembler.toResourceFromException(exception, HttpStatus.UNAUTHORIZED);

    // Assert
    assertThat(resource.status()).isEqualTo(401);
    assertThat(resource.error()).isEqualTo("Unauthorized");
    assertThat(resource.code()).isEqualTo("REFRESH_TOKEN_REPLAY_DETECTED");
    assertThat(resource.message()).isEqualTo(AuthErrorCode.REFRESH_TOKEN_REPLAY_DETECTED.getDefaultMessage());
    assertThat(Instant.parse(resource.timestamp())).isBetween(before, Instant.now());
  }

  @Test
  @DisplayName("AuthErrorResourceFromExceptionAssembler.toResourceFromException(...) -> reflects whichever HTTP status it is given (AAA)")
  void authErrorResourceOtherStatus() {
    // Act
    var resource = AuthErrorResourceFromExceptionAssembler.toResourceFromException(
        new RefreshTokenException(AuthErrorCode.REFRESH_TOKEN_RECENTLY_ROTATED), HttpStatus.CONFLICT);

    // Assert
    assertThat(resource.status()).isEqualTo(409);
    assertThat(resource.error()).isEqualTo("Conflict");
    assertThat(resource.code()).isEqualTo("REFRESH_TOKEN_RECENTLY_ROTATED");
  }
}
