package pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.AddressValidationResult;
import pe.edu.upc.ice.track.platform.assets.test.fixtures.AssetsDomainFixtures;

import static org.assertj.core.api.Assertions.assertThat;

class AddressValidationResourceFromResultAssemblerTest {

  @Test
  @DisplayName("toResource(AddressValidationResult) -> flattens the verified address, coverage area and token (AAA)")
  void toResource() {
    // Arrange
    var result = new AddressValidationResult(AssetsDomainFixtures.verifiedAddress(), AssetsDomainFixtures.VALIDATION_TOKEN);

    // Act
    var resource = AddressValidationResourceFromResultAssembler.toResource(result);

    // Assert
    assertThat(resource.formattedAddress()).isEqualTo(AssetsDomainFixtures.VALID_ADDRESS);
    assertThat(resource.latitude()).isEqualTo(AssetsDomainFixtures.LIMA_LATITUDE);
    assertThat(resource.longitude()).isEqualTo(AssetsDomainFixtures.LIMA_LONGITUDE);
    assertThat(resource.placeId()).isEqualTo(AssetsDomainFixtures.VALID_PLACE_ID);
    assertThat(resource.district()).isEqualTo(AssetsDomainFixtures.VALID_DISTRICT);
    assertThat(resource.area()).isEqualTo("LIMA_METROPOLITANA");
    assertThat(resource.validationToken()).isEqualTo(AssetsDomainFixtures.VALIDATION_TOKEN);
  }
}
