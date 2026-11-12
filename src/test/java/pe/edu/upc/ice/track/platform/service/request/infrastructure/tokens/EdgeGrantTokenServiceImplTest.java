package pe.edu.upc.ice.track.platform.service.request.infrastructure.tokens;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.EdgeGrant;
import pe.edu.upc.ice.track.platform.service.request.infrastructure.tokens.EdgeGrantTokenServiceImpl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class EdgeGrantTokenServiceImplTest {

  private final EdgeGrantTokenServiceImpl service = new EdgeGrantTokenServiceImpl();

  @Test
  @DisplayName("generate() -> it_grt_ prefix followed by 64 lowercase hex characters (AAA)")
  void format() {
    // Act
    var token = service.generate();

    // Assert
    assertThat(token.plainText()).startsWith(EdgeGrant.TOKEN_PREFIX).matches("it_grt_[0-9a-f]{64}");
  }

  @Test
  @DisplayName("generate() -> digest is the SHA-256 hex of the plain text (AAA)")
  void digest() throws Exception {
    // Act
    var token = service.generate();

    // Assert
    var expected = HexFormat.of().formatHex(
        MessageDigest.getInstance("SHA-256").digest(token.plainText().getBytes(StandardCharsets.UTF_8)));
    assertThat(token.digest()).isEqualTo(expected).hasSize(64);
    assertThat(token.digest()).isNotEqualTo(token.plainText());
  }

  @Test
  @DisplayName("generate() -> every call yields a different secret and digest (AAA)")
  void unique() {
    // Act
    var tokens = IntStream.range(0, 50).mapToObj(i -> service.generate()).toList();

    // Assert
    assertThat(new HashSet<>(tokens.stream().map(t -> t.plainText()).toList())).hasSize(50);
    assertThat(new HashSet<>(tokens.stream().map(t -> t.digest()).toList())).hasSize(50);
  }
}
