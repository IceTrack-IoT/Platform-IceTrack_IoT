package pe.edu.upc.ice.track.platform.notifications.application.internal.messages;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationMessageComposerTest {

  private static final String KEY = "notification.sample";

  @Mock
  private MessageSource messageSource;

  @InjectMocks
  private NotificationMessageComposer composer;

  @Test
  @DisplayName("compose(key, args) -> resolves in English with every argument rendered as text (AAA)")
  void composeStringifiesArguments() {
    // Arrange
    var instant = Instant.parse("2026-01-01T10:00:00Z");
    when(messageSource.getMessage(KEY, new Object[] {"1234567", "Cold Room", instant.toString()},
        NotificationMessageComposer.RECIPIENT_LOCALE)).thenReturn("hello");

    // Act
    var result = composer.compose(KEY, 1234567L, "Cold Room", instant);

    // Assert
    assertThat(result).isEqualTo("hello");
    verify(messageSource, times(1)).getMessage(KEY, new Object[] {"1234567", "Cold Room", instant.toString()},
        java.util.Locale.ENGLISH);
  }

  @Test
  @DisplayName("compose(key, null argument) -> null is rendered as a dash (AAA)")
  void composeNullBecomesDash() {
    // Arrange
    when(messageSource.getMessage(KEY, new Object[] {"-", "x"}, java.util.Locale.ENGLISH)).thenReturn("ok");

    // Act
    var result = composer.compose(KEY, null, "x");

    // Assert
    assertThat(result).isEqualTo("ok");
  }

  @Test
  @DisplayName("compose(key) with a text of exactly 255 characters -> returned untouched (AAA)")
  void composeKeepsMaxLength() {
    // Arrange
    var text = "a".repeat(NotificationMessageComposer.MAX_MESSAGE_LENGTH);
    when(messageSource.getMessage(KEY, new Object[] {}, java.util.Locale.ENGLISH)).thenReturn(text);

    // Act
    var result = composer.compose(KEY);

    // Assert
    assertThat(result).isEqualTo(text).hasSize(255);
  }

  @Test
  @DisplayName("compose(key) with a longer text -> cut to 255 characters ending in an ellipsis (AAA)")
  void composeTruncatesLongText() {
    // Arrange
    when(messageSource.getMessage(KEY, new Object[] {}, java.util.Locale.ENGLISH)).thenReturn("b".repeat(300));

    // Act
    var result = composer.compose(KEY);

    // Assert
    assertThat(result).hasSize(255).endsWith("...").startsWith("b".repeat(252));
  }
}
