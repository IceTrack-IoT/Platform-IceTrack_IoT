package pe.edu.upc.ice.track.platform.notifications.application.internal.outboundservices.push;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Outbound port through which the notifications application layer asks an external push
 * provider (Firebase Cloud Messaging today) to deliver a message to a set of devices.
 *
 * <p>The application layer depends on this port only; the adapter in
 * {@code notifications.infrastructure.push} is the single class that knows the provider's SDK.</p>
 */
public interface PushGateway {

  /**
   * Sends one message to every token.
   *
   * @param tokens registration tokens to deliver to
   * @param title  notification title
   * @param body   notification body
   * @param data   key/value payload delivered to the app (all values must be non-null strings)
   * @return the tokens the provider reported as permanently invalid (unregistered or malformed),
   *         which the caller must delete; transient failures are not included
   */
  Set<String> send(Collection<String> tokens, String title, String body, Map<String, String> data);
}
