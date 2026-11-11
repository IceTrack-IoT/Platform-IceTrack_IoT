package pe.edu.upc.ice.track.platform.profiles.application.internal.queryservices;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.profiles.application.internal.outboundservices.acl.ExternalAssetsService;
import pe.edu.upc.ice.track.platform.profiles.application.internal.outboundservices.acl.ExternalIamService;
import pe.edu.upc.ice.track.platform.profiles.application.queryservices.SessionContextQueryService;
import pe.edu.upc.ice.track.platform.profiles.application.results.SessionContext;
import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.OwnerProfile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetSessionContextQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.repositories.DashboardConfigRepository;
import pe.edu.upc.ice.track.platform.profiles.domain.repositories.ProfileRepository;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.time.Instant;
import java.util.Optional;

/**
 * Default {@link SessionContextQueryService}.
 *
 * <p>Resolves the account through the IAM ACL, then reads its profile and dashboard configuration from
 * this context and, for an owner, counts its sites and units through the Assets ACL. A technician, or
 * an account without a profile, gets no owner profile and zero counts. The email address is the one
 * recorded on the profile, which is where this platform keeps it.</p>
 */
@Service
public class SessionContextQueryServiceImpl implements SessionContextQueryService {

  private final ExternalIamService externalIamService;
  private final ExternalAssetsService externalAssetsService;
  private final ProfileRepository profileRepository;
  private final DashboardConfigRepository dashboardConfigRepository;

  public SessionContextQueryServiceImpl(
      ExternalIamService externalIamService,
      ExternalAssetsService externalAssetsService,
      ProfileRepository profileRepository,
      DashboardConfigRepository dashboardConfigRepository) {
    this.externalIamService = externalIamService;
    this.externalAssetsService = externalAssetsService;
    this.profileRepository = profileRepository;
    this.dashboardConfigRepository = dashboardConfigRepository;
  }

  // inherited javadoc
  @Override
  @Transactional(readOnly = true)
  public Optional<SessionContext> handle(GetSessionContextQuery query) {
    return externalIamService.fetchAccountByUsername(query.username()).map(account -> {
      var userId = new UserId(account.userId());
      var profile = profileRepository.findByUserId(userId).orElse(null);
      var owner = profile instanceof OwnerProfile ownerProfile ? ownerProfile : null;
      var dashboardConfig = dashboardConfigRepository.findByUserId(userId).orElse(null);
      var siteCount = owner == null ? 0L : externalAssetsService.countSites(owner.getUserProfileId());
      var equipmentCount = owner == null ? 0L : externalAssetsService.countEquipment(owner.getUserProfileId());
      return new SessionContext(
          account.userId(),
          account.username(),
          profile == null ? null : profile.getEmail().address(),
          account.role(),
          account.provider(),
          owner,
          dashboardConfig,
          siteCount,
          equipmentCount,
          Instant.now());
    });
  }
}
