package pe.edu.upc.ice.track.platform.assets.application.internal.commandservices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.assets.application.commandservices.SiteCommandService;
import pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.acl.AssetsExternalOwnerService;
import pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.addressvalidation.AddressValidationTokenPort;
import pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.addressvalidation.ValidationRateLimiterPort;
import pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.geocoding.GeocodingPort;
import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Site;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.RegisterSiteCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.ResumeSiteGeocodingCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.RetrySiteGeocodingCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.UpdateSiteAddressCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.UpdateSiteInfoCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.policies.CoveragePolicy;
import pe.edu.upc.ice.track.platform.assets.domain.model.policies.PinDistancePolicy;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.Address;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.GeoLocation;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.GeocodingFailureReason;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.GeocodingStatus;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.SiteContactPhone;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.VerifiedAddress;
import pe.edu.upc.ice.track.platform.assets.domain.repositories.SiteRepository;
import pe.edu.upc.ice.track.platform.shared.application.concurrency.VersionPrecondition;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

/**
 * Site Command Service Implementation.
 *
 * <p>Every command names its caller by username, never by owner: the owner is resolved here through
 * the {@link AssetsExternalOwnerService} ACL. An account with no owner profile is answered with
 * {@code ACCESS_DENIED}.</p>
 *
 * <p>An address enters a site only through a validation token issued for the same owner. The token
 * is checked here, then the optional pin is checked against the verified point (at most 150 meters
 * away). The site is saved as {@code RESOLVED} with those coordinates; the geocoding provider is not
 * queried. The provider is only queried to locate again a site that has no location, and such a retry
 * is charged to the same per-caller quota as address validation.</p>
 *
 * <p>An update carrying the version the caller edited is refused with
 * {@code CONCURRENT_MODIFICATION_CONFLICT} when the site has moved past it.</p>
 *
 * <p>An {@link OptimisticLockingFailureException} is rethrown rather than turned into a result, so
 * the global handler answers 409 {@code CONCURRENT_MODIFICATION_CONFLICT}.</p>
 */
@Service
@Slf4j
public class SiteCommandServiceImpl implements SiteCommandService {

  private static final String SITE_RESOURCE = "Site";

  private final SiteRepository siteRepository;
  private final AssetsExternalOwnerService assetsExternalOwnerService;
  private final AddressValidationTokenPort addressValidationTokenPort;
  private final GeocodingPort geocodingPort;
  private final ValidationRateLimiterPort validationRateLimiter;

