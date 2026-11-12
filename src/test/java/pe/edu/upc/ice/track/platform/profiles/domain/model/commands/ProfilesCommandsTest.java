package pe.edu.upc.ice.track.platform.profiles.domain.model.commands;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pe.edu.upc.ice.track.platform.profiles.domain.model.factories.ProfileCreationData;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.CERTIFICATION;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.OWNER_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.SITE_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.TECHNICIAN_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.USER_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.address;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.email;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.ownerData;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.personName;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.phone;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.ruc;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.speciality;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.technicianData;

class ProfilesCommandsTest {

  @Nested
  class CreateCommands {

    @Test
    @DisplayName("new CreateOwnerCommand(user, data with ruc) -> keeps both (AAA)")
    void createOwner() {
      // Act
      var command = new CreateOwnerCommand(new UserId(USER_ID), ownerData());

      // Assert
      assertThat(command.userId()).isEqualTo(new UserId(USER_ID));
      assertThat(command.data().ruc()).isEqualTo(ruc());
    }

    @Test
    @DisplayName("new CreateOwnerCommand(null user, null data or data without ruc) -> NullPointerException (AAA)")
    void createOwnerInvalid() {
      // Arrange
      var user = new UserId(USER_ID);
      var dataWithoutRuc = new ProfileCreationData(personName(), email(), phone(), address(), null, null, null);

      // Act & Assert
      assertThatThrownBy(() -> new CreateOwnerCommand(null, ownerData())).hasMessage("userId must not be null");
      assertThatThrownBy(() -> new CreateOwnerCommand(user, null)).hasMessage("data must not be null");
      assertThatThrownBy(() -> new CreateOwnerCommand(user, dataWithoutRuc)).hasMessage("ruc must not be null");
    }

    @Test
    @DisplayName("new CreateTechnicianCommand(user, data) -> keeps both (AAA)")
    void createTechnician() {
      // Act
      var command = new CreateTechnicianCommand(new UserId(USER_ID), technicianData());

      // Assert
      assertThat(command.data().speciality()).isEqualTo(speciality());
      assertThat(command.data().certificationNumber()).isEqualTo(CERTIFICATION);
    }

    @Test
    @DisplayName("new CreateTechnicianCommand(invalid) -> rejects null user/data/speciality and blank certification (AAA)")
    void createTechnicianInvalid() {
      // Arrange
      var user = new UserId(USER_ID);
      var noSpeciality = new ProfileCreationData(personName(), email(), phone(), address(), null, null, CERTIFICATION);
      var blankCertification = new ProfileCreationData(personName(), email(), phone(), address(), null, speciality(), " ");

      // Act & Assert
      assertThatThrownBy(() -> new CreateTechnicianCommand(null, technicianData())).hasMessage("userId must not be null");
      assertThatThrownBy(() -> new CreateTechnicianCommand(user, null)).hasMessage("data must not be null");
      assertThatThrownBy(() -> new CreateTechnicianCommand(user, noSpeciality)).hasMessage("speciality must not be null");
      assertThatThrownBy(() -> new CreateTechnicianCommand(user, blankCertification))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Certification number must not be null or blank");
    }
  }

  @Nested
  class UpdateCommands {

    @Test
    @DisplayName("new UpdateOwnerCommand(without version) -> expectedVersion is null (AAA)")
    void updateOwnerWithoutVersion() {
      // Act
      var command = new UpdateOwnerCommand(OWNER_ID, personName(), phone(), address(), ruc());

      // Assert
      assertThat(command.expectedVersion()).isNull();
      assertThat(command.ownerId()).isEqualTo(OWNER_ID);
    }

    @Test
    @DisplayName("new UpdateOwnerCommand(null parts) -> NullPointerException naming the part (AAA)")
    void updateOwnerInvalid() {
      // Act & Assert
      assertThatThrownBy(() -> new UpdateOwnerCommand(null, personName(), phone(), address(), ruc()))
          .hasMessage("ownerId must not be null");
      assertThatThrownBy(() -> new UpdateOwnerCommand(OWNER_ID, null, phone(), address(), ruc()))
          .hasMessage("fullName must not be null");
      assertThatThrownBy(() -> new UpdateOwnerCommand(OWNER_ID, personName(), null, address(), ruc()))
          .hasMessage("phone must not be null");
      assertThatThrownBy(() -> new UpdateOwnerCommand(OWNER_ID, personName(), phone(), null, ruc()))
          .hasMessage("address must not be null");
      assertThatThrownBy(() -> new UpdateOwnerCommand(OWNER_ID, personName(), phone(), address(), null))
          .hasMessage("ruc must not be null");
    }

    @Test
    @DisplayName("new UpdateTechnicianCommand(valid) -> keeps its values (AAA)")
    void updateTechnician() {
      // Act
      var command = new UpdateTechnicianCommand(TECHNICIAN_ID, personName(), phone(), address(), speciality(),
          CERTIFICATION);

      // Assert
      assertThat(command.technicianId()).isEqualTo(TECHNICIAN_ID);
      assertThat(command.certificationNumber()).isEqualTo(CERTIFICATION);
    }

