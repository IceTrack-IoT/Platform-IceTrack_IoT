package pe.edu.upc.ice.track.platform.assets.application.internal.queryservices;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.acl.AssetsExternalOwnerService;
import pe.edu.upc.ice.track.platform.assets.application.queryservices.SiteQueryService;
import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Site;
import pe.edu.upc.ice.track.platform.assets.domain.model.queries.GetSiteByIdQuery;
import pe.edu.upc.ice.track.platform.assets.domain.model.queries.GetSitesByOwnerQuery;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.CallerRole;
import pe.edu.upc.ice.track.platform.assets.domain.repositories.SiteRepository;

import java.util.List;
import java.util.Optional;

/**
 * Application service that resolves site read queries.
 *
 * <p>Read only, so it joins no transaction beyond a read-only one and never loads a site the
 * requesting owner may not see. Every query names its caller by username, which is resolved to an
 * owner through the {@link AssetsExternalOwnerService} ACL; a caller that holds no owner profile sees no
 * site at all. The one exception is fetching a single site as a technician, which is not scoped by
 * ownership because a technician is sent to sites of any owner.</p>
 */
@Service
public class SiteQueryServiceImpl implements SiteQueryService {

  private final SiteRepository siteRepository;
  private final AssetsExternalOwnerService assetsExternalOwnerService;

  /**
   * Creates the query service with its dependencies.
   *
   * @param siteRepository       site repository port
   * @param assetsExternalOwnerService outbound ACL service, used to resolve the caller to its owner
   */
  public SiteQueryServiceImpl(SiteRepository siteRepository, AssetsExternalOwnerService assetsExternalOwnerService) {
    this.siteRepository = siteRepository;
    this.assetsExternalOwnerService = assetsExternalOwnerService;
  }

  // inherited javadoc
  @Override
  @Transactional(readOnly = true)
  public List<Site> handle(GetSitesByOwnerQuery query) {
    return assetsExternalOwnerService.fetchOwnerIdByUsername(query.username())
        .map(siteRepository::findByOwnerId)
        .orElse(List.of());
  }

  /**
   * Fetches a site: any site for a technician, and for an owner only one it owns.
   *
   * @param query the query naming the site, the requesting caller and the role it acts in
   * @return the site, or empty when it does not exist or, for an owner, is owned by somebody else
   *         or the caller holds no owner profile
   */
  @Override
  @Transactional(readOnly = true)
  public Optional<Site> handle(GetSiteByIdQuery query) {
    if (query.callerRole() == CallerRole.TECHNICIAN) {
      return siteRepository.findById(query.siteId());
    }
    return assetsExternalOwnerService.fetchOwnerIdByUsername(query.username())
        .flatMap(ownerId -> siteRepository.findById(query.siteId())
            .filter(site -> site.belongsTo(ownerId)));
  }
}
