package pe.edu.upc.ice.track.platform.assets.application.commandservices;

import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Site;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.RegisterSiteCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.UpdateSiteInfoCommand;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

/**
 * Application service handling write operations on {@link Site}.
 *
 * <p>Every method returns a {@link Result} rather than throwing, so a rejected write reaches the
 * caller as a value it can render as a status code instead of as an exception it has to
 * translate. The distinction the codes carry: a {@code *_NOT_FOUND} failure means the site does
 * not exist, a {@code SITE_CONFLICT} failure means it exists but belongs to somebody else, so a
 * caller cannot probe for the existence of other owners' sites.</p>
 */
public interface SiteCommandService {

  /**
   * Register a new site for the owner carried by the command.
   *
   * @param command The {@link RegisterSiteCommand} Command
   * @return A {@link Result} containing the registered {@link Site} on success, or an
   *         {@link ApplicationError} when a value violates a site invariant
   */
  Result<Site, ApplicationError> handle(RegisterSiteCommand command);

  /**
   * Replace the editable details of an existing site.
   *
   * <p>Loads the site, verifies it belongs to the command's owner, applies the changes through
   * its domain method and persists it.</p>
   *
   * @param command The {@link UpdateSiteInfoCommand} Command
   * @return A {@link Result} containing the updated {@link Site} on success, or an
   *         {@link ApplicationError} when the site is missing, belongs to another owner, or a
   *         value violates a site invariant
   */
  Result<Site, ApplicationError> handle(UpdateSiteInfoCommand command);
}