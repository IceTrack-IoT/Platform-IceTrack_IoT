package pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects;

import jakarta.persistence.Embeddable;

/**
 *  Value object representing the identifier of a user, as referenced by Reporting.
 * @param userId  the identifier of the user; must not be null
 */
@Embeddable
public record UserId(Long userId) {

  public UserId{
    if (userId == null){
      throw new IllegalArgumentException("UserId cannot be null");
    }
  }
}
