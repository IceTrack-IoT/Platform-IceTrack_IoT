package pe.edu.upc.ice.track.platform.iam.application.internal.commandservices;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.iam.application.commandservices.RoleCommandService;
import pe.edu.upc.ice.track.platform.iam.domain.model.commands.SeedRolesCommand;
import pe.edu.upc.ice.track.platform.iam.domain.model.entities.Role;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.Roles;
import pe.edu.upc.ice.track.platform.iam.domain.repositories.RoleRepository;

import java.util.Arrays;

/**
 * Implementation of {@link RoleCommandService} to handle {@link SeedRolesCommand}.
 */
@Service
public class RoleCommandServiceImpl implements RoleCommandService {

  private final RoleRepository roleRepository;

  public RoleCommandServiceImpl(RoleRepository roleRepository) {
    this.roleRepository = roleRepository;
  }

  @Override
  public void handle(SeedRolesCommand command) {
    Arrays.stream(Roles.values()).forEach(role -> {
      if (!roleRepository.existsByName(role)) {
        roleRepository.save(new Role(Roles.valueOf(role.name())));
      }
    });
  }
}