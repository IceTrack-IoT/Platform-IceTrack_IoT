package pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.acl;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.acl.MonitoringContextFacade;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Outbound service through which Device Management reads the alert policy an edge gateway
 * enforces, and the offline window a board's connectivity is evaluated with.
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
   * Reads how long each unit may stay silent before it counts as offline, for a whole listing at once.
   *
   * @param equipmentIds identifiers of the units; {@code null} entries are ignored
   * @return the offline window of each unit asked about
   */
  public Map<Long, Duration> fetchOfflineWindows(Collection<Long> equipmentIds) {
    if (equipmentIds == null || equipmentIds.isEmpty()) {
      return Map.of();
    }
    return monitoringContextFacade.fetchOfflineSilenceSeconds(equipmentIds).entrySet().stream()
        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> Duration.ofSeconds(entry.getValue())));
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
