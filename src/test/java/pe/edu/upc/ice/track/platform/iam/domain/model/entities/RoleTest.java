package pe.edu.upc.ice.track.platform.iam.domain.model.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.Roles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoleTest {

  @Test
  @DisplayName("Role(Roles) -> creates an unsaved role (AAA)")
  void constructorWithNameLeavesIdNull() {
    // Act
    var role = new Role(Roles.OWNER_ROLE);

    // Assert
    assertThat(role.getId()).isNull();
    assertThat(role.getName()).isEqualTo(Roles.OWNER_ROLE);
    assertThat(role.getStringName()).isEqualTo("OWNER_ROLE");
  }

  @Test
  @DisplayName("toRoleFromName(String) -> resolves a role from a loose name (AAA)")
  void toRoleFromNameResolves() {
    assertThat(Role.toRoleFromName("technician").getName()).isEqualTo(Roles.TECHNICIAN_ROLE);
  }

  @Test
  @DisplayName("toRoleFromName(String) -> propagates the failure for an unknown name (AAA)")
  void toRoleFromNameRejectsUnknown() {
    assertThrows(IllegalArgumentException.class, () -> Role.toRoleFromName("admin"));
  }
}
