package pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.EquipmentType;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.StatusEquipment;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static pe.edu.upc.ice.track.platform.assets.test.fixtures.AssetsDomainFixtures.*;

/** Covers every {@code *CommandFromResourceAssembler} of the assets context. */
class CommandFromResourceAssemblersTest {

  @Test
  @DisplayName("RegisterEquipmentCommandFromResourceAssembler.toCommandFromResource -> maps the resource and caller (AAA)")
  void registerEquipment() {
    // Arrange
    var resource = new RegisterEquipmentResource(
        SITE_ID, "ESP32-0001", "Refrigerador 1", EquipmentTypeResource.COLD_ROOM, 2.0, 8.0, 90);

    // Act
    var command = RegisterEquipmentCommandFromResourceAssembler.toCommandFromResource(USERNAME, resource);

    // Assert
    assertThat(command.username()).isEqualTo(USERNAME);
    assertThat(command.siteId()).isEqualTo(SITE_ID);
    assertThat(command.uid()).isEqualTo("ESP32-0001");
    assertThat(command.name()).isEqualTo("Refrigerador 1");
    assertThat(command.equipmentType()).isEqualTo(EquipmentType.COLD_ROOM);
    assertThat(command.minCelsius()).isEqualTo(2.0);
    assertThat(command.maxCelsius()).isEqualTo(8.0);
    assertThat(command.reminderIntervalDays()).isEqualTo(90);
  }

  @Test
  @DisplayName("UpdateEquipmentCommandFromResourceAssembler.toCommandFromResource -> maps the resource and parses If-Match (AAA)")
  void updateEquipment() {
    // Arrange
    var resource = new UpdateEquipmentResource("Refrigerador Principal", EquipmentTypeResource.DISPLAY_CASE, 120);

    // Act
    var command = UpdateEquipmentCommandFromResourceAssembler.toCommandFromResource(
        EQUIPMENT_ID, USERNAME, resource, "\"4\"");

    // Assert
    assertThat(command.equipmentId()).isEqualTo(EQUIPMENT_ID);
    assertThat(command.username()).isEqualTo(USERNAME);
    assertThat(command.name()).isEqualTo("Refrigerador Principal");
    assertThat(command.equipmentType()).isEqualTo(EquipmentType.DISPLAY_CASE);
    assertThat(command.reminderIntervalDays()).isEqualTo(120);
    assertThat(command.expectedVersion()).isEqualTo(4L);
  }

  @Test
  @DisplayName("UpdateEquipmentCommandFromResourceAssembler.toCommandFromResource -> expected version is null without If-Match (AAA)")
  void updateEquipmentWithoutIfMatch() {
    // Arrange
    var resource = new UpdateEquipmentResource("Refrigerador Principal", EquipmentTypeResource.DISPLAY_CASE, 120);

    // Act
    var command = UpdateEquipmentCommandFromResourceAssembler.toCommandFromResource(
        EQUIPMENT_ID, USERNAME, resource, null);

    // Assert
    assertThat(command.expectedVersion()).isNull();
  }

  @Test
  @DisplayName("UpdateEquipmentCommandFromResourceAssembler.toCommandFromResource -> rejects a malformed If-Match (AAA)")
  void updateEquipmentMalformedIfMatch() {
    // Arrange
    var resource = new UpdateEquipmentResource("Refrigerador Principal", EquipmentTypeResource.DISPLAY_CASE, 120);

    // Act & Assert
    assertThrows(IllegalArgumentException.class,
        () -> UpdateEquipmentCommandFromResourceAssembler.toCommandFromResource(EQUIPMENT_ID, USERNAME, resource, "abc"));
  }

  @Test
  @DisplayName("ChangeThresholdCommandFromResourceAssembler.toCommandFromResource -> maps the bounds and parses a weak If-Match (AAA)")
  void changeThreshold() {
    // Act
    var command = ChangeThresholdCommandFromResourceAssembler.toCommandFromResource(
        EQUIPMENT_ID, USERNAME, new ChangeThresholdResource(3.0, 9.0), "W/\"2\"");

    // Assert
    assertThat(command.equipmentId()).isEqualTo(EQUIPMENT_ID);
    assertThat(command.username()).isEqualTo(USERNAME);
    assertThat(command.minCelsius()).isEqualTo(3.0);
    assertThat(command.maxCelsius()).isEqualTo(9.0);
    assertThat(command.expectedVersion()).isEqualTo(2L);
  }

  @Test
  @DisplayName("ChangeStatusCommandFromResourceAssembler.toCommandFromResource -> maps the status to the domain enum (AAA)")
  void changeStatus() {
    // Act
    var command = ChangeStatusCommandFromResourceAssembler.toCommandFromResource(
        EQUIPMENT_ID, USERNAME, new ChangeStatusResource(StatusEquipmentResource.REPAIR), "6");

    // Assert
    assertThat(command.equipmentId()).isEqualTo(EQUIPMENT_ID);
    assertThat(command.username()).isEqualTo(USERNAME);
    assertThat(command.newStatus()).isEqualTo(StatusEquipment.REPAIR);
    assertThat(command.expectedVersion()).isEqualTo(6L);
  }

