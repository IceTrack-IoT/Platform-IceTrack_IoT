package pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.AddressValidationResult;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.AddressValidationResource;

/**
 * Converts a successful {@link AddressValidationResult} into its REST resource.
 */
public class AddressValidationResourceFromResultAssembler {

  private AddressValidationResourceFromResultAssembler() {
  }

  /**
   * @param result the successful validation
   * @return the resource carrying the verified address and its token
   */
  public static AddressValidationResource toResource(AddressValidationResult result) {
    var verified = result.verified();
    return new AddressValidationResource(
        verified.formattedAddress(),
        verified.location().latitude(),
        verified.location().longitude(),
        verified.placeId(),
        verified.district(),
        verified.area().name(),
        result.validationToken());
  }
}
