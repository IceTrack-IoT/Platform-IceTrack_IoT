package pe.edu.upc.ice.track.platform.iam.infrastructure.tokens.refresh.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RefreshTokenServiceImplTest {

  private final RefreshTokenServiceImpl service = new RefreshTokenServiceImpl(7, 30);

  @Test
  @DisplayName("RefreshTokenServiceImpl(long, long) -> exposes the configured grace period (AAA)")
  void exposesGracePeriod() {
    assertThat(service.getReuseGracePeriod()).isEqualTo(Duration.ofSeconds(30));
  }

  @Test
  @DisplayName("RefreshTokenServiceImpl(long, long) -> accepts a zero grace period (AAA)")
  void acceptsZeroGracePeriod() {
    assertThat(new RefreshTokenServiceImpl(1, 0).getReuseGracePeriod()).isEqualTo(Duration.ZERO);
  }

  @ParameterizedTest(name = "[{index}] days={0}")
  @ValueSource(longs = {0, -1})
  @DisplayName("RefreshTokenServiceImpl(long, long) -> rejects a non-positive lifetime (AAA)")
  void rejectsNonPositiveLifetime(long days) {
    var ex = assertThrows(IllegalArgumentException.class, () -> new RefreshTokenServiceImpl(days, 30));
    assertThat(ex).hasMessage("authorization.refresh-token.expiration.days must be positive");
  }

  @Test
  @DisplayName("RefreshTokenServiceImpl(long, long) -> rejects a negative grace period (AAA)")
  void rejectsNegativeGracePeriod() {
    var ex = assertThrows(IllegalArgumentException.class, () -> new RefreshTokenServiceImpl(7, -1));
    assertThat(ex).hasMessage("authorization.refresh-token.reuse-grace-period.seconds must not be negative");
  }

  @Test
  @DisplayName("generateToken() -> returns URL-safe, unpadded tokens of 32 random bytes (AAA)")
  void generatesUrlSafeTokens() {
    // Act
    var token = service.generateToken();

    // Assert
    assertThat(token).matches("[A-Za-z0-9_-]{43}");
  }

  @Test
  @DisplayName("generateToken() -> never repeats across many calls (AAA)")
  void generatesUniqueTokens() {
    // Act
    var tokens = new HashSet<String>();
    for (int i = 0; i < 1_000; i++) {
      tokens.add(service.generateToken());
    }

    // Assert
    assertThat(tokens).hasSize(1_000);
  }

  @Test
  @DisplayName("hashToken(String) -> returns the lowercase hex SHA-256 digest (AAA)")
  void hashesWithSha256() {
    // Act & Assert (well-known SHA-256 of "abc")
    assertThat(service.hashToken("abc"))
        .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
  }

  @Test
  @DisplayName("hashToken(String) -> is deterministic and input-sensitive (AAA)")
  void hashIsDeterministic() {
    assertThat(service.hashToken("token-a")).isEqualTo(service.hashToken("token-a"));
    assertThat(service.hashToken("token-a")).isNotEqualTo(service.hashToken("token-b"));
    assertThat(service.hashToken("token-a")).hasSize(64);
  }

  @ParameterizedTest(name = "[{index}] token=''{0}''")
  @NullAndEmptySource
  @ValueSource(strings = {" "})
  @DisplayName("hashToken(String) -> rejects a null or blank token (AAA)")
  void hashRejectsBlank(String token) {
    var ex = assertThrows(IllegalArgumentException.class, () -> service.hashToken(token));
    assertThat(ex).hasMessage("refresh token must not be null or blank");
  }

  @Test
  @DisplayName("calculateExpiryDate(Instant) -> adds the configured lifetime to the issue instant (AAA)")
  void calculatesExpiry() {
    // Arrange
    var issuedAt = Instant.parse("2026-01-01T00:00:00Z");

    // Act & Assert
    assertThat(service.calculateExpiryDate(issuedAt)).isEqualTo(Instant.parse("2026-01-08T00:00:00Z"));
  }
}
