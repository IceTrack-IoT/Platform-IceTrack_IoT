package pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.acl;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.acl.MonitoringContextFacade;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Outbound service through which {@code assets} reads the offline window of its units, the one rule
 * of a unit's connectivity it does not own: alert policies live in Monitoring and Alerting.
 *
 * <p>This is the only class in {@code assets} that knows Monitoring and Alerting exists. It talks to
 * it exclusively through {@link MonitoringContextFacade}, whose signature is made of primitives and
 * collections of them.</p>
 */
@Service
public class AssetsExternalMonitoringService {

  private final MonitoringContextFacade monitoringContextFacade;

  /**
   * Creates the outbound service.
   *
   * @param monitoringContextFacade the inbound ACL facade of the Monitoring and Alerting context
   */
  public AssetsExternalMonitoringService(MonitoringContextFacade monitoringContextFacade) {
    this.monitoringContextFacade = monitoringContextFacade;
  }

  /**
   * Resolves how long each unit may stay silent before it counts as offline.
   *
   * @param equipmentIds identifiers of the units
   * @return the window of every unit asked about, read in one call
   */
  public Map<Long, Duration> fetchOfflineSilence(Collection<Long> equipmentIds) {
    if (equipmentIds == null || equipmentIds.isEmpty()) {
      return Map.of();
    }
    var windows = new HashMap<Long, Duration>();
    monitoringContextFacade.fetchOfflineSilenceSeconds(equipmentIds)
        .forEach((equipmentId, seconds) -> windows.put(equipmentId, Duration.ofSeconds(seconds)));
    return Map.copyOf(windows);
  }
}