  /**
   * Constructor
   *
   * @param siteRepository             the {@link SiteRepository} instance
   * @param assetsExternalOwnerService the owner resolution ACL
   * @param addressValidationTokenPort the port that checks validation tokens
   * @param geocodingPort              the port that locates an address, used by geocoding retries
   * @param validationRateLimiter      the per-caller quota shared with address validation
   */
  public SiteCommandServiceImpl(
      SiteRepository siteRepository,
      AssetsExternalOwnerService assetsExternalOwnerService,
      AddressValidationTokenPort addressValidationTokenPort,
      GeocodingPort geocodingPort,
      ValidationRateLimiterPort validationRateLimiter) {
    this.siteRepository = siteRepository;
    this.assetsExternalOwnerService = assetsExternalOwnerService;
    this.addressValidationTokenPort = addressValidationTokenPort;
    this.geocodingPort = geocodingPort;
    this.validationRateLimiter = validationRateLimiter;
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<Site, ApplicationError> handle(RegisterSiteCommand command) {
    return resolveOwnerId(command.username()).flatMap(ownerId ->
        verifiedAddress(command.validationToken(), ownerId).flatMap(verified ->
            pinnedLocation(verified, command.pinLatitude(), command.pinLongitude()).flatMap(location -> {
              try {
                var site = Site.registerVerified(
                    ownerId,
                    command.name(),
                    new Address(verified.formattedAddress()),
                    command.contactName(),
                    new SiteContactPhone(command.phone()),
                    location);
                var savedSite = siteRepository.save(site);
                log.info("Registered site {} for owner {} at a verified address", savedSite.getSiteId(), ownerId);
                return Result.success(savedSite);
              } catch (IllegalArgumentException e) {
                return Result.failure(ApplicationError.validationError(SITE_RESOURCE, e.getMessage()));
              } catch (Exception e) {
                return Result.failure(ApplicationError.unexpected("Site registration", e.getMessage()));
              }
            })));
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<Site, ApplicationError> handle(UpdateSiteInfoCommand command) {
    return reachableSite(command.siteId(), command.username()).flatMap(site -> {
      var stale = VersionPrecondition.check(command.expectedVersion(), site.getVersion());
      if (stale.isPresent()) {
        return Result.failure(stale.get());
      }
      try {
        site.updateInfo(command.name(), command.contactName(), new SiteContactPhone(command.phone()));
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
    return reachableSite(command.siteId(), command.username())
        .flatMap(site -> VersionPrecondition.check(command.expectedVersion(), site.getVersion())
            .<Result<Site, ApplicationError>>map(Result::failure)
            .orElseGet(() -> Result.success(site)))
        .flatMap(site ->
        verifiedAddress(command.validationToken(), site.getOwnerId()).flatMap(verified ->
            pinnedLocation(verified, command.pinLatitude(), command.pinLongitude()).flatMap(location -> {
              try {
                site.changeVerifiedAddress(new Address(verified.formattedAddress()), location);
                var savedSite = siteRepository.save(site);
                log.info("Moved site {} to a verified address", savedSite.getSiteId());
                return Result.success(savedSite);
              } catch (IllegalArgumentException e) {
                return Result.failure(ApplicationError.validationError(SITE_RESOURCE, e.getMessage()));
              } catch (OptimisticLockingFailureException e) {
                throw e;
              } catch (Exception e) {
                return Result.failure(ApplicationError.unexpected("Site address update", e.getMessage()));
              }
            })));
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<Site, ApplicationError> handle(RetrySiteGeocodingCommand command) {
    return reachableSite(command.siteId(), command.username()).flatMap(site -> {
      if (!site.canRetryGeocoding()) {
        return Result.failure(new ApplicationError(
            "INVALID_STATUS_TRANSITION",
            "The site is already located",
            "Only a site whose geocoding_status is PENDING, FAILED or NOT_FOUND can be located again"));
      }
      if (!validationRateLimiter.tryAcquire(command.username())) {
        return Result.failure(ApplicationError.tooManyRequests(
            "More than 20 address validations or geocoding retries within one minute; try again shortly"));
      }
      var outcome = siteRepository.save(applyLookup(site, false));
      log.info("Geocoding retry of site {} ended {}", outcome.getSiteId(), outcome.getGeocodingStatus());
      if (outcome.getGeocodingStatus() == GeocodingStatus.FAILED
          && outcome.getGeocodingFailureReason() == GeocodingFailureReason.PROVIDER_UNAVAILABLE) {
        return Result.failure(new ApplicationError(
            "GEOCODING_UNAVAILABLE",
            "The maps service is unavailable",
            "PERMANENT_FAILURE"));
      }
      return Result.success(outcome);
    });
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<Site, ApplicationError> handle(ResumeSiteGeocodingCommand command) {
    var site = siteRepository.findById(command.siteId());
    if (site.isEmpty()) {
      return Result.failure(ApplicationError.notFound(SITE_RESOURCE, String.valueOf(command.siteId())));
    }
    if (site.get().getGeocodingStatus() != GeocodingStatus.PENDING) {
      // Located or re-addressed meanwhile: nothing is left to do.
      return Result.success(site.get());
    }
    var outcome = siteRepository.save(applyLookup(site.get(), command.finalAttempt()));
    log.info("Background geocoding of site {} ended {}", outcome.getSiteId(), outcome.getGeocodingStatus());
    return Result.success(outcome);
  }

  /**
   * Looks the site's current address up and records the outcome on the site.
   *
   * <ul>
   *   <li>A single match inside the service area locates the site.</li>
   *   <li>A match outside it leaves the site {@code NOT_FOUND} with {@code OUT_OF_COVERAGE}.</li>
   *   <li>No match, or an ambiguous one, leaves it {@code NOT_FOUND} with {@code ADDRESS_NOT_FOUND}.</li>
   *   <li>A provider that refused the request leaves it {@code FAILED} with
   *       {@code PROVIDER_UNAVAILABLE}.</li>
   *   <li>A provider that could not answer right away leaves it {@code PENDING}, raising the event
   *       that continues in the background, unless this is the final attempt, which leaves it
   *       {@code FAILED} instead.</li>
   * </ul>
   *
   * @param site         the site to locate
   * @param finalAttempt whether no attempt follows this one
   * @return the same site, changed
   */
  private Site applyLookup(Site site, boolean finalAttempt) {
    var result = geocodingPort.geocode(site.getAddress().value(), null);
    switch (result.outcome()) {
      case RESOLVED -> {
        var location = result.location();
        if (CoveragePolicy.areaOf(location.latitude(), location.longitude()).isPresent()) {
          site.recordGeocodingResolved(location);
        } else {
          site.recordGeocodingFailure(GeocodingStatus.NOT_FOUND, GeocodingFailureReason.OUT_OF_COVERAGE);
        }
      }
      case NOT_FOUND -> site.recordGeocodingFailure(GeocodingStatus.NOT_FOUND, GeocodingFailureReason.ADDRESS_NOT_FOUND);
      case PERMANENT_FAILURE -> site.recordGeocodingFailure(GeocodingStatus.FAILED, GeocodingFailureReason.PROVIDER_UNAVAILABLE);
      case TRANSIENT_FAILURE -> {
        if (finalAttempt) {
          site.recordGeocodingFailure(GeocodingStatus.FAILED, GeocodingFailureReason.PROVIDER_UNAVAILABLE);
        } else {
          site.markGeocodingPending();
        }
      }
    }
    return site;
  }

  /**
   * Resolves the caller's username to the owner it acts as.
   *
   * @param username username of the requesting caller
   * @return the owner identifier, or a 403 failure when the caller holds no owner profile
   */
  private Result<Long, ApplicationError> resolveOwnerId(String username) {
    var ownerId = assetsExternalOwnerService.fetchOwnerIdByUsername(username);
    return ownerId.<Result<Long, ApplicationError>>map(Result::success).orElseGet(() -> Result.failure(noOwnerProfile()));
  }

  /**
   * Checks a validation token for the owner and returns the address it carries.
   *
   * @param token   the token presented by the client; may be {@code null}
   * @param ownerId the owner presenting it
   * @return the verified address, or {@code ADDRESS_VALIDATION_REQUIRED} when the token is missing,
   *         forged, expired, or issued to another owner
   */
  private Result<VerifiedAddress, ApplicationError> verifiedAddress(String token, Long ownerId) {
    return addressValidationTokenPort.verify(token, ownerId)
        .<Result<VerifiedAddress, ApplicationError>>map(Result::success)
        .orElseGet(() -> Result.failure(new ApplicationError(
            "ADDRESS_VALIDATION_REQUIRED",
            "A valid address validation token is required",
            "Validate the address first and present the validation_token it returns; tokens expire after 15 minutes")));
  }

  /**
   * Chooses the coordinates to save: the owner's pin when given, otherwise the verified point.
   *
   * <p>A pin must be given with both coordinates or not at all, and must lie within
   * {@link PinDistancePolicy#MAX_METERS} of the verified point.</p>
   *
   * @param verified  the verified address
   * @param pinLat    latitude of the pin, or {@code null}
   * @param pinLng    longitude of the pin, or {@code null}
   * @return the coordinates to save, or a failure for a half-given or out-of-range pin
   */
  private static Result<GeoLocation, ApplicationError> pinnedLocation(
      VerifiedAddress verified, Double pinLat, Double pinLng) {
    if (pinLat == null && pinLng == null) {
      return Result.success(verified.location());
    }
    if (pinLat == null || pinLng == null) {
      return Result.failure(ApplicationError.validationError("pin", "latitude and longitude must be given together"));
    }
    var origin = verified.location();
    if (!PinDistancePolicy.withinRange(origin.latitude(), origin.longitude(), pinLat, pinLng)) {
      return Result.failure(new ApplicationError(
          "PIN_OUT_OF_RANGE",
          "The pin is too far from the verified address",
          "The pin must lie within %d meters of the verified location".formatted((int) PinDistancePolicy.MAX_METERS)));
    }
    try {
      return Result.success(new GeoLocation(pinLat, pinLng, verified.placeId()));
    } catch (IllegalArgumentException e) {
      return Result.failure(ApplicationError.validationError("pin", e.getMessage()));
    }
  }

  /**
   * Loads a site the caller may act on: it must exist and belong to the owner the caller's username
   * resolves to.
   *
   * <p>A site of another owner is reported the same way as a missing one.</p>
   *
   * @param siteId   the site identifier
   * @param username username of the requesting caller
   * @return the site; a 403 failure when the caller holds no owner profile; or a 404 failure
   */
  private Result<Site, ApplicationError> reachableSite(Long siteId, String username) {
    return resolveOwnerId(username).flatMap(ownerId -> {
      var site = siteRepository.findById(siteId).filter(candidate -> candidate.belongsTo(ownerId));
      return site.<Result<Site, ApplicationError>>map(Result::success)
          .orElseGet(() -> Result.failure(ApplicationError.notFound(SITE_RESOURCE, String.valueOf(siteId))));
    });
  }

  /**
   * Builds the failure reported when the caller's account holds no owner profile.
   *
   * @return a 403 failure
   */
  private static ApplicationError noOwnerProfile() {
    return ApplicationError.accessDenied(
        "The authenticated account has no owner profile, so it owns no site or equipment");
  }
}
