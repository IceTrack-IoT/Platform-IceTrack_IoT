package pe.edu.upc.ice.track.platform.assets.application.commandservices;

import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Equipment;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.ChangeStatusCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.ChangeThresholdCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.RegisterEquipmentCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.UpdateEquipmentCommand;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

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
   * that was never applied.</p>
   *
   * @param command The {@link ChangeThresholdCommand} Command
   * @return A {@link Result} containing the updated {@link Equipment} on success, or an
   *         {@link ApplicationError} when the unit is missing, belongs to another owner, or the
   *         band is inverted
   */
  Result<Equipment, ApplicationError> handle(ChangeThresholdCommand command);

  /**
   * Move a unit to a new operational status, when the transition is part of the matrix carried
   * by {@code StatusEquipment}.
   *
   * @param command The {@link ChangeStatusCommand} Command
   * @return A {@link Result} containing the updated {@link Equipment} on success, or an
   *         {@link ApplicationError} when the unit is missing, belongs to another owner, or the
   *         transition is not allowed
   */
  Result<Equipment, ApplicationError> handle(ChangeStatusCommand command);
}