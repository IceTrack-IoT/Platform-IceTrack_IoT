package pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Error body returned when an address matches no place or more than one, carrying the candidates.
 *
 * @param code       always {@code ADDRESS_NOT_VERIFIED}
 * @param message    a human-readable explanation
 * @param candidates up to five candidate addresses, empty when the provider offered none
 */
@Schema(
    name = "AddressNotVerifiedResponse",
    description = "An address that could not be verified as a single location, with up to five candidates")
public record AddressNotVerifiedResource(
    @Schema(example = "ADDRESS_NOT_VERIFIED")
    String code,

    @Schema(example = "The address could not be verified as a single location")
    String message,

    @Schema(description = "Candidate addresses to choose from, at most five")
    List<String> candidates
) {
}
