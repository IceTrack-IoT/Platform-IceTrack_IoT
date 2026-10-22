package pe.edu.upc.ice.track.platform.assets.application.internal.commandservices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.assets.application.commandservices.SiteCommandService;
import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Site;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.RegisterSiteCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.UpdateSiteAddressCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.UpdateSiteInfoCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.Address;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.Phone;
import pe.edu.upc.ice.track.platform.assets.domain.repositories.SiteRepository;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

import java.util.Optional;

/**
 * Site Command Service Implementation.
 *
 * <p>Translates raw command values into the site value objects, which are what actually enforce
 * the length and format rules, then lets the repository adapter insert the aggregate and publish
 * its creation event. The creation event is not registered here on purpose: it carries the
 * persistence identity, which only exists after the insert.</p>
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

  /**
   * Constructor
   *
   * @param siteRepository The {@link SiteRepository} instance
   */
  public SiteCommandServiceImpl(SiteRepository siteRepository) {
    this.siteRepository = siteRepository;
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<Site, ApplicationError> handle(RegisterSiteCommand command) {
    try {
      var site = new Site(
          null,
          command.ownerId(),
          command.name(),
          new Address(command.address()),
          command.contactName(),
          new Phone(command.phone()));
      var savedSite = siteRepository.save(site);
      log.info("Registered site {} for owner {}", savedSite.getSiteId(), command.ownerId());
      return Result.success(savedSite);
    } catch (IllegalArgumentException e) {
      return Result.failure(ApplicationError.validationError(SITE_RESOURCE, e.getMessage()));
    } catch (Exception e) {
      return Result.failure(ApplicationError.unexpected("Site registration", e.getMessage()));
    }
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<Site, ApplicationError> handle(UpdateSiteInfoCommand command) {
    var reachable = reachableSite(command.siteId(), command.ownerId());
    if (reachable.isEmpty()) {
      return Result.failure(notReachableSite(command.siteId()));
    }
    var site = reachable.get();
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
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<Site, ApplicationError> handle(UpdateSiteAddressCommand command) {
    var reachable = reachableSite(command.siteId(), command.ownerId());
    if (reachable.isEmpty()) {
      return Result.failure(notReachableSite(command.siteId()));
    }
    var site = reachable.get();
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
  }

  /**
   * Loads a site only when it belongs to the requesting owner.
   *
   * <p>A site of another owner is reported the same way as a missing one, never as a conflict:
   * telling the caller the site belongs to somebody else would turn these endpoints into an oracle
   * for discovering which site identifiers are real, exactly as Equipment's commands already
   * avoid.</p>
   *
   * @param siteId  the site identifier
   * @param ownerId the requesting owner
   * @return the site, or empty when it does not exist or is owned by somebody else
   */
  private Optional<Site> reachableSite(Long siteId, Long ownerId) {
    return siteRepository.findById(siteId).filter(site -> site.belongsTo(ownerId));
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
}