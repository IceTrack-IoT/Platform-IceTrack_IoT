package pe.edu.upc.ice.track.platform.assets.application.commandservices;

import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Site;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.RegisterSiteCommand;
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
 * {@link AddressValidationCommandService}. None of these methods calls the geocoding provider.</p>
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
}
