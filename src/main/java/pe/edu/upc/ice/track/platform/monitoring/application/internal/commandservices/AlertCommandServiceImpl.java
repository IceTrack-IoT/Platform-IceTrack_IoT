package pe.edu.upc.ice.track.platform.monitoring.application.internal.commandservices;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.monitoring.application.commandservices.AlertCommandService;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.Alert;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.AcknowledgeAlertCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.DismissAlertCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.ResolveAlertCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertRepository;

import java.util.Optional;

@Service
public class AlertCommandServiceImpl implements AlertCommandService {

  private final AlertRepository alertRepository;

  public AlertCommandServiceImpl(AlertRepository alertRepository) {
    this.alertRepository = alertRepository;
  }

  /**
   *  Handles the acknowledgment of an alert by finding it in the repository, acknowledging it, and saving the updated state.
   * @param command the command containing the alert identifier and the user acknowledging it
   * @return  an Optional containing the updated alert if found, or an empty Optional if not found
   */
  @Override
  public Optional<Alert> handle(AcknowledgeAlertCommand command) {
    return alertRepository.findById(command.alertId()).map(alert -> {
      alert.acknowledge();
      return alertRepository.save(alert);
    });
  }

  /**
   *  Handles the resolution of an alert by finding it in the repository, resolving it, and saving the updated state.
   * @param command the command containing the alert identifier and the user resolving it
   * @return  an Optional containing the updated alert if found, or an empty Optional if not found
   */
  @Override
  public Optional<Alert> handle(ResolveAlertCommand command) {
    return alertRepository.findById(command.alertId()).map(alert -> {
      alert.resolve();
      return alertRepository.save(alert);
    });
  }

  /**
   * Handles the dismissal of an alert by finding it in the repository, dismissing it, and saving the updated state.
   * @param command the command containing the alert identifier and the user dismissing it
   * @return  an Optional containing the updated alert if found, or an empty Optional if not found
   */
  @Override
  public Optional<Alert> handle(DismissAlertCommand command) {
    return alertRepository.findById(command.alertId()).map(alert -> {
      alert.dismiss();
      return alertRepository.save(alert);
    });
  }
}
