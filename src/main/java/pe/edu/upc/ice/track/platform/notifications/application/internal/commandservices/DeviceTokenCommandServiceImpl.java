package pe.edu.upc.ice.track.platform.notifications.application.internal.commandservices;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.notifications.application.commandservices.DeviceTokenCommandService;
import pe.edu.upc.ice.track.platform.notifications.domain.model.aggregates.DeviceToken;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.RegisterDeviceTokenCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.UnregisterDeviceTokenCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.repositories.DeviceTokenRepository;

import java.util.List;

@Service
public class DeviceTokenCommandServiceImpl implements DeviceTokenCommandService {

  private final DeviceTokenRepository deviceTokenRepository;

  public DeviceTokenCommandServiceImpl(DeviceTokenRepository deviceTokenRepository) {
    this.deviceTokenRepository = deviceTokenRepository;
  }

  @Override
  public DeviceToken handle(RegisterDeviceTokenCommand command) {
    var deviceToken = deviceTokenRepository.findByToken(command.token())
        .map(existing -> {
          existing.reassignTo(command.recipientUserId(), command.platform());
          return existing;
        })
        .orElseGet(() -> new DeviceToken(command.recipientUserId(), command.token(), command.platform()));
    return deviceTokenRepository.save(deviceToken);
  }

  @Override
  public void handle(UnregisterDeviceTokenCommand command) {
    deviceTokenRepository.findByToken(command.token())
        .filter(deviceToken -> deviceToken.belongsTo(command.recipientUserId()))
        .ifPresent(deviceToken -> deviceTokenRepository.deleteByTokenIn(List.of(deviceToken.getToken())));
  }
}
