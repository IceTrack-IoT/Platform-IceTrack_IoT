package pe.edu.upc.ice.track.platform.assets.application.queryservices;

import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Site;
import pe.edu.upc.ice.track.platform.assets.domain.model.queries.GetSiteByIdQuery;
import pe.edu.upc.ice.track.platform.assets.domain.model.queries.GetSitesByOwnerQuery;

import java.util.List;

/**
 * Application service handling read operations on {@link Site}.
 *
 * <p>Every query is owner scoped for a caller acting as an owner, so it can never widen its own
 * visibility by changing a path variable. Only fetching a single site is open to a technician, and
 * then without that scope.</p>
 */
public interface SiteQueryService {

  /**
   * List every site belonging to an owner.
   *
   * @param query The {@link GetSitesByOwnerQuery} Query
   * @return the sites of that owner, empty when it has none
   */
  List<Site> handle(GetSitesByOwnerQuery query);

  /**
   * Fetch a single site the caller owns, or any site for a technician.
   *
   * @param query The {@link GetSiteByIdQuery} Query
   * @return the site, or empty when it does not exist or, for an owner, belongs to another owner
   */
  java.util.Optional<Site> handle(GetSiteByIdQuery query);
}