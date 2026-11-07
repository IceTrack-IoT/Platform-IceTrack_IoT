package pe.edu.upc.ice.track.platform.monitoring.application.internal.commandservices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.monitoring.application.commandservices.AlertCommandService;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.accesspolicies.EquipmentAccessPolicy;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.profiles.ExternalProfilesServiceForMonitoring;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.Alert;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.AcknowledgeAlertCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.DismissAlertCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.ResolveAlertCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertRepository;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

import java.util.function.BiConsumer;

/**
 * Default implementation of {@link AlertCommandService}.
 *
 * <p>Every lifecycle transition is owner-only: an alert is loaded, then kept only when the
 * caller owns the unit it was raised on, as decided by {@link EquipmentAccessPolicy}. An alert of
 * another tenant is answered exactly like a missing one, so alert identifiers cannot be
 * harvested.</p>
 *
 * <p>The actor of every transition is the IAM identifier of the authenticated caller, resolved
 * through {@link ExternalProfilesServiceForMonitoring}; it is never taken from the request. A
 * transition the alert's status forbids, such as acknowledging a RESOLVED alert, is answered with
 * {@code INVALID_STATUS_TRANSITION} rather than surfacing the aggregate's exception.</p>
 */
@Service
@Slf4j
public class AlertCommandServiceImpl implements AlertCommandService {

  private static final String ALERT_RESOURCE = "Alert";

  private final AlertRepository alertRepository;
  private final EquipmentAccessPolicy equipmentAccessPolicy;
  private final ExternalProfilesServiceForMonitoring externalProfilesService;

  public AlertCommandServiceImpl(
      AlertRepository alertRepository,
      EquipmentAccessPolicy equipmentAccessPolicy,
      ExternalProfilesServiceForMonitoring externalProfilesService) {
    this.alertRepository = alertRepository;
    this.equipmentAccessPolicy = equipmentAccessPolicy;
    this.externalProfilesService = externalProfilesService;
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<Alert, ApplicationError> handle(AcknowledgeAlertCommand command) {
    return transition(command.alertId(), command.username(), Alert::acknowledge);
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<Alert, ApplicationError> handle(ResolveAlertCommand command) {
    return transition(command.alertId(), command.username(), Alert::resolve);
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<Alert, ApplicationError> handle(DismissAlertCommand command) {
    return transition(command.alertId(), command.username(),
        (alert, actorUserId) -> alert.dismiss(actorUserId, command.reason(), command.evidence()));
  }

  /**
   * Applies a lifecycle transition to one of the caller's alerts, recording the caller as actor.
   *
   * @param alertId  identifier of the alert
   * @param username username of the authenticated caller
   * @param change   the transition, receiving the alert and the caller's IAM identifier
   * @return the persisted alert, or the reason the transition was rejected
   */
  private Result<Alert, ApplicationError> transition(Long alertId, String username, BiConsumer<Alert, Long> change) {
    var alert = alertId == null ? null : alertRepository.findById(alertId)
        .filter(candidate -> equipmentAccessPolicy.canManage(candidate.getEquipmentId(), username))
        .orElse(null);
    var actorUserId = externalProfilesService.fetchUserIdByUsername(username);
    if (alert == null || actorUserId.isEmpty()) {
      return Result.failure(ApplicationError.notFound(ALERT_RESOURCE, String.valueOf(alertId)));
    }
    try {
      change.accept(alert, actorUserId.get());
    } catch (IllegalStateException exception) {
      return Result.failure(new ApplicationError(
          "INVALID_STATUS_TRANSITION", "Invalid alert status transition", exception.getMessage()));
    } catch (IllegalArgumentException exception) {
      return Result.failure(ApplicationError.validationError("alert", exception.getMessage()));
    }
    var saved = alertRepository.save(alert);
    log.info("Alert {} moved to {} by user {}", saved.getId(), saved.getStatus(), actorUserId.get());
    return Result.success(saved);
  }
}
