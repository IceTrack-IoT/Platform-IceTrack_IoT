package pe.edu.upc.ice.track.platform.service.request.application.internal.outboundservices.acl;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.acl.MonitoringContextFacade;

import java.util.Optional;

/**
 * Outbound service through which the {@code service.request} bounded context resolves the alert a
 * corrective request is opened for.
 *
 * <p>This is the outbound half of the Anti-Corruption Layer between {@code service.request} and
 * Monitoring and Alerting, and the only class in {@code service.request} that knows that context
 * exists. It talks to it exclusively through {@link MonitoringContextFacade}, whose signature is made
 * of primitives; the facade answers {@code 0L} for "not found", which is turned into
 * {@link Optional#empty()} here so that sentinel never leaks into this context.</p>
 */
@Service
public class ExternalMonitoringService {

  private static final long NO_ALERT = 0L;

  private final MonitoringContextFacade monitoringContextFacade;

  /**
   * Creates the outbound service.
   *
   * @param monitoringContextFacade the inbound ACL facade of the Monitoring and Alerting context
   */
  public ExternalMonitoringService(MonitoringContextFacade monitoringContextFacade) {
    this.monitoringContextFacade = monitoringContextFacade;
  }

  /**
   * Resolves the unit an alert was raised on, whatever its status.
   *
   * @param alertId identifier of the alert
   * @return the unit identifier, or empty when no alert carries that identifier
   */
  public Optional<Long> fetchAlertEquipmentId(Long alertId) {
    if (alertId == null) {
      return Optional.empty();
    }
    var equipmentId = monitoringContextFacade.fetchAlertEquipmentId(alertId);
    return equipmentId == null || equipmentId == NO_ALERT ? Optional.empty() : Optional.of(equipmentId);
  }
}
