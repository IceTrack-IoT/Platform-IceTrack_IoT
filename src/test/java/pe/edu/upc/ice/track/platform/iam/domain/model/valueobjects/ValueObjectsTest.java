package pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import pe.edu.upc.ice.track.platform.iam.domain.exceptions.RefreshTokenException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static pe.edu.upc.ice.track.platform.iam.test.fixtures.IamDomainFixtures.*;

class ValueObjectsTest {

  @ParameterizedTest(name = "[{index}] ''{0}'' -> {1}")
  @CsvSource({
      "OWNER_ROLE,OWNER_ROLE",
      "owner,OWNER_ROLE",
      "  Owner  ,OWNER_ROLE",
      "technician_role,TECHNICIAN_ROLE",
      "TECHNICIAN,TECHNICIAN_ROLE"
  })
  @DisplayName("Roles.fromName(String) -> normalizes case, whitespace and the _ROLE suffix (AAA)")
  void fromNameNormalizes(String input, Roles expected) {
    // Act & Assert
    assertThat(Roles.fromName(input)).isEqualTo(expected);
  }

  @ParameterizedTest(name = "[{index}] name=''{0}''")
  @NullAndEmptySource
  @ValueSource(strings = {"  "})
  @DisplayName("Roles.fromName(String) -> rejects a null or blank name (AAA)")
  void fromNameRejectsBlank(String input) {
    // Act & Assert
    var ex = assertThrows(IllegalArgumentException.class, () -> Roles.fromName(input));
    assertThat(ex).hasMessage("Role must not be null or blank");
  }

  @Test
  @DisplayName("Roles.fromName(String) -> rejects an unknown role with a descriptive message (AAA)")
  void fromNameRejectsUnknown() {
    // Act & Assert
    var ex = assertThrows(IllegalArgumentException.class, () -> Roles.fromName("admin"));
    assertThat(ex).hasMessage("Unknown role 'admin'; expected OWNER_ROLE or TECHNICIAN_ROLE");
  }

  @Test
  @DisplayName("ClientChannel.getAdmittedRole() -> WEB admits owners and MOBILE admits technicians (AAA)")
  void channelAdmittedRoles() {
    assertThat(ClientChannel.WEB.getAdmittedRole()).isEqualTo(Roles.OWNER_ROLE);
    assertThat(ClientChannel.MOBILE.getAdmittedRole()).isEqualTo(Roles.TECHNICIAN_ROLE);
  }

  @Test
  @DisplayName("ClientChannel.admits(User) -> true only for a user holding the admitted role (AAA)")
  void channelAdmitsMatchingUserOnly() {
    assertThat(ClientChannel.WEB.admits(localOwner())).isTrue();
    assertThat(ClientChannel.WEB.admits(localTechnician())).isFalse();
    assertThat(ClientChannel.MOBILE.admits(localTechnician())).isTrue();
    assertThat(ClientChannel.MOBILE.admits(localOwner())).isFalse();
  }

  @Test
  @DisplayName("ClientChannel.admits(User) -> false for a null user (AAA)")
  void channelAdmitsNullUser() {
    assertThat(ClientChannel.WEB.admits(null)).isFalse();
  }

  @Test
  @DisplayName("ClientChannel.forRole(Roles) -> maps each role to its channel (AAA)")
  void forRoleMapsRoles() {
    assertThat(ClientChannel.forRole(Roles.OWNER_ROLE)).isEqualTo(ClientChannel.WEB);
    assertThat(ClientChannel.forRole(Roles.TECHNICIAN_ROLE)).isEqualTo(ClientChannel.MOBILE);
  }

  @Test
  @DisplayName("ClientChannel.forRole(Roles) -> rejects a null role (AAA)")
  void forRoleRejectsNull() {
    assertThrows(IllegalArgumentException.class, () -> ClientChannel.forRole(null));
  }

  @Test
  @DisplayName("ClientChannel.requiredFor(User) -> returns the channel that admits the user (AAA)")
  void requiredForReturnsChannel() {
    assertThat(ClientChannel.requiredFor(localOwner())).isEqualTo(ClientChannel.WEB);
    assertThat(ClientChannel.requiredFor(googleTechnician())).isEqualTo(ClientChannel.MOBILE);
  }

