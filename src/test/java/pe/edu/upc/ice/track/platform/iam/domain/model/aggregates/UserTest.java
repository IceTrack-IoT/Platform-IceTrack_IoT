package pe.edu.upc.ice.track.platform.iam.domain.model.aggregates;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import pe.edu.upc.ice.track.platform.iam.domain.model.entities.Role;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.AuthProvider;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.Roles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static pe.edu.upc.ice.track.platform.iam.test.fixtures.IamDomainFixtures.*;

class UserTest {

  @Test
  @DisplayName("User(...) -> keeps every supplied value (AAA)")
  void constructorKeepsValues() {
    // Act
    var user = new User(USER_ID, USERNAME, ENCODED_PASSWORD, ownerRole(), AuthProvider.LOCAL, null);

    // Assert
    assertThat(user.getId()).isEqualTo(USER_ID);
    assertThat(user.getUsername()).isEqualTo(USERNAME);
    assertThat(user.getPassword()).isEqualTo(ENCODED_PASSWORD);
    assertThat(user.getRole()).isEqualTo(ownerRole());
    assertThat(user.getProvider()).isEqualTo(AuthProvider.LOCAL);
    assertThat(user.getExternalId()).isNull();
  }

  @Test
  @DisplayName("User(...) -> null password becomes empty and null provider defaults to LOCAL (AAA)")
  void constructorAppliesDefaults() {
    // Act
    var user = new User(null, USERNAME, null, ownerRole(), null, null);

    // Assert
    assertThat(user.getPassword()).isEmpty();
    assertThat(user.getProvider()).isEqualTo(AuthProvider.LOCAL);
    assertThat(user.hasLocalPassword()).isFalse();
  }

  @ParameterizedTest(name = "[{index}] username=''{0}''")
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t"})
  @DisplayName("User(...) -> rejects a null or blank username (AAA)")
  void constructorRejectsBlankUsername(String username) {
    // Act & Assert
    var ex = assertThrows(IllegalArgumentException.class,
        () -> new User(null, username, ENCODED_PASSWORD, ownerRole(), AuthProvider.LOCAL, null));
    assertThat(ex).hasMessage("username must not be null or blank");
  }

  @Test
  @DisplayName("User(...) -> rejects a null role (AAA)")
  void constructorRejectsNullRole() {
    // Act & Assert
    assertThrows(IllegalArgumentException.class,
        () -> new User(null, USERNAME, ENCODED_PASSWORD, null, AuthProvider.LOCAL, null));
  }

  @Test
  @DisplayName("User(...) -> rejects a role without a name (AAA)")
  void constructorRejectsRoleWithoutName() {
    // Arrange
    var nameless = new Role(1L, null);

    // Act & Assert
    assertThrows(IllegalArgumentException.class,
        () -> new User(null, USERNAME, ENCODED_PASSWORD, nameless, AuthProvider.LOCAL, null));
  }

  @Test
  @DisplayName("registeredLocally(String, String, Role) -> builds an unsaved LOCAL user (AAA)")
  void registeredLocallyBuildsLocalUser() {
    // Act
    var user = User.registeredLocally(USERNAME, ENCODED_PASSWORD, ownerRole());

    // Assert
    assertThat(user.getId()).isNull();
    assertThat(user.getProvider()).isEqualTo(AuthProvider.LOCAL);
    assertThat(user.getPassword()).isEqualTo(ENCODED_PASSWORD);
    assertThat(user.getExternalId()).isNull();
    assertThat(user.hasLocalPassword()).isTrue();
    assertThat(user.isFederated()).isFalse();
  }

  @ParameterizedTest(name = "[{index}] password=''{0}''")
  @NullAndEmptySource
  @ValueSource(strings = {"  "})
  @DisplayName("registeredLocally(String, String, Role) -> rejects a null or blank encoded password (AAA)")
  void registeredLocallyRejectsBlankPassword(String password) {
    // Act & Assert
    var ex = assertThrows(IllegalArgumentException.class,
        () -> User.registeredLocally(USERNAME, password, ownerRole()));
    assertThat(ex).hasMessage("password must not be null or blank");
  }

