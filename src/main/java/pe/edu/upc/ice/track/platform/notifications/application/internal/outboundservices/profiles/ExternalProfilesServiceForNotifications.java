package pe.edu.upc.ice.track.platform.notifications.application.internal.outboundservices.profiles;

import java.util.Optional;

/**
 * Outbound port (Anti-Corruption Layer) through which notifications resolves <em>who the caller is</em>:
 * the recipient identifier that notifications are addressed to (the caller's owner profile).
 */
public interface ExternalProfilesServiceForNotifications {

  /**
   * @param username username of the authenticated account
   * @return the owner profile identifier, or empty when the account does not exist or holds no owner profile
   */
  Optional<Long> fetchRecipientUserIdByUsername(String username);
}
