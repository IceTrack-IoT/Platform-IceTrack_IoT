package pe.edu.upc.ice.track.platform.notifications.domain.repositories;

import pe.edu.upc.ice.track.platform.notifications.domain.model.aggregates.DeviceToken;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Domain repository port for {@link DeviceToken}. */
public interface DeviceTokenRepository {

  /**
   *  Saves the given device token. If a token with the same value already exists, it is replaced.
   * @param deviceToken the device token to save
   * @return  the saved device token
   */
  DeviceToken save(DeviceToken deviceToken);

  /**
   *  Finds a device token by its value.
   * @param token the device token value
   * @return  an optional containing the device token if found, or empty if not found
   */
  Optional<DeviceToken> findByToken(String token);

  /**
   *  Finds all device tokens associated with a given recipient user ID.
   * @param recipientUserId the recipient user ID
   * @return  a list of device tokens associated with the recipient user ID
   */
  List<DeviceToken> findByRecipientUserId(Long recipientUserId);

  /** Deletes the given tokens. Unknown tokens are ignored. */
  void deleteByTokenIn(Collection<String> tokens);
}
