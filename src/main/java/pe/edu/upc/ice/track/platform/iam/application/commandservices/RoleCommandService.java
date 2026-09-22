package pe.edu.upc.ice.track.platform.iam.application.commandservices;

import pe.edu.upc.ice.track.platform.iam.domain.model.commands.SeedRolesCommand;

/**
 * Application service contract for IAM role commands.
 */
public interface RoleCommandService {
  /**
   * Handles the role seeding command.
   *
   * @param command role-seeding command
   */
  void handle(SeedRolesCommand command);
}