  @Test
  @DisplayName("registeredWithGoogle(String, String, Role) -> builds an unsaved GOOGLE user without password (AAA)")
  void registeredWithGoogleBuildsFederatedUser() {
    // Act
    var user = User.registeredWithGoogle(USERNAME, GOOGLE_SUB, ownerRole());

    // Assert
    assertThat(user.getId()).isNull();
    assertThat(user.getProvider()).isEqualTo(AuthProvider.GOOGLE);
    assertThat(user.getExternalId()).isEqualTo(GOOGLE_SUB);
    assertThat(user.hasLocalPassword()).isFalse();
    assertThat(user.isFederated()).isTrue();
  }

  @ParameterizedTest(name = "[{index}] sub=''{0}''")
  @NullAndEmptySource
  @ValueSource(strings = {" "})
  @DisplayName("registeredWithGoogle(String, String, Role) -> rejects a null or blank google sub (AAA)")
  void registeredWithGoogleRejectsBlankSub(String sub) {
    // Act & Assert
    var ex = assertThrows(IllegalArgumentException.class,
        () -> User.registeredWithGoogle(USERNAME, sub, ownerRole()));
    assertThat(ex).hasMessage("googleSub must not be null or blank");
  }

  @Test
  @DisplayName("changePassword(String) -> replaces the stored password (AAA)")
  void changePasswordReplacesPassword() {
    // Arrange
    var user = localOwner();

    // Act
    user.changePassword("new-encoded");

    // Assert
    assertThat(user.getPassword()).isEqualTo("new-encoded");
  }

  @Test
  @DisplayName("changePassword(String) -> rejects a blank password and keeps the previous one (AAA)")
  void changePasswordRejectsBlank() {
    // Arrange
    var user = localOwner();

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> user.changePassword(" "));
    assertThat(user.getPassword()).isEqualTo(ENCODED_PASSWORD);
  }

  @Test
  @DisplayName("linkGoogleAccount(String) -> stores the sub and switches the provider to GOOGLE (AAA)")
  void linkGoogleAccountSwitchesProvider() {
    // Arrange
    var user = localOwner();

    // Act
    user.linkGoogleAccount(GOOGLE_SUB);

    // Assert
    assertThat(user.getExternalId()).isEqualTo(GOOGLE_SUB);
    assertThat(user.getProvider()).isEqualTo(AuthProvider.GOOGLE);
    assertThat(user.isFederated()).isTrue();
    assertThat(user.hasLocalPassword()).isTrue();
  }

  @Test
  @DisplayName("linkGoogleAccount(String) -> rejects a blank sub and leaves the account untouched (AAA)")
  void linkGoogleAccountRejectsBlank() {
    // Arrange
    var user = localOwner();

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> user.linkGoogleAccount(""));
    assertThat(user.getProvider()).isEqualTo(AuthProvider.LOCAL);
    assertThat(user.getExternalId()).isNull();
  }

  @Test
  @DisplayName("hasRole(Roles) -> true only for the assigned role (AAA)")
  void hasRoleMatchesAssignedRole() {
    // Arrange
    var owner = localOwner();

    // Assert
    assertThat(owner.hasRole(Roles.OWNER_ROLE)).isTrue();
    assertThat(owner.hasRole(Roles.TECHNICIAN_ROLE)).isFalse();
    assertThat(localTechnician().hasRole(Roles.TECHNICIAN_ROLE)).isTrue();
  }

  @Test
  @DisplayName("getRoleName() / getProviderName() -> expose the enum names (AAA)")
  void exposesRoleAndProviderNames() {
    // Arrange
    var user = googleOwner();

    // Assert
    assertThat(user.getRoleName()).isEqualTo("OWNER_ROLE");
    assertThat(user.getProviderName()).isEqualTo("GOOGLE");
  }
}
