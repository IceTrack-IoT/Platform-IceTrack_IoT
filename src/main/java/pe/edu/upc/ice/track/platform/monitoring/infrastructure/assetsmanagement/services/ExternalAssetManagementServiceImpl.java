package pe.edu.upc.ice.track.platform.monitoring.infrastructure.assetsmanagement.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.assetmanagement.ExternalAssetManagementService;

import java.util.Optional;

@Service
public class ExternalAssetManagementServiceImpl implements ExternalAssetManagementService {

  private final double minCelsius;
  private final double maxCelsius;

  public ExternalAssetManagementServiceImpl(
      @Value("${monitoring.threshold.default-min-celsius:-25.0}") double minCelsius,
      @Value("${monitoring.threshold.default-max-celsius:-15.0}") double maxCelsius) {
    this.minCelsius = minCelsius;
    this.maxCelsius = maxCelsius;
  }

  @Override
  public Optional<double[]> fetchTemperatureThreshold(Long equipmentId) {
    return equipmentId == null ? Optional.empty() : Optional.of(new double[]{minCelsius, maxCelsius});
  }
}