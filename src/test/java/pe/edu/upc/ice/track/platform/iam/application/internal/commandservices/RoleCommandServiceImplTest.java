package pe.edu.upc.ice.track.platform.iam.application.internal.commandservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.iam.domain.model.commands.SeedRolesCommand;
import pe.edu.upc.ice.track.platform.iam.domain.model.entities.Role;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.Roles;
import pe.edu.upc.ice.track.platform.iam.domain.repositories.RoleRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleCommandServiceImplTest {

  @Mock
  private RoleRepository roleRepository;

  @InjectMocks
  private RoleCommandServiceImpl service;

  @Test
  @DisplayName("handle(SeedRolesCommand) -> saves every missing role (AAA)")
  void seedsAllMissingRoles() {
    // Arrange
    when(roleRepository.existsByName(Roles.OWNER_ROLE)).thenReturn(false);
    doReturn(false).when(roleRepository).existsByName(Roles.TECHNICIAN_ROLE);

    // Act
    service.handle(new SeedRolesCommand());

    // Assert
    var captor = ArgumentCaptor.forClass(Role.class);
    verify(roleRepository, times(2)).save(captor.capture());
    assertThat(captor.getAllValues()).extracting(Role::getName)
        .containsExactlyInAnyOrder(Roles.OWNER_ROLE, Roles.TECHNICIAN_ROLE);
    verifyNoMoreInteractions(roleRepository);
  }

  @Test
  @DisplayName("handle(SeedRolesCommand) -> saves only the roles that do not exist yet (AAA)")
  void seedsOnlyMissingRoles() {
    // Arrange
    when(roleRepository.existsByName(Roles.OWNER_ROLE)).thenReturn(true);
    doReturn(false).when(roleRepository).existsByName(Roles.TECHNICIAN_ROLE);

    // Act
    service.handle(new SeedRolesCommand());

    // Assert
    var captor = ArgumentCaptor.forClass(Role.class);
    verify(roleRepository, times(1)).save(captor.capture());
    assertThat(captor.getValue().getName()).isEqualTo(Roles.TECHNICIAN_ROLE);
  }

  @Test
  @DisplayName("handle(SeedRolesCommand) -> is idempotent when every role exists (AAA)")
  void seedingIsIdempotent() {
    // Arrange
    when(roleRepository.existsByName(Roles.OWNER_ROLE)).thenReturn(true);
    doReturn(true).when(roleRepository).existsByName(Roles.TECHNICIAN_ROLE);

    // Act
    service.handle(new SeedRolesCommand());

    // Assert
    verify(roleRepository, never()).save(any(Role.class));
  }
}
