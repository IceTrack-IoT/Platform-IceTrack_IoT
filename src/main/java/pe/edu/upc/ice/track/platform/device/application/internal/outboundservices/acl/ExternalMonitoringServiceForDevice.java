package pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.acl;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.acl.MonitoringContextFacade;

/**
 * Outbound service through which Device Management reads the alert policy an edge gateway
 * enforces.
 *
 * <p>The only class in this context that knows about Monitoring and Alerting; it talks to it
 * exclusively through {@link MonitoringContextFacade}.</p>
 */
@Service
public class ExternalMonitoringServiceForDevice {

  private final MonitoringContextFacade monitoringContextFacade;

  public ExternalMonitoringServiceForDevice(MonitoringContextFacade monitoringContextFacade) {
    this.monitoringContextFacade = monitoringContextFacade;
  }

  /**
   * @param equipmentId identifier of the equipment
   * @return the applicable policy: the unit's own, else the platform default
   */
  public AlertPolicyValues fetchApplicableAlertPolicy(Long equipmentId) {
    var settings = monitoringContextFacade.fetchApplicableAlertPolicy(equipmentId);
    return new AlertPolicyValues(
        settings.sustainedExcursionMinutes(), settings.hysteresisMarginCelsius(), settings.missedSyncWindowsForOffline());
  }

  /**
   * The alert policy values the gateway needs, in this context's own terms.
   *
   * @param sustainedExcursionMinutes   minutes an excursion must last before it is raised
   * @param hysteresisMarginCelsius     recovery margin, in Celsius, before an excursion closes
   * @param missedSyncWindowsForOffline missed synchronization windows before a device counts as offline
   */
  public record AlertPolicyValues(int sustainedExcursionMinutes, double hysteresisMarginCelsius,
                                  int missedSyncWindowsForOffline) {
  }
}
