package pe.edu.upc.ice.track.platform.device.infrastructure.credentials;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static pe.edu.upc.ice.track.platform.device.test.fixtures.DeviceDomainFixtures.VALID_API_KEY;

class DeviceCredentialServiceImplTest {

  private final DeviceCredentialServiceImpl service = new DeviceCredentialServiceImpl();

  private static String sha256Hex(String value) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  @DisplayName("issue() -> returns a well formed key whose digest is the SHA-256 of that key (AAA)")
  void issueReturnsWellFormedKeyAndMatchingDigest() throws Exception {
    // Act
    var credential = service.issue();

    // Assert
    assertThat(credential.plainText()).matches("it_dev_[0-9a-f]{64}");
    assertThat(credential.digest()).matches("[0-9a-f]{64}").isEqualTo(sha256Hex(credential.plainText()));
  }

  @Test
  @DisplayName("issue() -> never repeats a key across calls (AAA)")
  void issueIsUnique() {
    // Arrange
    var keys = new HashSet<String>();

    // Act
    for (int i = 0; i < 50; i++) {
      keys.add(service.issue().plainText());
    }

    // Assert
    assertThat(keys).hasSize(50);
  }


  @Test
  @DisplayName("digestOf(String) -> returns the SHA-256 hex of a well formed key (AAA)")
  void digestOfWellFormedKey() throws Exception {
    // Act
    var digest = service.digestOf(VALID_API_KEY);

    // Assert
    assertThat(digest).contains(sha256Hex(VALID_API_KEY));
  }

  @ParameterizedTest(name = "[{index}] apiKey={0}")
  @MethodSource("pe.edu.upc.ice.track.platform.shared.test.fixtures.CommonCommandFixtures#invalidBlankStrings")
  @DisplayName("digestOf(String) -> empty for a blank key (AAA)")
  void digestOfBlankKey(String apiKey) {
    // Act & Assert
    assertThat(service.digestOf(apiKey)).isEmpty();
  }

  @ParameterizedTest(name = "[{index}] apiKey={0}")
  @ValueSource(strings = {
      "plain-text",
      "it_dev_",
      "it_dev_abc",
      "wrong_prefix_cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc",
      "it_dev_CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC",
      "it_dev_cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc0",
      " it_dev_cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc"
  })
  @DisplayName("digestOf(String) -> empty for a key that does not match the format (AAA)")
  void digestOfMalformedKey(String apiKey) {
    // Act & Assert
    assertThat(service.digestOf(apiKey)).isEmpty();
  }


  @Test
  @DisplayName("matches(String, String) -> true for the digest of the same key (AAA)")
  void matchesCorrectDigest() throws Exception {
    // Act & Assert
    assertThat(service.matches(VALID_API_KEY, sha256Hex(VALID_API_KEY))).isTrue();
  }

  @Test
  @DisplayName("matches(String, String) -> true for an issued credential and its own digest (AAA)")
  void matchesIssuedCredential() {
    // Arrange
    var credential = service.issue();

    // Act & Assert
    assertThat(service.matches(credential.plainText(), credential.digest())).isTrue();
  }

  @Test
  @DisplayName("matches(String, String) -> false for the digest of a different key (AAA)")
  void matchesWrongDigest() throws Exception {
    // Act & Assert
    assertThat(service.matches(VALID_API_KEY, sha256Hex("it_dev_" + "d".repeat(64)))).isFalse();
  }

  @Test
  @DisplayName("matches(String, String) -> false for a malformed key even with a matching digest (AAA)")
  void matchesMalformedKey() throws Exception {
    // Act & Assert
    assertThat(service.matches("garbage", sha256Hex("garbage"))).isFalse();
  }

  @ParameterizedTest(name = "[{index}] storedDigest={0}")
  @MethodSource("pe.edu.upc.ice.track.platform.shared.test.fixtures.CommonCommandFixtures#invalidBlankStrings")
  @DisplayName("matches(String, String) -> false for a blank stored digest (AAA)")
  void matchesBlankDigest(String storedDigest) {
    // Act & Assert
    assertThat(service.matches(VALID_API_KEY, storedDigest)).isFalse();
  }

  @Test
  @DisplayName("matches(String, String) -> false for a stored digest that is not hexadecimal (AAA)")
  void matchesNonHexDigest() {
    // Act & Assert
    assertThat(service.matches(VALID_API_KEY, "zz-not-hex")).isFalse();
  }

  @Test
  @DisplayName("matches(String, String) -> false for a null key (AAA)")
  void matchesNullKey() {
    // Act & Assert
    assertThat(service.matches(null, "a".repeat(64))).isFalse();
  }
}
