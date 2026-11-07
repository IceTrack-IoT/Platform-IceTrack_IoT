package pe.edu.upc.ice.track.platform.monitoring.application.internal.commandservices;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.monitoring.application.commandservices.AlertCommandService;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.accesspolicies.EquipmentAccessPolicy;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.Alert;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.AcknowledgeAlertCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.DismissAlertCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.ResolveAlertCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertRepository;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Default implementation of {@link AlertCommandService}.
 *
 * <p>Every lifecycle transition is owner-only, an alert is loaded, then kept only when
 * the caller owns the unit it was raised on, as decided by {@link EquipmentAccessPolicy}. An alert
 * of another tenant is answered exactly like a missing one, so alert identifiers cannot be
 * harvested.</p>
 */
@Service
public class AlertCommandServiceImpl implements AlertCommandService {

  private final AlertRepository alertRepository;
  private final EquipmentAccessPolicy equipmentAccessPolicy;

  public AlertCommandServiceImpl(AlertRepository alertRepository, EquipmentAccessPolicy equipmentAccessPolicy) {
    this.alertRepository = alertRepository;
    this.equipmentAccessPolicy = equipmentAccessPolicy;
  }

  /**
   * Acknowledges one of the caller's alerts.
   * @param command the command containing the alert identifier and the user acknowledging it
   * @return the updated alert, or empty when it does not exist or belongs to another owner
   */
  @Override
  @Transactional
  public Optional<Alert> handle(AcknowledgeAlertCommand command) {
    return transition(command.alertId(), command.username(), Alert::acknowledge);
  }

  /**
   * Resolves one of the caller's alerts.
   * @param command the command containing the alert identifier and the user resolving it
   * @return the updated alert, or empty when it does not exist or belongs to another owner
   */
  @Override
  @Transactional
  public Optional<Alert> handle(ResolveAlertCommand command) {
    return transition(command.alertId(), command.username(), Alert::resolve);
  }

  /**
   * Dismisses one of the caller's alerts.
   * @param command the command containing the alert identifier and the user dismissing it
   * @return the updated alert, or empty when it does not exist or belongs to another owner
   */
  @Override
  @Transactional
  public Optional<Alert> handle(DismissAlertCommand command) {
    return transition(command.alertId(), command.username(), Alert::dismiss);
  }

  private Optional<Alert> transition(Long alertId, String username, Consumer<Alert> change) {
    return alertRepository.findById(alertId)
        .filter(alert -> equipmentAccessPolicy.canManage(alert.getEquipmentId(), username))
        .map(alert -> {
          change.accept(alert);
          return alertRepository.save(alert);
        });
  }
}
