package pe.edu.upc.ice.track.platform.iam.domain.model.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.ClientChannel;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static pe.edu.upc.ice.track.platform.iam.test.fixtures.IamDomainFixtures.*;

class RefreshTokenTest {

  @Test
  @DisplayName("issue(Long, String, Instant, ClientChannel) -> creates an unsaved, active token (AAA)")
  void issueCreatesActiveToken() {
    // Arrange
    var expiry = Instant.now().plusSeconds(3600);

    // Act
    var token = RefreshToken.issue(USER_ID, REFRESH_DIGEST, expiry, ClientChannel.MOBILE);

    // Assert
    assertThat(token.getId()).isNull();
    assertThat(token.getUserId()).isEqualTo(USER_ID);
    assertThat(token.getToken()).isEqualTo(REFRESH_DIGEST);
    assertThat(token.getExpiryDate()).isEqualTo(expiry);
    assertThat(token.getChannel()).isEqualTo(ClientChannel.MOBILE);
    assertThat(token.isRevoked()).isFalse();
    assertThat(token.getRevokedAt()).isNull();
    assertThat(token.getReplacedByToken()).isNull();
    assertThat(token.isExpired()).isFalse();
    assertThat(token.isRotated()).isFalse();
  }

  @Test
  @DisplayName("issue(Long, String, Instant, ClientChannel) -> rejects a null channel (AAA)")
  void issueRejectsNullChannel() {
    // Act & Assert
    var ex = assertThrows(IllegalArgumentException.class,
        () -> RefreshToken.issue(USER_ID, REFRESH_DIGEST, Instant.now().plusSeconds(60), null));
    assertThat(ex).hasMessage("channel must not be null");
  }

  @Test
  @DisplayName("RefreshToken(...) -> rejects a null userId (AAA)")
  void constructorRejectsNullUserId() {
    // Act & Assert
    var ex = assertThrows(IllegalArgumentException.class,
        () -> new RefreshToken(null, null, REFRESH_DIGEST, Instant.now(), false, null, null, ClientChannel.WEB));
    assertThat(ex).hasMessage("userId must not be null");
  }

  @ParameterizedTest(name = "[{index}] token=''{0}''")
  @NullAndEmptySource
  @ValueSource(strings = {"  "})
  @DisplayName("RefreshToken(...) -> rejects a null or blank token digest (AAA)")
  void constructorRejectsBlankToken(String token) {
    // Act & Assert
    var ex = assertThrows(IllegalArgumentException.class,
        () -> new RefreshToken(null, USER_ID, token, Instant.now(), false, null, null, ClientChannel.WEB));
    assertThat(ex).hasMessage("token must not be null or blank");
  }

  @Test
  @DisplayName("RefreshToken(...) -> rejects a null expiry date (AAA)")
  void constructorRejectsNullExpiry() {
    // Act & Assert
    var ex = assertThrows(IllegalArgumentException.class,
        () -> new RefreshToken(null, USER_ID, REFRESH_DIGEST, null, false, null, null, ClientChannel.WEB));
    assertThat(ex).hasMessage("expiryDate must not be null");
  }

  @Test
  @DisplayName("RefreshToken(...) -> allows a null channel for legacy rows (AAA)")
  void constructorAllowsNullChannel() {
    // Act
    var token = new RefreshToken(1L, USER_ID, REFRESH_DIGEST, Instant.now(), false, null, null, null);

    // Assert
    assertThat(token.getChannel()).isNull();
  }

  @Test
  @DisplayName("rotate(String) -> revokes the token and records its replacement (AAA)")
  void rotateRevokesAndRecordsReplacement() {
    // Arrange
    var token = activeRefreshToken();
    var before = Instant.now();

    // Act
    token.rotate(OTHER_REFRESH_DIGEST);

    // Assert
    assertThat(token.isRevoked()).isTrue();
    assertThat(token.getReplacedByToken()).isEqualTo(OTHER_REFRESH_DIGEST);
    assertThat(token.getRevokedAt()).isBetween(before, Instant.now());
    assertThat(token.isRotated()).isTrue();
  }

  @ParameterizedTest(name = "[{index}] newToken=''{0}''")
  @NullAndEmptySource
  @ValueSource(strings = {" "})
  @DisplayName("rotate(String) -> rejects a null or blank replacement and stays active (AAA)")
  void rotateRejectsBlankReplacement(String newToken) {
    // Arrange
    var token = activeRefreshToken();

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> token.rotate(newToken));
    assertThat(token.isRevoked()).isFalse();
  }

  @Test
  @DisplayName("rotate(String) -> rejects an already revoked token (AAA)")
  void rotateRejectsRevokedToken() {
    // Arrange
    var token = revokedRefreshToken();

    // Act & Assert
    var ex = assertThrows(IllegalStateException.class, () -> token.rotate(OTHER_REFRESH_DIGEST));
    assertThat(ex).hasMessage("A revoked refresh token cannot be rotated");
  }

  @Test
  @DisplayName("isRotated() -> false for a token revoked without replacement (AAA)")
  void isRotatedFalseWhenOnlyRevoked() {
    assertThat(revokedRefreshToken().isRotated()).isFalse();
  }

  @Test
  @DisplayName("isExpired() -> true once the expiry date has passed (AAA)")
  void isExpiredAfterExpiryDate() {
    assertThat(expiredRefreshToken().isExpired()).isTrue();
    assertThat(activeRefreshToken().isExpired()).isFalse();
  }

  @Test
  @DisplayName("isWithinGracePeriod(Duration) -> true for a token revoked moments ago (AAA)")
  void withinGracePeriodWhenRecentlyRevoked() {
    // Arrange
    var token = rotatedRefreshToken(Instant.now().minusSeconds(1), ClientChannel.WEB);

    // Assert
    assertThat(token.isWithinGracePeriod(GRACE_PERIOD)).isTrue();
  }

  @Test
  @DisplayName("isWithinGracePeriod(Duration) -> false once the grace window has elapsed (AAA)")
  void outsideGracePeriodWhenOldRevocation() {
    // Arrange
    var token = rotatedRefreshToken(Instant.now().minus(Duration.ofMinutes(5)), ClientChannel.WEB);

    // Assert
    assertThat(token.isWithinGracePeriod(GRACE_PERIOD)).isFalse();
  }

  @Test
  @DisplayName("isWithinGracePeriod(Duration) -> false when not revoked, without revokedAt or with null period (AAA)")
  void gracePeriodFalseForDegenerateStates() {
    // Arrange
    var revokedWithoutTimestamp = new RefreshToken(1L, USER_ID, REFRESH_DIGEST, Instant.now().plusSeconds(60),
        true, null, null, ClientChannel.WEB);

    // Assert
    assertThat(activeRefreshToken().isWithinGracePeriod(GRACE_PERIOD)).isFalse();
    assertThat(revokedWithoutTimestamp.isWithinGracePeriod(GRACE_PERIOD)).isFalse();
    assertThat(revokedRefreshToken().isWithinGracePeriod(null)).isFalse();
  }
}
