package pe.edu.upc.ice.track.platform.assets.application.internal.queryservices;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.acl.ExternalOwnerService;
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
 * requesting owner may not see. Every query names its caller by username, which is resolved to an
 * owner through the {@link ExternalOwnerService} ACL; a caller that holds no owner profile sees no
 * site at all.</p>
 */
@Service
public class SiteQueryServiceImpl implements SiteQueryService {

  private final SiteRepository siteRepository;
  private final ExternalOwnerService externalOwnerService;

  /**
   * Creates the query service with its dependencies.
   *
   * @param siteRepository       site repository port
   * @param externalOwnerService outbound ACL service, used to resolve the caller to its owner
   */
  public SiteQueryServiceImpl(SiteRepository siteRepository, ExternalOwnerService externalOwnerService) {
    this.siteRepository = siteRepository;
    this.externalOwnerService = externalOwnerService;
  }

  // inherited javadoc
  @Override
  @Transactional(readOnly = true)
  public List<Site> handle(GetSitesByOwnerQuery query) {
    return externalOwnerService.fetchOwnerIdByUsername(query.username())
        .map(siteRepository::findByOwnerId)
        .orElse(List.of());
  }

  /**
   * Fetches a site only when it belongs to the owner the query's caller resolves to.
   *
   * @param query the query naming the site and the requesting caller
   * @return the site, or empty when it does not exist, is owned by somebody else, or the caller
   *         holds no owner profile
   */
  @Override
  @Transactional(readOnly = true)
  public Optional<Site> handle(GetSiteByIdQuery query) {
    return externalOwnerService.fetchOwnerIdByUsername(query.username())
        .flatMap(ownerId -> siteRepository.findById(query.siteId())
            .filter(site -> site.belongsTo(ownerId)));
  }
}
