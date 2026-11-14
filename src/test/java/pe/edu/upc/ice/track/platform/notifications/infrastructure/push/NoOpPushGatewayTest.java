package pe.edu.upc.ice.track.platform.notifications.infrastructure.push;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NoOpPushGatewayTest {

  @Test
  @DisplayName("send(tokens, title, body, data) -> reports no invalid tokens (AAA)")
  void sendReportsNothingInvalid() {
    // Arrange
    var gateway = new NoOpPushGateway();

    // Act
    var result = gateway.send(List.of("a", "b"), "title", "body", Map.of("k", "v"));

    // Assert
    assertThat(result).isEmpty();
  }
}
