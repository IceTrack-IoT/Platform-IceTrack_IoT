package pe.edu.upc.ice.track.platform.monitoring.application.queryservices;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetDashboardSummaryQuery;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.readmodels.DashboardSummary;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

/** Application service computing the {@link DashboardSummary} read model of an owner. */
public interface DashboardSummaryQueryService {

  /**
   * Computes the figures of the caller's dashboard, optionally narrowed to one of its sites.
   *
   * @param query the query naming the caller and the optional site
   * @return the summary; or a {@code SITE_NOT_FOUND} failure when the query names a site that does
   *         not exist or is not the caller's
   */
  Result<DashboardSummary, ApplicationError> handle(GetDashboardSummaryQuery query);
}
