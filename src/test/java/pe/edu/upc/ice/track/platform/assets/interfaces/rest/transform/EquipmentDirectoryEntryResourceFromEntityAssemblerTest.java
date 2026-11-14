package pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.StatusEquipment;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.EquipmentTypeResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.StatusEquipmentResource;
import pe.edu.upc.ice.track.platform.assets.test.fixtures.AssetsDomainFixtures;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static pe.edu.upc.ice.track.platform.assets.test.fixtures.AssetsDomainFixtures.EQUIPMENT_ID;
import static pe.edu.upc.ice.track.platform.assets.test.fixtures.AssetsDomainFixtures.OTHER_EQUIPMENT_ID;

class EquipmentDirectoryEntryResourceFromEntityAssemblerTest {

  @Test
  @DisplayName("toResourceFromEntity(Equipment) -> maps the directory attributes including the site name (AAA)")
  void toResourceFromEntity() {
    // Arrange
    var unit = AssetsDomainFixtures.equipmentBuilder().status(StatusEquipment.REPAIR).build();
    unit.attachSiteName("Sede Central");

    // Act
    var resource = EquipmentDirectoryEntryResourceFromEntityAssembler.toResourceFromEntity(unit);

    // Assert
    assertThat(resource.id()).isEqualTo(EQUIPMENT_ID);
    assertThat(resource.uid()).isEqualTo(AssetsDomainFixtures.VALID_UID);
    assertThat(resource.name()).isEqualTo(AssetsDomainFixtures.VALID_EQUIPMENT_NAME);
    assertThat(resource.equipmentType()).isEqualTo(EquipmentTypeResource.REFRIGERATOR);
    assertThat(resource.status()).isEqualTo(StatusEquipmentResource.REPAIR);
    assertThat(resource.siteId()).isEqualTo(AssetsDomainFixtures.SITE_ID);
    assertThat(resource.siteName()).isEqualTo("Sede Central");
  }

  @Test
  @DisplayName("toResourcesFromEntities(List) -> maps every unit preserving order (AAA)")
  void toResourcesFromEntities() {
    // Arrange
    var other = AssetsDomainFixtures.equipmentBuilder().id(OTHER_EQUIPMENT_ID).uid("ESP32-0002").build();

    // Act
    var resources = EquipmentDirectoryEntryResourceFromEntityAssembler.toResourcesFromEntities(
        List.of(AssetsDomainFixtures.equipment(), other));

    // Assert
    assertThat(resources).extracting(r -> r.id()).containsExactly(EQUIPMENT_ID, OTHER_EQUIPMENT_ID);
  }

  @Test
  @DisplayName("toResourcesFromEntities(List) -> returns an empty list for no units (AAA)")
  void toResourcesFromEntitiesEmpty() {
    // Act & Assert
    assertThat(EquipmentDirectoryEntryResourceFromEntityAssembler.toResourcesFromEntities(List.of())).isEmpty();
  }
}
