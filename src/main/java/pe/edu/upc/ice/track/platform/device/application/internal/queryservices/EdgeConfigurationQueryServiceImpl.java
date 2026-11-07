package pe.edu.upc.ice.track.platform.device.application.internal.queryservices;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.assetmanagement.ExternalAssetManagementServiceForDevice;
import pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.monitoring.ExternalMonitoringServiceForDevice;
import pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.servicerequests.ExternalServiceRequestServiceForDevice;
import pe.edu.upc.ice.track.platform.device.application.queryservices.EdgeConfigurationQueryService;
import pe.edu.upc.ice.track.platform.device.application.results.EdgeConfiguration;
import pe.edu.upc.ice.track.platform.device.domain.model.queries.GetEdgeConfigurationQuery;

import java.util.Optional;

/**
 * Default implementation of {@link EdgeConfigurationQueryService}.
 *
 * <p>The version is read <em>before</em> the configuration it numbers. Every change to a
 * configuration input increments the version in the same transaction, so reading in this order
 * can only pair newer content with an older version - which makes the gateway download the
 * configuration once more on its next poll - and never older content with a newer version, which
 * the gateway would then keep, because it would answer every later poll with 304.</p>
 */
@Service
public class EdgeConfigurationQueryServiceImpl implements EdgeConfigurationQueryService {

  private final ExternalAssetManagementServiceForDevice externalAssetManagementService;
  private final ExternalMonitoringServiceForDevice externalMonitoringService;
  private final ExternalServiceRequestServiceForDevice externalServiceRequestService;

  public EdgeConfigurationQueryServiceImpl(
      ExternalAssetManagementServiceForDevice externalAssetManagementService,
      ExternalMonitoringServiceForDevice externalMonitoringService,
      ExternalServiceRequestServiceForDevice externalServiceRequestService) {
    this.externalAssetManagementService = externalAssetManagementService;
    this.externalMonitoringService = externalMonitoringService;
    this.externalServiceRequestService = externalServiceRequestService;
  }

  // inherited javadoc
  @Override
  public Optional<EdgeConfiguration> handle(GetEdgeConfigurationQuery query) {
    var equipmentId = query.equipmentId();
    var configVersion = externalAssetManagementService.fetchConfigVersion(equipmentId);
    if (configVersion.isEmpty()) {
      return Optional.empty();
    }
    var threshold = externalAssetManagementService.fetchThresholdRange(equipmentId);
    if (threshold.isEmpty()) {
      return Optional.empty();
    }
    var policy = externalMonitoringService.fetchApplicableAlertPolicy(equipmentId);
    var grants = externalServiceRequestService.fetchActiveEdgeGrants(equipmentId);
    return Optional.of(new EdgeConfiguration(
        configVersion.get(),
        threshold.get()[0],
        threshold.get()[1],
        policy.sustainedExcursionMinutes(),
        policy.hysteresisMarginCelsius(),
        policy.missedSyncWindowsForOffline(),
        grants));
  }
}
