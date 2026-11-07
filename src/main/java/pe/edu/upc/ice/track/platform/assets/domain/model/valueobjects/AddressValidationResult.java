package pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects;

import java.util.Objects;

/**
 * The outcome of a successful address validation: the verified address and the token that proves it.
 *
 * @param verified        the verified address; required
 * @param validationToken the signed token to present when registering the site; required
 */
public record AddressValidationResult(VerifiedAddress verified, String validationToken) {

  public AddressValidationResult {
    Objects.requireNonNull(verified, "verified must not be null");
    Objects.requireNonNull(validationToken, "validationToken must not be null");
  }
}
