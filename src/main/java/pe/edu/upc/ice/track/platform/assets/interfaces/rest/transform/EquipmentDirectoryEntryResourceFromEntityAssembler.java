package pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Equipment;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.EquipmentDirectoryEntryResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.EquipmentTypeResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.StatusEquipmentResource;

import java.util.List;

/**
 * Assembler to convert {@link Equipment} aggregates into {@link EquipmentDirectoryEntryResource}s.
 *
 * <p>Only the identity of the unit is copied: no telemetry, threshold or configuration version is
 * read here, so none can make the directory look changed.</p>
 */
public final class EquipmentDirectoryEntryResourceFromEntityAssembler {

  private EquipmentDirectoryEntryResourceFromEntityAssembler() {
  }

  /**
   * Converts an {@link Equipment} aggregate to its directory entry.
   *
   * @param entity the {@link Equipment} aggregate to convert
   * @return the {@link EquipmentDirectoryEntryResource} resource
   */
  public static EquipmentDirectoryEntryResource toResourceFromEntity(Equipment entity) {
    return new EquipmentDirectoryEntryResource(
        entity.getEquipmentId(),
        entity.getUid(),
        entity.getName(),
        EquipmentTypeResource.fromDomain(entity.getEquipmentType()),
        StatusEquipmentResource.fromDomain(entity.getStatus()),
        entity.getSiteId(),
        entity.getSiteName());
  }

  /**
   * Converts a list of units to the directory, keeping its order.
   *
   * @param entities the units, already ordered
   * @return the directory entries, in the same order
   */
  public static List<EquipmentDirectoryEntryResource> toResourcesFromEntities(List<Equipment> entities) {
    return entities.stream()
        .map(EquipmentDirectoryEntryResourceFromEntityAssembler::toResourceFromEntity)
        .toList();
  }
}
