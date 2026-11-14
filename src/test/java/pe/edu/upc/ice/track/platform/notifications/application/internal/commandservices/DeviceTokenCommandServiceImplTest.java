package pe.edu.upc.ice.track.platform.notifications.application.internal.commandservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.notifications.domain.model.aggregates.DeviceToken;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.RegisterDeviceTokenCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.UnregisterDeviceTokenCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.DevicePlatform;
import pe.edu.upc.ice.track.platform.notifications.domain.repositories.DeviceTokenRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.notifications.test.fixtures.NotificationsCommandFixtures.registerTokenCommand;
import static pe.edu.upc.ice.track.platform.notifications.test.fixtures.NotificationsCommandFixtures.unregisterTokenCommand;
import static pe.edu.upc.ice.track.platform.notifications.test.fixtures.NotificationsDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class DeviceTokenCommandServiceImplTest {

  @Mock
  private DeviceTokenRepository deviceTokenRepository;

  @InjectMocks
  private DeviceTokenCommandServiceImpl service;

  @Test
  @DisplayName("handle(RegisterDeviceTokenCommand) unknown token -> creates and saves a new device token (AAA)")
  void registerNewToken() {
    // Arrange
    when(deviceTokenRepository.findByToken(TOKEN)).thenReturn(Optional.empty());
    when(deviceTokenRepository.save(any(DeviceToken.class))).thenAnswer(i -> i.getArgument(0));

    // Act
    var result = service.handle(registerTokenCommand());

    // Assert
    assertThat(result.getRecipientUserId()).isEqualTo(RECIPIENT_ID);
    assertThat(result.getToken()).isEqualTo(TOKEN);
    assertThat(result.getPlatform()).isEqualTo(DevicePlatform.ANDROID);
    verify(deviceTokenRepository, times(1)).save(result);
  }

  @Test
  @DisplayName("handle(RegisterDeviceTokenCommand) known token -> reassigns the existing one instead of duplicating (AAA)")
  void registerExistingTokenReassigns() {
    // Arrange
    var existing = persistedDeviceToken();
    when(deviceTokenRepository.findByToken(TOKEN)).thenReturn(Optional.of(existing));
    when(deviceTokenRepository.save(existing)).thenReturn(existing);
    var command = new RegisterDeviceTokenCommand(OTHER_RECIPIENT_ID, TOKEN, DevicePlatform.IOS);

    // Act
    var result = service.handle(command);

    // Assert
    assertThat(result).isSameAs(existing);
    assertThat(existing.getRecipientUserId()).isEqualTo(OTHER_RECIPIENT_ID);
    assertThat(existing.getPlatform()).isEqualTo(DevicePlatform.IOS);
    assertThat(existing.getId()).isEqualTo(1L);
    verify(deviceTokenRepository, times(1)).save(existing);
  }

  @Test
  @DisplayName("handle(UnregisterDeviceTokenCommand) owned token -> deletes it (AAA)")
  void unregisterOwnedToken() {
    // Arrange
    when(deviceTokenRepository.findByToken(TOKEN)).thenReturn(Optional.of(persistedDeviceToken()));

    // Act
    service.handle(unregisterTokenCommand());

    // Assert
    verify(deviceTokenRepository, times(1)).deleteByTokenIn(List.of(TOKEN));
  }

  @Test
  @DisplayName("handle(UnregisterDeviceTokenCommand) token of another recipient -> nothing is deleted (AAA)")
  void unregisterForeignToken() {
    // Arrange
    when(deviceTokenRepository.findByToken(TOKEN)).thenReturn(Optional.of(persistedDeviceToken()));
    var command = new UnregisterDeviceTokenCommand(OTHER_RECIPIENT_ID, TOKEN);

    // Act
    service.handle(command);

    // Assert
    verify(deviceTokenRepository, never()).deleteByTokenIn(any());
  }

  @Test
  @DisplayName("handle(UnregisterDeviceTokenCommand) unknown token -> no-op (AAA)")
  void unregisterUnknownToken() {
    // Arrange
    when(deviceTokenRepository.findByToken(TOKEN)).thenReturn(Optional.empty());

    // Act
    service.handle(unregisterTokenCommand());

    // Assert
    verify(deviceTokenRepository, never()).deleteByTokenIn(any());
  }
}