  @Test
  @DisplayName("RegisterSiteCommandFromResourceAssembler.toCommandFromResource -> flattens the pin when present (AAA)")
  void registerSiteWithPin() {
    // Arrange
    var resource = new RegisterSiteResource(
        VALID_SITE_NAME, VALID_CONTACT_NAME, VALID_PHONE, VALIDATION_TOKEN, new PinResource(LIMA_LATITUDE, LIMA_LONGITUDE));

    // Act
    var command = RegisterSiteCommandFromResourceAssembler.toCommandFromResource(USERNAME, resource);

    // Assert
    assertThat(command.username()).isEqualTo(USERNAME);
    assertThat(command.name()).isEqualTo(VALID_SITE_NAME);
    assertThat(command.contactName()).isEqualTo(VALID_CONTACT_NAME);
    assertThat(command.phone()).isEqualTo(VALID_PHONE);
    assertThat(command.validationToken()).isEqualTo(VALIDATION_TOKEN);
    assertThat(command.pinLatitude()).isEqualTo(LIMA_LATITUDE);
    assertThat(command.pinLongitude()).isEqualTo(LIMA_LONGITUDE);
  }

  @Test
  @DisplayName("RegisterSiteCommandFromResourceAssembler.toCommandFromResource -> leaves the pin coordinates null without a pin (AAA)")
  void registerSiteWithoutPin() {
    // Arrange
    var resource = new RegisterSiteResource(VALID_SITE_NAME, VALID_CONTACT_NAME, VALID_PHONE, VALIDATION_TOKEN, null);

    // Act
    var command = RegisterSiteCommandFromResourceAssembler.toCommandFromResource(USERNAME, resource);

    // Assert
    assertThat(command.pinLatitude()).isNull();
    assertThat(command.pinLongitude()).isNull();
  }

  @Test
  @DisplayName("UpdateSiteInfoCommandFromResourceAssembler.toCommandFromResource -> maps the info and parses If-Match (AAA)")
  void updateSiteInfo() {
    // Arrange
    var resource = new UpdateSiteResource("Sede Norte", "Carlos Diaz", "+51 912345678");

    // Act
    var command = UpdateSiteInfoCommandFromResourceAssembler.toCommandFromResource(SITE_ID, USERNAME, resource, "\"1\"");

    // Assert
    assertThat(command.siteId()).isEqualTo(SITE_ID);
    assertThat(command.username()).isEqualTo(USERNAME);
    assertThat(command.name()).isEqualTo("Sede Norte");
    assertThat(command.contactName()).isEqualTo("Carlos Diaz");
    assertThat(command.phone()).isEqualTo("+51 912345678");
    assertThat(command.expectedVersion()).isEqualTo(1L);
  }

  @Test
  @DisplayName("UpdateSiteAddressCommandFromResourceAssembler.toCommandFromResource -> flattens the pin and parses If-Match (AAA)")
  void updateSiteAddressWithPin() {
    // Arrange
    var resource = new UpdateSiteAddressResource(VALIDATION_TOKEN, new PinResource(LIMA_LATITUDE, LIMA_LONGITUDE));

    // Act
    var command = UpdateSiteAddressCommandFromResourceAssembler.toCommandFromResource(SITE_ID, USERNAME, resource, "3");

    // Assert
    assertThat(command.siteId()).isEqualTo(SITE_ID);
    assertThat(command.username()).isEqualTo(USERNAME);
    assertThat(command.validationToken()).isEqualTo(VALIDATION_TOKEN);
    assertThat(command.pinLatitude()).isEqualTo(LIMA_LATITUDE);
    assertThat(command.pinLongitude()).isEqualTo(LIMA_LONGITUDE);
    assertThat(command.expectedVersion()).isEqualTo(3L);
  }

  @Test
  @DisplayName("UpdateSiteAddressCommandFromResourceAssembler.toCommandFromResource -> leaves the pin null without a pin (AAA)")
  void updateSiteAddressWithoutPin() {
    // Arrange
    var resource = new UpdateSiteAddressResource(VALIDATION_TOKEN, null);

    // Act
    var command = UpdateSiteAddressCommandFromResourceAssembler.toCommandFromResource(SITE_ID, USERNAME, resource, null);

    // Assert
    assertThat(command.pinLatitude()).isNull();
    assertThat(command.pinLongitude()).isNull();
    assertThat(command.expectedVersion()).isNull();
  }

  @Test
  @DisplayName("ValidateAddressCommandFromResourceAssembler.toCommandFromResource -> maps address and place id (AAA)")
  void validateAddress() {
    // Act
    var command = ValidateAddressCommandFromResourceAssembler.toCommandFromResource(
        USERNAME, new ValidateAddressResource(VALID_ADDRESS, VALID_PLACE_ID));

    // Assert
    assertThat(command.username()).isEqualTo(USERNAME);
    assertThat(command.address()).isEqualTo(VALID_ADDRESS);
    assertThat(command.placeId()).isEqualTo(VALID_PLACE_ID);
  }
}
