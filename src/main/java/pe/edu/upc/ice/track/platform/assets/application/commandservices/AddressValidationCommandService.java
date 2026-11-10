package pe.edu.upc.ice.track.platform.assets.application.commandservices;

import pe.edu.upc.ice.track.platform.assets.domain.model.commands.ValidateAddressCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.AddressValidationResult;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

/**
 * Application service verifying an address with the geocoding provider before a site is saved.
 *
 * <p>Nothing is persisted here. A successful verification returns the normalized address and a
 * token; the site itself is saved later, by presenting that token.</p>
 *
 * <p>Failures are reported as {@link ApplicationError} codes: {@code ACCESS_DENIED} for a caller with
 * no owner profile, {@code TOO_MANY_REQUESTS} when the caller exceeded its quota (the provider is not
 * queried), {@code ADDRESS_OUT_OF_COVERAGE}, {@code ADDRESS_NOT_VERIFIED} (whose details list the
 * candidates, one per line) and {@code GEOCODING_UNAVAILABLE}.</p>
 */
public interface AddressValidationCommandService {

  /**
   * Verifies an address for the owner carried by the command.
   *
   * @param command the {@link ValidateAddressCommand} to handle
   * @return the verified address and its token, or the reason it could not be verified
   */
  Result<AddressValidationResult, ApplicationError> handle(ValidateAddressCommand command);
}
