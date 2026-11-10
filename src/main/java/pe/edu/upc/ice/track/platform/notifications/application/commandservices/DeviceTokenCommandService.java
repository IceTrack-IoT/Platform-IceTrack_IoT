package pe.edu.upc.ice.track.platform.notifications.application.commandservices;

import pe.edu.upc.ice.track.platform.notifications.domain.model.aggregates.DeviceToken;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.RegisterDeviceTokenCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.UnregisterDeviceTokenCommand;

/** Application service handling write operations on {@link DeviceToken}. */
public interface DeviceTokenCommandService {

  /** Registers the token, or refreshes it (and hands it to the caller) when it already exists. */
  DeviceToken handle(RegisterDeviceTokenCommand command);

  /** Removes the token when it belongs to the caller. Idempotent. */
  void handle(UnregisterDeviceTokenCommand command);
}
