package pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects;

import jakarta.persistence.Embeddable;

/**
 *  Value object representing the identifier of a site, as referenced by Reporting.
 * @param siteId  the identifier of the site; must not be null
 */
@Embeddable
public record SiteId(Long siteId) {
  public SiteId{
    if (siteId == null){
      throw new IllegalArgumentException("SiteId cannot be null");
    }
  }
}
