package pe.edu.upc.ice.track.platform.profiles.application.queryservices;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.DashboardConfig;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetDashboardConfigByIdQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetDashboardConfigByUserIdQuery;

import java.util.Optional;

/**
 * Dashboard Config Query Service
 */
public interface DashboardConfigQueryService {

  /**
   * Handle Get Dashboard Config By User Id Query.
   *
   * @param query the query
   * @return the dashboard configuration with its cards, or empty when the account has none
   */
  Optional<DashboardConfig> handle(GetDashboardConfigByUserIdQuery query);

  /**
   * Handle Get Dashboard Config By ID Query.
   *
   * @param query the query
   * @return the dashboard configuration with its cards, or empty when not found
   */
  Optional<DashboardConfig> handle(GetDashboardConfigByIdQuery query);
}
