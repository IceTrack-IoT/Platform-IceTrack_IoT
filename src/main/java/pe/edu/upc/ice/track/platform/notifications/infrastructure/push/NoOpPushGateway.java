package pe.edu.upc.ice.track.platform.notifications.infrastructure.push;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.notifications.application.internal.outboundservices.push.PushGateway;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/** Default {@link PushGateway}: pushes are only logged. Active unless {@code notifications.push.provider=fcm}. */
@Slf4j
@Service
@ConditionalOnProperty(name = "notifications.push.provider", havingValue = "none", matchIfMissing = true)
public class NoOpPushGateway implements PushGateway {

  @Override
  public Set<String> send(Collection<String> tokens, String title, String body, Map<String, String> data) {
    log.debug("Push provider disabled; skipped push to {} device(s): {}", tokens.size(), body);
    return Set.of();
  }
}
