package pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Site;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.RegisterSiteCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.UpdateSiteInfoCommand;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.RegisterSiteResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.SiteResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.UpdateSiteResource;

/**
 * Assembler that converts site REST payloads into site commands, and a {@link Site} aggregate back
 * into a {@link SiteResource}.
 *
 * <p>The owner is a parameter rather than something read out of the payload: no site request
 * carries one, so it can only come from the identity of the caller.</p>
 */
public final class SiteResourceTransformer {

  private SiteResourceTransformer() {
  }

  /**
   * Converts a site registration payload into its command.
   *
   * @param ownerId  identifier of the authenticated owner
   * @param resource the {@link RegisterSiteResource} payload
   * @return the {@link RegisterSiteCommand} command
   */
  public static RegisterSiteCommand toRegisterCommandFromResource(Long ownerId, RegisterSiteResource resource) {
    return new RegisterSiteCommand(
        ownerId, resource.name(), resource.address(), resource.contactName(), resource.phone());
  }

  /**
   * Converts a site update payload into its command.
   *
   * @param siteId   identifier of the site being updated, taken from the request path
   * @param ownerId  identifier of the authenticated owner
   * @param resource the {@link UpdateSiteResource} payload
   * @return the {@link UpdateSiteInfoCommand} command
   */
  public static UpdateSiteInfoCommand toUpdateCommandFromResource(
      Long siteId, Long ownerId, UpdateSiteResource resource) {
    return new UpdateSiteInfoCommand(
        siteId, ownerId, resource.name(), resource.address(), resource.contactName(), resource.phone());
  }

  /**
   * Converts a {@link Site} aggregate into its outbound resource.
   *
   * @param site the aggregate to convert
   * @return the {@link SiteResource} resource
   */
  public static SiteResource toResourceFromEntity(Site site) {
    return new SiteResource(
        site.getSiteId(),
        site.getOwnerId(),
        site.getName(),
        site.getAddress().value(),
        site.getContactName(),
        site.getPhone().value());
  }
}