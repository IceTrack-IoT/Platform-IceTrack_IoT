package pe.edu.upc.ice.track.platform.notifications.domain.model.aggregates;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.DevicePlatform;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pe.edu.upc.ice.track.platform.notifications.test.fixtures.NotificationsDomainFixtures.*;

class DeviceTokenTest {

  @Test
  @DisplayName("new DeviceToken(recipient, token, platform) -> unsaved token seen just now (AAA)")
  void constructorBuildsToken() {
    // Arrange
    var before = Instant.now();

    // Act
    var deviceToken = deviceToken();

    // Assert
    assertThat(deviceToken.getId()).isNull();
    assertThat(deviceToken.getRecipientUserId()).isEqualTo(RECIPIENT_ID);
    assertThat(deviceToken.getToken()).isEqualTo(TOKEN);
    assertThat(deviceToken.getPlatform()).isEqualTo(DevicePlatform.ANDROID);
    assertThat(deviceToken.getLastSeenAt()).isAfterOrEqualTo(before);
  }

  @ParameterizedTest(name = "[{index}] token <{0}>")
  @MethodSource("pe.edu.upc.ice.track.platform.shared.test.fixtures.CommonCommandFixtures#invalidBlankStrings")
  @DisplayName("new DeviceToken(blank token) -> IllegalArgumentException (AAA)")
  void constructorRejectsBlankToken(String blank) {
    // Arrange & Act & Assert
    assertThatThrownBy(() -> new DeviceToken(RECIPIENT_ID, blank, DevicePlatform.IOS))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("new DeviceToken(null recipient or platform) -> NullPointerException (AAA)")
  void constructorRejectsMissingReferences() {
    // Arrange & Act & Assert
    assertThatThrownBy(() -> new DeviceToken(null, TOKEN, DevicePlatform.IOS)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new DeviceToken(RECIPIENT_ID, TOKEN, null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("rehydrate(...) -> restores the stored id and last-seen instant (AAA)")
  void rehydrateRestoresState() {
    // Arrange & Act
    var deviceToken = persistedDeviceToken();

    // Assert
    assertThat(deviceToken.getId()).isEqualTo(1L);
    assertThat(deviceToken.getLastSeenAt()).isEqualTo(T0);
  }

  @Test
  @DisplayName("reassignTo(newRecipient, newPlatform) -> moves the token to another account and refreshes last seen (AAA)")
  void reassignToMovesToken() {
    // Arrange
    var deviceToken = persistedDeviceToken();

    // Act
    deviceToken.reassignTo(OTHER_RECIPIENT_ID, DevicePlatform.WEB);

    // Assert
    assertThat(deviceToken.getRecipientUserId()).isEqualTo(OTHER_RECIPIENT_ID);
    assertThat(deviceToken.getPlatform()).isEqualTo(DevicePlatform.WEB);
    assertThat(deviceToken.getLastSeenAt()).isAfter(T0);
    assertThat(deviceToken.getToken()).isEqualTo(TOKEN);
  }

  @Test
  @DisplayName("reassignTo(null, platform) -> NullPointerException and state untouched (AAA)")
  void reassignToRejectsNulls() {
    // Arrange
    var deviceToken = persistedDeviceToken();

    // Act & Assert
    assertThatThrownBy(() -> deviceToken.reassignTo(null, DevicePlatform.WEB)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> deviceToken.reassignTo(OTHER_RECIPIENT_ID, null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("belongsTo(recipient) -> true only for the current owner of the token (AAA)")
  void belongsToMatchesOwner() {
    // Arrange
    var deviceToken = deviceToken();

    // Act & Assert
    assertThat(deviceToken.belongsTo(RECIPIENT_ID)).isTrue();
    assertThat(deviceToken.belongsTo(OTHER_RECIPIENT_ID)).isFalse();
    assertThat(deviceToken.belongsTo(null)).isFalse();
  }
}
