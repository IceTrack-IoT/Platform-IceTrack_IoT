package pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Equipment;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.ChangeStatusCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.ChangeThresholdCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.RegisterEquipmentCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.UpdateEquipmentCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.EquipmentPage;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.ChangeStatusResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.ChangeThresholdResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.EquipmentResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.EquipmentTypeResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.PagedEquipmentResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.RegisterEquipmentResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.StatusEquipmentResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.UpdateEquipmentResource;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Assembler that converts equipment REST payloads into equipment commands, and an {@link Equipment}
 * aggregate back into an {@link EquipmentResource}.
 *
 * <p>The owner is always a parameter rather than something read out of a payload: no equipment
 * request carries one, so it can only come from the identity of the caller.</p>
 */
public final class EquipmentResourceTransformer {

  private EquipmentResourceTransformer() {
  }

  /**
   * Converts an equipment registration payload into its command.
   *
   * @param ownerId  identifier of the authenticated owner
   * @param resource the {@link RegisterEquipmentResource} payload
   * @return the {@link RegisterEquipmentCommand} command
   */
  public static RegisterEquipmentCommand toRegisterCommandFromResource(
      Long ownerId, RegisterEquipmentResource resource) {
    return new RegisterEquipmentCommand(
        ownerId,
        resource.siteId(),
        resource.uid(),
        resource.name(),
        resource.equipmentType().toDomain(),
        resource.minCelsius(),
        resource.maxCelsius(),
        resource.reminderIntervalDays());
  }

  /**
   * Converts an equipment update payload into its command.
   *
   * @param equipmentId identifier of the unit being updated, taken from the request path
   * @param ownerId     identifier of the authenticated owner
   * @param resource    the {@link UpdateEquipmentResource} payload
   * @return the {@link UpdateEquipmentCommand} command
   */
  public static UpdateEquipmentCommand toUpdateCommandFromResource(
      Long equipmentId, Long ownerId, UpdateEquipmentResource resource) {
    return new UpdateEquipmentCommand(
        equipmentId,
        ownerId,
        resource.name(),
        resource.equipmentType().toDomain(),
        resource.reminderIntervalDays());
  }

  /**
   * Converts a threshold change payload into its command.
   *
   * @param equipmentId identifier of the unit, taken from the request path
   * @param ownerId     identifier of the authenticated owner
   * @param resource    the {@link ChangeThresholdResource} payload
   * @return the {@link ChangeThresholdCommand} command
   */
  public static ChangeThresholdCommand toChangeThresholdCommandFromResource(
      Long equipmentId, Long ownerId, ChangeThresholdResource resource) {
    return new ChangeThresholdCommand(
        equipmentId, ownerId, resource.minCelsius(), resource.maxCelsius());
  }

  /**
   * Converts a status change payload into its command.
   *
   * @param equipmentId identifier of the unit, taken from the request path
   * @param ownerId     identifier of the authenticated owner
   * @param resource    the {@link ChangeStatusResource} payload
   * @return the {@link ChangeStatusCommand} command
   */
  public static ChangeStatusCommand toChangeStatusCommandFromResource(
      Long equipmentId, Long ownerId, ChangeStatusResource resource) {
    return new ChangeStatusCommand(equipmentId, ownerId, resource.newStatus().toDomain());
  }

  /**
   * Converts an {@link Equipment} aggregate into its outbound resource.
   *
   * @param equipment the aggregate to convert
   * @return the {@link EquipmentResource} resource
   */
  public static EquipmentResource toResourceFromEntity(Equipment equipment) {
    var threshold = equipment.getTemperatureThreshold();
    return new EquipmentResource(
        equipment.getEquipmentId(),
        equipment.getSiteId(),
        equipment.getUid(),
        equipment.getName(),
EquipmentTypeResource.fromDomain(equipment.getEquipmentType()),
        StatusEquipmentResource.fromDomain(equipment.getStatus()),
        equipment.isOnline(),
        threshold.minCelsius(),
        threshold.maxCelsius(),
        equipment.getReminderIntervalDays(),
        toIsoString(equipment.getLastReadingAt()),
        equipment.getLastKnownTemperature());
  }

  /**
   * Converts an {@link EquipmentPage} into its outbound paged resource.
   *
   * @param page the page to convert
   * @return the {@link PagedEquipmentResource} resource
   */
  public static PagedEquipmentResource toPagedResourceFromPage(EquipmentPage page) {
    return new PagedEquipmentResource(
        page.content().stream().map(EquipmentResourceTransformer::toResourceFromEntity).toList(),
        page.page(),
        page.size(),
        page.totalElements(),
        page.totalPages());
  }

  /**
   * Renders a timestamp in the ISO-8601 local date-time form the rest of the platform expects.
   *
   * @param value the timestamp, may be {@code null}
   * @return the formatted timestamp, or {@code null} when there is none
   */
  private static String toIsoString(LocalDateTime value) {
    return value == null ? null : value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
  }
}