package pe.edu.upc.ice.track.platform.assets.application.internal.commandservices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.assets.application.commandservices.SiteCommandService;
import pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.acl.AssetsExternalOwnerService;
import pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.addressvalidation.AddressValidationTokenPort;
import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Site;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.RegisterSiteCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.UpdateSiteAddressCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.UpdateSiteInfoCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.policies.PinDistancePolicy;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.Address;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.GeoLocation;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.SiteContactPhone;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.VerifiedAddress;
import pe.edu.upc.ice.track.platform.assets.domain.repositories.SiteRepository;
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
 * away). The site is saved as {@code RESOLVED} with those coordinates; the geocoding provider is never
 * queried.</p>
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

  /**
   * Constructor
   *
   * @param siteRepository             the {@link SiteRepository} instance
   * @param assetsExternalOwnerService the owner resolution ACL
   * @param addressValidationTokenPort the port that checks validation tokens
   */
  public SiteCommandServiceImpl(
      SiteRepository siteRepository,
      AssetsExternalOwnerService assetsExternalOwnerService,
      AddressValidationTokenPort addressValidationTokenPort) {
    this.siteRepository = siteRepository;
    this.assetsExternalOwnerService = assetsExternalOwnerService;
    this.addressValidationTokenPort = addressValidationTokenPort;
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
    return reachableSite(command.siteId(), command.username()).flatMap(site ->
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
