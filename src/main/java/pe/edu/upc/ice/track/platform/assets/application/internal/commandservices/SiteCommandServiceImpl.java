package pe.edu.upc.ice.track.platform.assets.application.internal.commandservices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.assets.application.commandservices.SiteCommandService;
import pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.acl.ExternalOwnerService;
import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Site;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.RegisterSiteCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.UpdateSiteAddressCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.UpdateSiteInfoCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.Address;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.Phone;
import pe.edu.upc.ice.track.platform.assets.domain.repositories.SiteRepository;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

/**
 * Site Command Service Implementation.
 *
 * <p>Translates raw command values into the site value objects, which are what actually enforce
 * the length and format rules, then lets the repository adapter insert the aggregate and publish
 * its creation event. The creation event is not registered here on purpose: it carries the
 * persistence identity, which only exists after the insert.</p>
 *
 * <p>Every command names its caller by username, never by owner: the owner is resolved here
 * through the {@link ExternalOwnerService} ACL, so a client cannot act as an owner it does not
 * hold. An account that holds no owner profile is answered with {@code ACCESS_DENIED}.</p>
 *
 * <p>No method here calls the geocoding provider, directly or indirectly. Registering a site or
 * changing its address only saves it as {@code PENDING} and raises a
 * {@code SiteAddressChangedEvent}, which is handled asynchronously after commit; a slow or failing
 * provider therefore cannot delay, fail or roll back any of these commands.</p>
 *
 * <p>An {@link OptimisticLockingFailureException} - the site changed after it was read, typically
 * because its geocoding was just recorded - is rethrown instead of being turned into a
 * {@link Result}. The transaction is already marked rollback-only at that point, so returning
 * normally would only surface as an {@code UnexpectedRollbackException} (a 500); rethrown, it
 * reaches {@code GlobalExceptionHandler} and is answered with 409
 * {@code CONCURRENT_MODIFICATION_CONFLICT}.</p>
 */
@Service
@Slf4j
public class SiteCommandServiceImpl implements SiteCommandService {

  private static final String SITE_RESOURCE = "Site";

  private final SiteRepository siteRepository;
  private final ExternalOwnerService externalOwnerService;

  /**
   * Constructor
   *
   * @param siteRepository       The {@link SiteRepository} instance
   * @param externalOwnerService The {@link ExternalOwnerService} instance
   */
  public SiteCommandServiceImpl(SiteRepository siteRepository, ExternalOwnerService externalOwnerService) {
    this.siteRepository = siteRepository;
    this.externalOwnerService = externalOwnerService;
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<Site, ApplicationError> handle(RegisterSiteCommand command) {
    return resolveOwnerId(command.username()).flatMap(ownerId -> {
      try {
        var site = new Site(
            null,
            ownerId,
            command.name(),
            new Address(command.address()),
            command.contactName(),
            new Phone(command.phone()));
        var savedSite = siteRepository.save(site);
        log.info("Registered site {} for owner {}", savedSite.getSiteId(), ownerId);
        return Result.success(savedSite);
      } catch (IllegalArgumentException e) {
        return Result.failure(ApplicationError.validationError(SITE_RESOURCE, e.getMessage()));
      } catch (Exception e) {
        return Result.failure(ApplicationError.unexpected("Site registration", e.getMessage()));
      }
    });
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<Site, ApplicationError> handle(UpdateSiteInfoCommand command) {
    return reachableSite(command.siteId(), command.username()).flatMap(site -> {
      try {
        site.updateInfo(
            command.name(),
            new Address(command.address()),
            command.contactName(),
            new Phone(command.phone()));
        var savedSite = siteRepository.save(site);
        log.info("Updated site {}", savedSite.getSiteId());
        return Result.success(savedSite);
      } catch (IllegalArgumentException e) {
        return Result.failure(ApplicationError.validationError(SITE_RESOURCE, e.getMessage()));
      } catch (OptimisticLockingFailureException e) {
        throw e;
      } catch (Exception e) {
        return Result.failure(ApplicationError.unexpected("Site update", e.getMessage()));
      }
    });
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<Site, ApplicationError> handle(UpdateSiteAddressCommand command) {
    return reachableSite(command.siteId(), command.username()).flatMap(site -> {
      try {
        // The geocoding lookup is not called from here: changeAddress only queues it as a domain
        // event, which is dispatched asynchronously once this transaction has committed.
        site.changeAddress(new Address(command.newAddress()));
        var savedSite = siteRepository.save(site);
        log.info("Changed address of site {}; geocoding is {}", savedSite.getSiteId(), savedSite.getGeocodingStatus());
        return Result.success(savedSite);
      } catch (IllegalArgumentException e) {
        return Result.failure(ApplicationError.validationError(SITE_RESOURCE, e.getMessage()));
      } catch (OptimisticLockingFailureException e) {
        throw e;
      } catch (Exception e) {
        return Result.failure(ApplicationError.unexpected("Site address update", e.getMessage()));
      }
    });
  }

  /**
   * Resolves the caller's username to the owner it acts as.
   *
   * @param username username of the requesting caller
   * @return the owner identifier, or a 403 failure when the caller holds no owner profile
   */
  private Result<Long, ApplicationError> resolveOwnerId(String username) {
    var ownerId = externalOwnerService.fetchOwnerIdByUsername(username);
    return ownerId.<Result<Long, ApplicationError>>map(Result::success).orElseGet(() -> Result.failure(noOwnerProfile()));
  }

  /**
   * Loads a site the caller may act on: it must exist and belong to the owner the caller's
   * username resolves to.
   *
   * <p>A site of another owner is reported the same way as a missing one, never as a conflict:
   * telling the caller the site belongs to somebody else would turn these endpoints into an oracle
   * for discovering which site identifiers are real, exactly as Equipment's commands already
   * avoid.</p>
   *
   * @param siteId   the site identifier
   * @param username username of the requesting caller
   * @return the site; a 403 failure when the caller holds no owner profile; or a 404 failure when
   *         the site does not exist or is owned by somebody else
   */
  private Result<Site, ApplicationError> reachableSite(Long siteId, String username) {
    return resolveOwnerId(username).flatMap(ownerId -> {
      var site = siteRepository.findById(siteId).filter(candidate -> candidate.belongsTo(ownerId));
      return site.<Result<Site, ApplicationError>>map(Result::success).orElseGet(() -> Result.failure(notReachableSite(siteId)));
    });
  }

  /**
   * Builds the failure reported when a site cannot be reached by the requesting owner.
   *
   * @param siteId the site identifier that was requested
   * @return a 404 failure that never reveals whether the site exists under another owner
   */
  private static ApplicationError notReachableSite(Long siteId) {
    return ApplicationError.notFound(SITE_RESOURCE, String.valueOf(siteId));
  }

  /**
   * Builds the failure reported when the caller's account holds no owner profile.
   *
   * <p>Reported as access denied rather than as a missing site: the caller is known and the
   * payload may be perfectly fine, there is simply no owner behind it.</p>
   *
   * @return a 403 failure
   */
  private static ApplicationError noOwnerProfile() {
    return ApplicationError.accessDenied(
        "The authenticated account has no owner profile, so it owns no site or equipment");
  }
}