  @Test
  @DisplayName("ContactDetails(...) -> keeps values and allows a null street number (AAA)")
  void contactDetailsAllowsNullNumber() {
    // Act
    var details = new ContactDetails("999", "Av. Lima", null, "Lima", "15001", "Peru");

    // Assert
    assertThat(details.number()).isNull();
    assertThat(details.phone()).isEqualTo("999");
  }

  @ParameterizedTest(name = "[{index}] blank {0}")
  @ValueSource(strings = {"phone", "street", "city", "postalCode", "country"})
  @DisplayName("ContactDetails(...) -> rejects each mandatory field when blank (AAA)")
  void contactDetailsRejectsBlankField(String field) {
    // Arrange
    var phone = field.equals("phone") ? " " : "999";
    var street = field.equals("street") ? null : "Av. Lima";
    var city = field.equals("city") ? "" : "Lima";
    var postalCode = field.equals("postalCode") ? " " : "15001";
    var country = field.equals("country") ? null : "Peru";

    // Act & Assert
    var ex = assertThrows(IllegalArgumentException.class,
        () -> new ContactDetails(phone, street, "1", city, postalCode, country));
    assertThat(ex).hasMessage("%s must not be null or blank".formatted(field));
  }

  @Test
  @DisplayName("ContactDetails -> has value-based equality (AAA)")
  void contactDetailsValueEquality() {
    assertThat(contactDetails()).isEqualTo(contactDetails());
  }


  @Test
  @DisplayName("SessionTokens(String, String) -> keeps both tokens (AAA)")
  void sessionTokensKeepsValues() {
    // Act
    var tokens = new SessionTokens(ACCESS_TOKEN, RAW_REFRESH_TOKEN);

    // Assert
    assertThat(tokens.accessToken()).isEqualTo(ACCESS_TOKEN);
    assertThat(tokens.refreshToken()).isEqualTo(RAW_REFRESH_TOKEN);
  }

  @ParameterizedTest(name = "[{index}] access=''{0}''")
  @NullAndEmptySource
  @ValueSource(strings = {" "})
  @DisplayName("SessionTokens(String, String) -> rejects a null or blank access token (AAA)")
  void sessionTokensRejectsBlankAccess(String access) {
    var ex = assertThrows(IllegalArgumentException.class, () -> new SessionTokens(access, RAW_REFRESH_TOKEN));
    assertThat(ex).hasMessage("accessToken must not be null or blank");
  }

  @ParameterizedTest(name = "[{index}] refresh=''{0}''")
  @NullAndEmptySource
  @ValueSource(strings = {" "})
  @DisplayName("SessionTokens(String, String) -> rejects a null or blank refresh token (AAA)")
  void sessionTokensRejectsBlankRefresh(String refresh) {
    var ex = assertThrows(IllegalArgumentException.class, () -> new SessionTokens(ACCESS_TOKEN, refresh));
    assertThat(ex).hasMessage("refreshToken must not be null or blank");
  }

  @Test
  @DisplayName("AuthErrorCode.getDefaultMessage() -> every code has a non-blank message (AAA)")
  void everyAuthErrorCodeHasMessage() {
    for (var code : AuthErrorCode.values()) {
      assertThat(code.getDefaultMessage()).isNotBlank();
    }
  }

  @Test
  @DisplayName("RefreshTokenException(AuthErrorCode) -> exposes the code and uses its default message (AAA)")
  void exceptionExposesCodeAndMessage() {
    // Act
    var exception = new RefreshTokenException(AuthErrorCode.REFRESH_TOKEN_EXPIRED);

    // Assert
    assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
    assertThat(exception).hasMessage(AuthErrorCode.REFRESH_TOKEN_EXPIRED.getDefaultMessage());
  }

  @Test
  @DisplayName("RefreshTokenException(AuthErrorCode) -> rejects a null code (AAA)")
  void exceptionRejectsNullCode() {
    var ex = assertThrows(IllegalArgumentException.class, () -> new RefreshTokenException(null));
    assertThat(ex).hasMessage("errorCode must not be null");
  }
}
