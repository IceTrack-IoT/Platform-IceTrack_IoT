package pe.edu.upc.ice.track.platform.assets.application.commandservices;

import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Site;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.RegisterSiteCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.ResumeSiteGeocodingCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.RetrySiteGeocodingCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.UpdateSiteAddressCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.UpdateSiteInfoCommand;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

/**
 * Application service handling write operations on {@link Site}.
 *
 * <p>Every method returns a {@link Result} rather than throwing, so a rejected write reaches the
 * caller as a value it can render as a status code. A {@code *_NOT_FOUND} failure means the site
 * does not exist or belongs to somebody else, so a caller cannot probe for other owners' sites.</p>
 *
 * <p>Registration and address changes require a {@code validation_token} issued by
 * {@link AddressValidationCommandService}; neither calls the geocoding provider. Only a geocoding
 * retry, on a site that has no location, does.</p>
 *
 * <p>Updates accept the version the caller edited ({@code If-Match}); a stale one is refused with
 * {@code CONCURRENT_MODIFICATION_CONFLICT} before anything changes.</p>
 */
public interface SiteCommandService {

  /**
   * Registers a new site at a verified address.
   *
   * @param command the {@link RegisterSiteCommand} to handle
   * @return the registered site, or an {@link ApplicationError} when the token is missing, invalid,
   *         expired or belongs to another owner ({@code ADDRESS_VALIDATION_REQUIRED}), the pin is
   *         too far away ({@code PIN_OUT_OF_RANGE}), or a value violates a site invariant
   */
  Result<Site, ApplicationError> handle(RegisterSiteCommand command);

  /**
   * Replaces the name, contact name and contact phone of an existing site.
   *
   * @param command the {@link UpdateSiteInfoCommand} to handle
   * @return the updated site, or an {@link ApplicationError} when the site is not reachable by the
   *         caller or a value violates a site invariant
   */
  Result<Site, ApplicationError> handle(UpdateSiteInfoCommand command);

  /**
   * Moves an existing site to an address verified beforehand, keeping its equipment.
   *
   * @param command the {@link UpdateSiteAddressCommand} to handle
   * @return the updated site, or an {@link ApplicationError} when the site is not reachable by the
   *         caller, the token is not valid for the caller, or the pin is out of range
   */
  Result<Site, ApplicationError> handle(UpdateSiteAddressCommand command);

  /**
   * Locates one of the caller's unlocated sites again from its current address.
   *
   * <p>The lookup runs synchronously. When it finishes, the site is answered {@code RESOLVED}, or
   * {@code NOT_FOUND} with its failure reason. When the maps service cannot answer right away, the
   * site is answered {@code PENDING} and the lookup continues in the background.</p>
   *
   * @param command the {@link RetrySiteGeocodingCommand} to handle
   * @return the site after the attempt, or an {@link ApplicationError}: {@code SITE_NOT_FOUND} when
   *         the site is not reachable by the caller, {@code INVALID_STATUS_TRANSITION} when it is
   *         already {@code RESOLVED}, {@code TOO_MANY_REQUESTS} when the caller used up the shared
   *         address validation quota, or {@code GEOCODING_UNAVAILABLE} when the maps service refused
   *         the request (the site is then left {@code FAILED})
   */
  Result<Site, ApplicationError> handle(RetrySiteGeocodingCommand command);

  /**
   * Continues locating a site left {@code PENDING}; called in the background, with no caller.
   *
   * @param command the {@link ResumeSiteGeocodingCommand} to handle
   * @return the site after the attempt, still {@code PENDING} when another attempt should follow, or
   *         {@code SITE_NOT_FOUND} when the site no longer exists
   */
  Result<Site, ApplicationError> handle(ResumeSiteGeocodingCommand command);
}
