package pe.edu.upc.ice.track.platform.assets.application.internal.queryservices;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.assets.application.queryservices.SiteQueryService;
import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Site;
import pe.edu.upc.ice.track.platform.assets.domain.model.queries.GetSiteByIdQuery;
import pe.edu.upc.ice.track.platform.assets.domain.model.queries.GetSitesByOwnerQuery;
import pe.edu.upc.ice.track.platform.assets.domain.repositories.SiteRepository;

import java.util.List;
import java.util.Optional;

/**
 * Application service that resolves site read queries.
 *
 * <p>Read only, so it joins no transaction beyond a read-only one and never loads a site the
 * requesting owner may not see.</p>
 */
@Service
public class SiteQueryServiceImpl implements SiteQueryService {

  private final SiteRepository siteRepository;

  /**
   * Creates the query service with the site repository dependency.
   *
   * @param siteRepository site repository port
   */
  public SiteQueryServiceImpl(SiteRepository siteRepository) {
    this.siteRepository = siteRepository;
  }

  // inherited javadoc
  @Override
  @Transactional(readOnly = true)
  public List<Site> handle(GetSitesByOwnerQuery query) {
    return siteRepository.findByOwnerId(query.ownerId());
  }

  /**
   * Fetches a site only when it belongs to the owner carried by the query.
   *
   * @param query the query naming the site and the requesting owner
   * @return the site, or empty when it does not exist or is owned by somebody else
   */
  @Override
  @Transactional(readOnly = true)
  public Optional<Site> handle(GetSiteByIdQuery query) {
    return siteRepository.findById(query.siteId())
        .filter(site -> site.belongsTo(query.ownerId()));
  }
}