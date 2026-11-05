package pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects;

/**
 * A value object representing a Site's unique identifier in the shared kernel.
 *
 * <p>The identifier is immutable and only accepts values that can be a persisted identity: a
 * non-null, strictly positive number.</p>
 *
 * @param siteId the site identifier; required and strictly positive
 */
public record SiteId(Long siteId) {

  /**
   * Validates the identifier.
   *
   * @throws IllegalArgumentException when the identifier is missing or not positive
   */
  public SiteId {
    if (siteId == null || siteId <= 0) {
      throw new IllegalArgumentException("Site identifier must be a positive number");
    }
  }
}
