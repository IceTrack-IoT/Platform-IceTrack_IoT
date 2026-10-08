package pe.edu.upc.ice.track.platform.assets.application.internal.commandservices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.assets.application.commandservices.SiteCommandService;
import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Site;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.RegisterSiteCommand;
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
    var existingSite = siteRepository.findById(command.siteId());
    if (existingSite.isEmpty()) {
      return Result.failure(ApplicationError.notFound(SITE_RESOURCE, String.valueOf(command.siteId())));
    }
    var site = existingSite.get();
    if (!site.belongsTo(command.ownerId())) {
      // Reported the same way as a missing site, never as a conflict: telling the caller the site
      // belongs to somebody else would turn this endpoint into an oracle for discovering which
      // site identifiers are real, exactly as Equipment's commands already avoid.
      return Result.failure(ApplicationError.notFound(SITE_RESOURCE, String.valueOf(command.siteId())));
    }
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
    } catch (Exception e) {
      return Result.failure(ApplicationError.unexpected("Site update", e.getMessage()));
    }
  }
}