    @Test
    @DisplayName("new UpdateTechnicianCommand(invalid) -> NullPointerException or blank certification rejected (AAA)")
    void updateTechnicianInvalid() {
      // Act & Assert
      assertThatThrownBy(() -> new UpdateTechnicianCommand(null, personName(), phone(), address(), speciality(), CERTIFICATION))
          .hasMessage("technicianId must not be null");
      assertThatThrownBy(() -> new UpdateTechnicianCommand(TECHNICIAN_ID, null, phone(), address(), speciality(), CERTIFICATION))
          .hasMessage("fullName must not be null");
      assertThatThrownBy(() -> new UpdateTechnicianCommand(TECHNICIAN_ID, personName(), null, address(), speciality(), CERTIFICATION))
          .hasMessage("phone must not be null");
      assertThatThrownBy(() -> new UpdateTechnicianCommand(TECHNICIAN_ID, personName(), phone(), null, speciality(), CERTIFICATION))
          .hasMessage("address must not be null");
      assertThatThrownBy(() -> new UpdateTechnicianCommand(TECHNICIAN_ID, personName(), phone(), address(), null, CERTIFICATION))
          .hasMessage("speciality must not be null");
      assertThatThrownBy(() -> new UpdateTechnicianCommand(TECHNICIAN_ID, personName(), phone(), address(), speciality(), " "))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Certification number must not be null or blank");
    }
  }

  @Nested
  class DashboardCommands {

    @Test
    @DisplayName("new InitializeDashboardConfigCommand(null user) -> NullPointerException (AAA)")
    void initializeRequiresUser() {
      // Act & Assert
      assertThatThrownBy(() -> new InitializeDashboardConfigCommand(null, SITE_ID, 2, 8, "C", "x"))
          .hasMessage("userId must not be null");
      assertThat(new InitializeDashboardConfigCommand(USER_ID, null, null, null, null, null).userId()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("new ResetDashboardConfigToDefaultCommand(null) -> NullPointerException (AAA)")
    void resetRequiresUser() {
      // Act & Assert
      assertThatThrownBy(() -> new ResetDashboardConfigToDefaultCommand(null)).hasMessage("userId must not be null");
    }

    @Test
    @DisplayName("new ToggleCardVisibilityCommand(user, card) -> no expected version; nulls rejected (AAA)")
    void toggle() {
      // Act & Assert
      assertThat(new ToggleCardVisibilityCommand(USER_ID, 101L).expectedVersion()).isNull();
      assertThat(new ToggleCardVisibilityCommand(USER_ID, 101L, 4L).expectedVersion()).isEqualTo(4L);
      assertThatThrownBy(() -> new ToggleCardVisibilityCommand(null, 101L)).hasMessage("userId must not be null");
      assertThatThrownBy(() -> new ToggleCardVisibilityCommand(USER_ID, null)).hasMessage("cardId must not be null");
    }

    @Test
    @DisplayName("new UpdateDashboardDefaultsCommand(without version) -> expectedVersion null; null user rejected (AAA)")
    void defaults() {
      // Act & Assert
      assertThat(new UpdateDashboardDefaultsCommand(USER_ID, SITE_ID, 2, 8, "C", "x").expectedVersion()).isNull();
      assertThatThrownBy(() -> new UpdateDashboardDefaultsCommand(null, SITE_ID, 2, 8, "C", "x", 1L))
          .hasMessage("userId must not be null");
    }

    @Test
    @DisplayName("new UpdateDashboardLayoutCommand(cards) -> defensive immutable copy; nulls rejected (AAA)")
    void layout() {
      // Arrange
      var cards = new ArrayList<>(List.of(new CardLayoutCommandItem(101L, 1, true)));

      // Act
      var command = new UpdateDashboardLayoutCommand(USER_ID, cards);
      cards.clear();

      // Assert
      assertThat(command.cards()).hasSize(1);
      assertThat(command.expectedVersion()).isNull();
      assertThatThrownBy(() -> command.cards().add(new CardLayoutCommandItem(102L, 2, true)))
          .isInstanceOf(UnsupportedOperationException.class);
      assertThatThrownBy(() -> new UpdateDashboardLayoutCommand(null, List.of())).hasMessage("userId must not be null");
      assertThatThrownBy(() -> new UpdateDashboardLayoutCommand(USER_ID, null)).hasMessage("cards must not be null");
    }

    @Test
    @DisplayName("new CardLayoutCommandItem(null id or order) -> NullPointerException (AAA)")
    void layoutItem() {
      // Act & Assert
      assertThatThrownBy(() -> new CardLayoutCommandItem(null, 1, true)).hasMessage("cardId must not be null");
      assertThatThrownBy(() -> new CardLayoutCommandItem(1L, null, true)).hasMessage("order must not be null");
      assertThat(new CardLayoutCommandItem(1L, 2, false).isVisible()).isFalse();
    }
  }
}
