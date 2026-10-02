package pe.edu.upc.ice.track.platform.iam.application.internal.eventhandlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.iam.application.commandservices.UserCommandService;
import pe.edu.upc.ice.track.platform.iam.domain.model.commands.PurgeExpiredRefreshTokensCommand;

/**
 * Scheduled handler that periodically purges expired refresh tokens.
 *
 * <p>The schedule is the {@code authorization.refresh-token.purge.cron} property. Running it on
 * several instances at once is harmless: the purge is an idempotent bulk delete.</p>
 */
@Service
@Slf4j
public class RefreshTokenPurgeScheduler {
  private final UserCommandService userCommandService;

  public RefreshTokenPurgeScheduler(UserCommandService userCommandService) {
    this.userCommandService = userCommandService;
  }

  /**
   * Triggers the purge of expired refresh tokens.
   *
   * <p>A failure is logged and swallowed, so that the next scheduled run still takes place.</p>
   */
  @Scheduled(cron = "${authorization.refresh-token.purge.cron}")
  public void purgeExpiredRefreshTokens() {
    try {
      userCommandService.handle(new PurgeExpiredRefreshTokensCommand());
    } catch (RuntimeException exception) {
      log.error("Purging expired refresh tokens failed: {}", exception.getMessage(), exception);
    }
  }
}
