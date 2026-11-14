package pe.edu.upc.ice.track.platform.iam.application.internal.queryservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.iam.domain.model.queries.GetAllRolesQuery;
import pe.edu.upc.ice.track.platform.iam.domain.model.queries.GetRoleByNameQuery;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.Roles;
import pe.edu.upc.ice.track.platform.iam.domain.repositories.RoleRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.iam.test.fixtures.IamDomainFixtures.ownerRole;
import static pe.edu.upc.ice.track.platform.iam.test.fixtures.IamDomainFixtures.technicianRole;

@ExtendWith(MockitoExtension.class)
class RoleQueryServiceImplTest {

  @Mock
  private RoleRepository roleRepository;

  @InjectMocks
  private RoleQueryServiceImpl service;

  @Test
  @DisplayName("handle(GetAllRolesQuery) -> returns every stored role (AAA)")
  void returnsAllRoles() {
    // Arrange
    when(roleRepository.findAll()).thenReturn(List.of(ownerRole(), technicianRole()));

    // Act
    var roles = service.handle(new GetAllRolesQuery());

    // Assert
    assertThat(roles).containsExactly(ownerRole(), technicianRole());
    verify(roleRepository, times(1)).findAll();
    verifyNoMoreInteractions(roleRepository);
  }

  @Test
  @DisplayName("handle(GetRoleByNameQuery) -> returns the role found by name (AAA)")
  void returnsRoleByName() {
    // Arrange
    when(roleRepository.findByName(Roles.OWNER_ROLE)).thenReturn(Optional.of(ownerRole()));

    // Act
    var role = service.handle(new GetRoleByNameQuery(Roles.OWNER_ROLE));

    // Assert
    assertThat(role).contains(ownerRole());
  }

  @Test
  @DisplayName("handle(GetRoleByNameQuery) -> returns empty when the role does not exist (AAA)")
  void returnsEmptyWhenRoleMissing() {
    // Arrange
    when(roleRepository.findByName(Roles.TECHNICIAN_ROLE)).thenReturn(Optional.empty());

    // Act
    var role = service.handle(new GetRoleByNameQuery(Roles.TECHNICIAN_ROLE));

    // Assert
    assertThat(role).isEmpty();
  }
}
