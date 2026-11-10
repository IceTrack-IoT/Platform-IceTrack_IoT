package pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.addressvalidation;

import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.VerifiedAddress;

import java.util.Optional;

/**
 * Outbound port issuing and checking the token that carries a verified address from the validation
 * endpoint to site registration.
 *
 * <p>Implemented by {@code AddressValidationTokenService}, which signs with a key of its own and
 * never with the key of the user authentication tokens.</p>
 */
public interface AddressValidationTokenPort {

  /**
   * Issues a token for a verified address.
   *
   * @param ownerId the owner the token is issued to
   * @param address the verified address the token carries
   * @return the signed token
   */
  String issue(Long ownerId, VerifiedAddress address);

  /**
   * Checks a token and returns the address it carries.
   *
   * @param token   the token received from the client; may be {@code null}
   * @param ownerId the owner presenting the token
   * @return the verified address, or empty when the token is missing, forged, expired or was issued
   *         to another owner
   */
  Optional<VerifiedAddress> verify(String token, Long ownerId);
}
