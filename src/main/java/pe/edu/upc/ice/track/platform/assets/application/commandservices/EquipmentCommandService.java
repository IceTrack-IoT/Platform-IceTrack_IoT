package pe.edu.upc.ice.track.platform.assets.application.commandservices;

import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Equipment;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.BumpConfigVersionCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.ChangeStatusCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.ChangeThresholdCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.EnterMaintenanceCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.LeaveMaintenanceCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.MarkEquipmentOfflineCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.RecordEquipmentReadingCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.RegisterEquipmentCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.UpdateEquipmentCommand;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

import java.util.Optional;

/**
 * Application service handling write operations on {@link Equipment}.
 *
 * <p>Ownership of a unit is reached through the site it is installed at, so every command here
 * first resolves the site and confirms it belongs to the command's owner. That is what keeps the
 * rule "a unit belongs to exactly one owner" in one place: a unit can never be created at, or
 * moved through, a site belonging to somebody else.</p>
 */
public interface EquipmentCommandService {

  /**
   * Register a new refrigeration unit at an existing site.
   *
   * @param command The {@link RegisterEquipmentCommand} Command
   * @return A {@link Result} containing the registered {@link Equipment} on success, or an
   *         {@link ApplicationError} when the site does not exist, is not the caller's, the
   *         {@code uid} is already taken, or the threshold and maintenance interval are invalid
   */
  Result<Equipment, ApplicationError> handle(RegisterEquipmentCommand command);

  /**
   * Replace the descriptive data and maintenance interval of an existing unit.
   *
   * @param command The {@link UpdateEquipmentCommand} Command
   * @return A {@link Result} containing the updated {@link Equipment} on success, or an
   *         {@link ApplicationError} when the unit is missing, belongs to another owner, or a
   *         value violates a unit invariant
   */
  Result<Equipment, ApplicationError> handle(UpdateEquipmentCommand command);

  /**
   * Replace the acceptable temperature band of a unit.
   *
   * <p>A band whose minimum is not strictly below its maximum is rejected before the aggregate
   * is touched, so no {@code TemperatureThresholdUpdatedEvent} is registered for a threshold
   * that was never applied. An accepted band increments the unit's edge configuration version.</p>
   *
   * @param command The {@link ChangeThresholdCommand} Command
   * @return A {@link Result} containing the updated {@link Equipment}, carrying its incremented
   *         configuration version, on success, or an {@link ApplicationError} when the unit is
   *         missing, belongs to another owner, or the band is inverted
   */
  Result<Equipment, ApplicationError> handle(ChangeThresholdCommand command);

  /**
   * Increment the edge configuration version of a unit, because its alert policy or its
   * technician grants changed.
   *
   * @param command The {@link BumpConfigVersionCommand} Command
   * @return the new version, or empty when the unit does not exist
   */
  Optional<Integer> handle(BumpConfigVersionCommand command);

  /**
   * Move a unit to {@code MAINTENANCE} because a maintenance window opened on it, remembering the
   * status it held. A unit that does not exist, is not {@code ACTIVE}, or is already in
   * {@code MAINTENANCE} is left unchanged.
   *
   * @param command The {@link EnterMaintenanceCommand} Command
   * @return {@code true} when the unit entered maintenance
   */
  boolean handle(EnterMaintenanceCommand command);

  /**
   * Return a unit to the status it held before a maintenance window moved it to {@code MAINTENANCE}.
   * A unit that does not exist, or that no window holds in maintenance, is left unchanged.
   *
   * @param command The {@link LeaveMaintenanceCommand} Command
   * @return {@code true} when the unit left maintenance
   */
  boolean handle(LeaveMaintenanceCommand command);

  /**
   * Record that a reading batch of a unit was stored: the unit goes online and, unless the batch is
   * older than the latest one recorded, its last reading instant and temperature move to the batch's.
   * A unit that does not exist is left alone.
   *
   * @param command The {@link RecordEquipmentReadingCommand} Command
   * @return {@code true} when the unit exists and was updated
   */
  boolean handle(RecordEquipmentReadingCommand command);

  /**
   * Record that a unit stopped reporting. A unit that does not exist or is already offline is left
   * unchanged.
   *
   * @param command The {@link MarkEquipmentOfflineCommand} Command
   * @return {@code true} when the unit was online and is now offline
   */
  boolean handle(MarkEquipmentOfflineCommand command);

  /**
   * Move a unit to a new operational status, when the transition is part of the matrix carried
   * by {@code StatusEquipment}.
   *
   * <p>The only command open to a caller acting as a technician, for whom the unit is not scoped
   * by ownership.</p>
   *
   * @param command The {@link ChangeStatusCommand} Command
   * @return A {@link Result} containing the updated {@link Equipment} on success, or an
   *         {@link ApplicationError} when the unit is missing, belongs to another owner (for an
   *         owner), or the transition is not allowed
   */
  Result<Equipment, ApplicationError> handle(ChangeStatusCommand command);
}