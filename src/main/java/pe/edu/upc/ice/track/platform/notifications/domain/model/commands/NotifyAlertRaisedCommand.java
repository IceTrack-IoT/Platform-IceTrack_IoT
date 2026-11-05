package pe.edu.upc.ice.track.platform.notifications.domain.model.commands;

import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.AlertId;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationSeverity;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationType;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.EquipmentId;

import java.util.Objects;

/**
 * Command to notify the owner of a unit that an alert was raised on it.
 *
 * <p>Expressed entirely in this context's own language: the infrastructure listener that receives
 * the alert from Monitoring and Alerting has already translated its vocabulary into
 * {@link NotificationType} and {@link NotificationSeverity}, so the application layer that handles
 * this command never learns that a {@code monitoring} context exists.</p>
 *
 * @param alertId         identifier of the originating alert; required
 * @param equipmentId     unit the alert was raised on; required
 * @param type            kind of notification; required
 * @param severity        urgency of the notification; required
 * @param peakTemperature peak temperature observed, in Celsius; may be {@code null}
 */
public record NotifyAlertRaisedCommand(
    AlertId alertId,
    EquipmentId equipmentId,
    NotificationType type,
    NotificationSeverity severity,
    Double peakTemperature) {

  public NotifyAlertRaisedCommand {
    Objects.requireNonNull(alertId, "alertId must not be null");
    Objects.requireNonNull(equipmentId, "equipmentId must not be null");
    Objects.requireNonNull(type, "type must not be null");
    Objects.requireNonNull(severity, "severity must not be null");
  }
}
