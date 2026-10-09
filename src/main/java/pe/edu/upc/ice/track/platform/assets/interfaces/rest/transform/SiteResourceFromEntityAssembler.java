package pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Site;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.SiteResource;

/**
 * Assembler to convert a {@link Site} aggregate to a {@link SiteResource}.
 */
public class SiteResourceFromEntityAssembler {

  /**
   * Converts a {@link Site} aggregate to a {@link SiteResource}.
   *
   * @param entity the {@link Site} aggregate to convert
   * @return the {@link SiteResource} resource
   */
  public static SiteResource toResourceFromEntity(Site entity) {
    return new SiteResource(
        entity.getSiteId(),
        entity.getOwnerId(),
        entity.getName(),
        entity.getAddress().value(),
        entity.getContactName(),
        entity.getPhone().value());
  }
}
