package pe.edu.upc.ice.track.platform.profiles.application.internal.queryservices;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.profiles.application.queryservices.DashboardConfigQueryService;
import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.DashboardConfig;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetDashboardConfigByIdQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetDashboardConfigByUserIdQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.repositories.DashboardConfigRepository;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.util.Optional;

/**
 * Application service that resolves dashboard configuration read queries.
 */
@Service
public class DashboardConfigQueryServiceImpl implements DashboardConfigQueryService {
  private final DashboardConfigRepository dashboardConfigRepository;

  /**
   * Creates the query service with the dashboard configuration repository dependency.
   *
   * @param dashboardConfigRepository dashboard configuration repository port
   */
  public DashboardConfigQueryServiceImpl(DashboardConfigRepository dashboardConfigRepository) {
    this.dashboardConfigRepository = dashboardConfigRepository;
  }

  // inherited javadoc
  @Override
  public Optional<DashboardConfig> handle(GetDashboardConfigByUserIdQuery query) {
    return dashboardConfigRepository.findByUserId(new UserId(query.userId()));
  }

  // inherited javadoc
  @Override
  public Optional<DashboardConfig> handle(GetDashboardConfigByIdQuery query) {
    return dashboardConfigRepository.findById(query.dashboardConfigId());
  }
}
