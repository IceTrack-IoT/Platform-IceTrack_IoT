package pe.edu.upc.ice.track.platform.notifications.application.internal.messages;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

/**
 * Composes notification texts from the {@code notification.*} keys of the {@code messages} bundle.
 *
 * <p>Notifications are composed outside any HTTP request, for a recipient whose language preference
 * profiles do not record, so every text is resolved in {@link #RECIPIENT_LOCALE}, the platform's
 * default locale. Resolving a recipient's own locale only needs this one place to change once such a
 * preference exists. Identifiers are passed as text, so they are never formatted with digit grouping,
 * and the result is cut to the {@value #MAX_MESSAGE_LENGTH} characters a notification holds.</p>
 */
@Component
public class NotificationMessageComposer {

  /** Locale every notification is composed in. */
  public static final Locale RECIPIENT_LOCALE = Locale.ENGLISH;

  /** Longest notification text, matching the {@code notifications.message} column. */
  public static final int MAX_MESSAGE_LENGTH = 255;

  private final MessageSource messageSource;

  public NotificationMessageComposer(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  /**
   * @param key       the message key, under {@code notification.}
   * @param arguments the values to fill in; {@code null} ones are rendered as {@code -}
   * @return the composed text, at most {@value #MAX_MESSAGE_LENGTH} characters
   */
  public String compose(String key, Object... arguments) {
    var texts = Arrays.stream(arguments)
        .map(argument -> Objects.toString(argument, "-"))
        .toArray(Object[]::new);
    var message = messageSource.getMessage(key, texts, RECIPIENT_LOCALE);
    return message.length() <= MAX_MESSAGE_LENGTH ? message : message.substring(0, MAX_MESSAGE_LENGTH - 3) + "...";
  }
}
