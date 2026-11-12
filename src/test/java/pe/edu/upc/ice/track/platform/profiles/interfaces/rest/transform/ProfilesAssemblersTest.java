package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.transform;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pe.edu.upc.ice.track.platform.profiles.application.results.SessionContext;
import pe.edu.upc.ice.track.platform.profiles.domain.model.readmodels.TechnicianDirectoryEntry;
import pe.edu.upc.ice.track.platform.profiles.domain.model.readmodels.TechnicianDirectoryPage;
import pe.edu.upc.ice.track.platform.profiles.domain.model.readmodels.TechnicianMetrics;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.authorization.ProfileView;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.CardLayoutItemResource;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.CreateDashboardConfigResource;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.TemperatureRangeResource;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.UpdateDashboardDefaultsResource;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.UpdateDashboardLayoutResource;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.UpdateOwnerResource;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.UpdateTechnicianResource;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.DASHBOARD_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.DASHBOARD_VERSION;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.EMAIL;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.OWNER_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.RUC;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.SITE_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.TECHNICIAN_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.USER_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.dashboard;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.persistedDashboard;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.persistedOwner;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.persistedTechnician;

class ProfilesAssemblersTest {

  private static final TemperatureRangeResource RANGE = new TemperatureRangeResource(2, 8, "C", "Cold chain");

  @Nested
  class DashboardAssemblers {

    @Test
    @DisplayName("DashboardConfigResourceFromEntityAssembler.toResourceFromEntity(config) -> ids, range, cards and version (AAA)")
    void dashboardToResource() {
      // Act
      var resource = DashboardConfigResourceFromEntityAssembler.toResourceFromEntity(persistedDashboard());

      // Assert
      assertThat(resource.id()).isEqualTo(DASHBOARD_ID);
      assertThat(resource.userId()).isEqualTo(USER_ID);
      assertThat(resource.defaultSiteId()).isEqualTo(SITE_ID);
      assertThat(resource.defaultTemperatureRange()).isEqualTo(RANGE);
      assertThat(resource.cards()).hasSize(4);
      assertThat(resource.cards().get(0).cardId()).isEqualTo(101L);
      assertThat(resource.cards().get(0).cardType()).isEqualTo("MONITORED_EQUIPMENT");
      assertThat(resource.cards().get(0).order()).isEqualTo(1);
      assertThat(resource.cards().get(0).isVisible()).isTrue();
      assertThat(resource.version()).isEqualTo(DASHBOARD_VERSION);
    }

    @Test
    @DisplayName("DashboardConfigResourceFromEntityAssembler.toResourceFromEntity(new config) -> version 0 when none stored (AAA)")
    void unversionedDashboardHasVersionZero() {
      // Act & Assert
      assertThat(DashboardConfigResourceFromEntityAssembler.toResourceFromEntity(dashboard()).version()).isZero();
    }

    @Test
    @DisplayName("InitializeDashboardConfigCommandFromResourceAssembler -> flattens the temperature range (AAA)")
    void initializeCommand() {
      // Act
      var command = InitializeDashboardConfigCommandFromResourceAssembler.toCommandFromResource(
          new CreateDashboardConfigResource(USER_ID, SITE_ID, RANGE));

      // Assert
      assertThat(command.userId()).isEqualTo(USER_ID);
      assertThat(command.siteId()).isEqualTo(SITE_ID);
      assertThat(command.tempMin()).isEqualTo(2);
      assertThat(command.tempMax()).isEqualTo(8);
      assertThat(command.tempUnit()).isEqualTo("C");
      assertThat(command.tempLabel()).isEqualTo("Cold chain");
    }

    @Test
    @DisplayName("UpdateDashboardDefaultsCommandFromResourceAssembler -> flattens range and parses If-Match (AAA)")
    void defaultsCommand() {
      // Act
      var command = UpdateDashboardDefaultsCommandFromResourceAssembler.toCommandFromResource(
          USER_ID, new UpdateDashboardDefaultsResource(4L, RANGE), "\"3\"");
      var withoutHeader = UpdateDashboardDefaultsCommandFromResourceAssembler.toCommandFromResource(
          USER_ID, new UpdateDashboardDefaultsResource(4L, RANGE), null);

      // Assert
      assertThat(command.siteId()).isEqualTo(4L);
      assertThat(command.tempLabel()).isEqualTo("Cold chain");
      assertThat(command.expectedVersion()).isEqualTo(3L);
      assertThat(withoutHeader.expectedVersion()).isNull();
    }

    @Test
    @DisplayName("UpdateDashboardLayoutCommandFromResourceAssembler -> maps each card and parses weak If-Match (AAA)")
    void layoutCommand() {
      // Arrange
      var resource = new UpdateDashboardLayoutResource(List.of(
          new CardLayoutItemResource(101L, 2, false), new CardLayoutItemResource(102L, 1, true)));

      // Act
      var command = UpdateDashboardLayoutCommandFromResourceAssembler.toCommandFromResource(USER_ID, resource, "W/\"7\"");

      // Assert
      assertThat(command.userId()).isEqualTo(USER_ID);
      assertThat(command.cards()).hasSize(2);
      assertThat(command.cards().get(0).cardId()).isEqualTo(101L);
      assertThat(command.cards().get(0).order()).isEqualTo(2);
      assertThat(command.cards().get(0).isVisible()).isFalse();
      assertThat(command.expectedVersion()).isEqualTo(7L);
    }

    @Test
    @DisplayName("update assemblers with a malformed If-Match -> IllegalArgumentException (AAA)")
    void malformedIfMatch() {
      // Act & Assert
      assertThatThrownBy(() -> UpdateDashboardLayoutCommandFromResourceAssembler.toCommandFromResource(
          USER_ID, new UpdateDashboardLayoutResource(List.of()), "abc"))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  class OwnerAssemblers {

    @Test
    @DisplayName("OwnerProfileResourceFromEntityAssembler(owner, FULL) -> every field including address parts and RUC (AAA)")
    void fullView() {
      // Act
      var resource = OwnerProfileResourceFromEntityAssembler.toResourceFromEntity(persistedOwner(), ProfileView.FULL);

      // Assert
      assertThat(resource.id()).isEqualTo(OWNER_ID);
      assertThat(resource.userId()).isEqualTo(USER_ID);
      assertThat(resource.fullName()).isEqualTo("Ana Torres");
      assertThat(resource.email()).isEqualTo(EMAIL);
      assertThat(resource.phone()).isEqualTo("+51 987654321");
      assertThat(resource.address()).isEqualTo("Av. Primavera 120, Lima, 15023, Peru");
      assertThat(resource.street()).isEqualTo("Av. Primavera");
      assertThat(resource.number()).isEqualTo("120");
      assertThat(resource.city()).isEqualTo("Lima");
      assertThat(resource.postalCode()).isEqualTo("15023");
      assertThat(resource.country()).isEqualTo("Peru");
      assertThat(resource.ruc()).isEqualTo(RUC);
      assertThat(resource.version()).isEqualTo(2L);
    }

    @Test
    @DisplayName("OwnerProfileResourceFromEntityAssembler(owner, CONTACT) -> name, e-mail and phone only (AAA)")
    void contactView() {
      // Act
      var resource = OwnerProfileResourceFromEntityAssembler.toResourceFromEntity(persistedOwner(), ProfileView.CONTACT);

      // Assert
      assertThat(resource.fullName()).isEqualTo("Ana Torres");
      assertThat(resource.email()).isEqualTo(EMAIL);
      assertThat(resource.phone()).isEqualTo("+51 987654321");
      assertThat(resource.address()).isNull();
      assertThat(resource.ruc()).isNull();
      assertThat(resource.version()).isNull();
    }

    @Test
    @DisplayName("OwnerProfileResourceFromEntityAssembler(owner, PUBLIC or NONE) -> only identity and name (AAA)")
    void publicView() {
      // Act & Assert
      for (var view : List.of(ProfileView.PUBLIC, ProfileView.NONE)) {
        var resource = OwnerProfileResourceFromEntityAssembler.toResourceFromEntity(persistedOwner(), view);
        assertThat(resource.id()).isEqualTo(OWNER_ID);
        assertThat(resource.fullName()).isEqualTo("Ana Torres");
        assertThat(resource.email()).isNull();
        assertThat(resource.phone()).isNull();
        assertThat(resource.ruc()).isNull();
      }
    }

    @Test
    @DisplayName("OwnerProfileResourceFromEntityAssembler(owner without stored version) -> version 0 (AAA)")
    void unversionedOwner() {
      // Act & Assert
      assertThat(OwnerProfileResourceFromEntityAssembler.toResourceFromEntity(
          new pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.OwnerProfile(OWNER_ID,
              new pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId(USER_ID),
              persistedOwner().getFullName(), persistedOwner().getEmail(), persistedOwner().getPhone(),
              persistedOwner().getAddress(), persistedOwner().getRuc())).version()).isZero();
    }

    @Test
    @DisplayName("UpdateOwnerCommandFromResourceAssembler -> parses names, phone, address, RUC and If-Match (AAA)")
    void updateOwnerCommand() {
      // Arrange
      var resource = new UpdateOwnerResource("Maria Lopez", "+51 911222333", "Calle Sol", "5", "Cusco", "08000", "Peru",
          10987654321L);

      // Act
      var command = UpdateOwnerCommandFromResourceAssembler.toCommandFromResource(OWNER_ID, resource, "2");

      // Assert
      assertThat(command.ownerId()).isEqualTo(OWNER_ID);
      assertThat(command.fullName().firstName()).isEqualTo("Maria");
      assertThat(command.phone().countryCode()).isEqualTo("+51");
      assertThat(command.address().city()).isEqualTo("Cusco");
      assertThat(command.ruc().value()).isEqualTo(10987654321L);
      assertThat(command.expectedVersion()).isEqualTo(2L);
    }
  }

  @Nested
  class TechnicianAssemblers {

    @Test
    @DisplayName("TechnicianProfileResourceFromEntityAssembler(technician, FULL) -> contact data, address and certification (AAA)")
    void fullView() {
      // Act
      var resource = TechnicianProfileResourceFromEntityAssembler.toResourceFromEntity(
          persistedTechnician(), ProfileView.FULL);

      // Assert
      assertThat(resource.id()).isEqualTo(TECHNICIAN_ID);
      assertThat(resource.email()).isEqualTo(EMAIL);
      assertThat(resource.phone()).isEqualTo("+51 987654321");
      assertThat(resource.address()).isEqualTo("Av. Primavera 120, Lima, 15023, Peru");
      assertThat(resource.speciality()).isEqualTo("Refrigeration");
      assertThat(resource.certificationNumber()).isEqualTo("CERT-001");
    }

    @Test
    @DisplayName("TechnicianProfileResourceFromEntityAssembler(technician, CONTACT) -> e-mail and phone, never the address (AAA)")
    void contactView() {
      // Act
      var resource = TechnicianProfileResourceFromEntityAssembler.toResourceFromEntity(
          persistedTechnician(), ProfileView.CONTACT);

      // Assert
      assertThat(resource.email()).isEqualTo(EMAIL);
      assertThat(resource.phone()).isEqualTo("+51 987654321");
      assertThat(resource.address()).isNull();
      assertThat(resource.speciality()).isEqualTo("Refrigeration");
    }

    @Test
    @DisplayName("TechnicianProfileResourceFromEntityAssembler(technician, PUBLIC or NONE) -> no contact data (AAA)")
    void publicView() {
      // Act & Assert
      for (var view : List.of(ProfileView.PUBLIC, ProfileView.NONE)) {
        var resource = TechnicianProfileResourceFromEntityAssembler.toResourceFromEntity(persistedTechnician(), view);
        assertThat(resource.email()).isNull();
        assertThat(resource.phone()).isNull();
        assertThat(resource.address()).isNull();
        assertThat(resource.fullName()).isEqualTo("Ana Torres");
        assertThat(resource.certificationNumber()).isEqualTo("CERT-001");
      }
    }

    @Test
    @DisplayName("UpdateTechnicianCommandFromResourceAssembler -> parses names, phone, address, speciality, certification (AAA)")
    void updateTechnicianCommand() {
      // Arrange
      var resource = new UpdateTechnicianResource("Luis Perez", "955666777", "Jr. Luna", null, "Arequipa", "04000",
          "Peru", "HVAC", "HV-77");

      // Act
      var command = UpdateTechnicianCommandFromResourceAssembler.toCommandFromResource(TECHNICIAN_ID, resource);

      // Assert
      assertThat(command.technicianId()).isEqualTo(TECHNICIAN_ID);
      assertThat(command.fullName().lastName()).isEqualTo("Perez");
      assertThat(command.phone().countryCode()).isEmpty();
      assertThat(command.address().number()).isNull();
      assertThat(command.speciality().name()).isEqualTo("HVAC");
      assertThat(command.certificationNumber()).isEqualTo("HV-77");
    }

    @Test
    @DisplayName("TechnicianPublicProfileResourceFromEntityAssembler(profile, metrics) -> counters and rating flag (AAA)")
    void publicProfileWithMetrics() {
      // Act
      var resource = TechnicianPublicProfileResourceFromEntityAssembler.toResourceFromEntity(
          persistedTechnician(), Optional.of(new TechnicianMetrics(TECHNICIAN_ID, 7, 8.0, 2, null)));

      // Assert
      assertThat(resource.id()).isEqualTo(TECHNICIAN_ID);
      assertThat(resource.fullName()).isEqualTo("Ana Torres");
      assertThat(resource.speciality()).isEqualTo("Refrigeration");
      assertThat(resource.completedServices()).isEqualTo(7);
      assertThat(resource.averageRating()).isEqualTo(4.0);
      assertThat(resource.ratingAvailable()).isTrue();
    }

    @Test
    @DisplayName("TechnicianPublicProfileResourceFromEntityAssembler(profile, no metrics) -> zero services, no rating (AAA)")
    void publicProfileWithoutMetrics() {
      // Act
      var resource = TechnicianPublicProfileResourceFromEntityAssembler.toResourceFromEntity(
          persistedTechnician(), Optional.empty());

      // Assert
      assertThat(resource.completedServices()).isZero();
      assertThat(resource.averageRating()).isNull();
      assertThat(resource.ratingAvailable()).isFalse();
    }

    @Test
    @DisplayName("TechnicianPublicProfileResourceFromEntityAssembler.toPagedResourceFromPage(page) -> items and paging (AAA)")
    void pagedResource() {
      // Arrange
      var entries = List.of(
          new TechnicianDirectoryEntry(persistedTechnician(), new TechnicianMetrics(TECHNICIAN_ID, 3, 5.0, 1, null)),
          new TechnicianDirectoryEntry(persistedTechnician(), null));
      var page = new TechnicianDirectoryPage(entries, 1, 2, 5L);

      // Act
      var resource = TechnicianPublicProfileResourceFromEntityAssembler.toPagedResourceFromPage(page);

      // Assert
      assertThat(resource.content()).hasSize(2);
      assertThat(resource.content().get(0).averageRating()).isEqualTo(5.0);
      assertThat(resource.content().get(1).ratingAvailable()).isFalse();
      assertThat(resource.page()).isEqualTo(1);
      assertThat(resource.size()).isEqualTo(2);
      assertThat(resource.totalElements()).isEqualTo(5L);
      assertThat(resource.totalPages()).isEqualTo(3);
    }
  }

  @Nested
  class SessionAssembler {

    @Test
    @DisplayName("SessionContextResourceFromResultAssembler(owner session) -> user, owner, dashboard, counts and time (AAA)")
    void ownerSession() {
      // Arrange
      var now = Instant.parse("2026-11-11T10:00:00Z");
      var context = new SessionContext(USER_ID, "atorres", EMAIL, "ROLE_OWNER", "LOCAL", persistedOwner(),
          persistedDashboard(), 3L, 12L, now);

      // Act
      var resource = SessionContextResourceFromResultAssembler.toResourceFromResult(context);

      // Assert
      assertThat(resource.user().id()).isEqualTo(USER_ID);
      assertThat(resource.user().username()).isEqualTo("atorres");
      assertThat(resource.user().email()).isEqualTo(EMAIL);
      assertThat(resource.user().role()).isEqualTo("ROLE_OWNER");
      assertThat(resource.user().provider()).isEqualTo("LOCAL");
      assertThat(resource.ownerProfile().ruc()).isEqualTo(RUC);
      assertThat(resource.dashboardConfig().id()).isEqualTo(DASHBOARD_ID);
      assertThat(resource.counts().sites()).isEqualTo(3L);
      assertThat(resource.counts().equipment()).isEqualTo(12L);
      assertThat(resource.serverTime()).isEqualTo(now);
    }

    @Test
    @DisplayName("SessionContextResourceFromResultAssembler(session without profile or dashboard) -> null parts (AAA)")
    void sessionWithoutProfile() {
      // Arrange
      var context = new SessionContext(USER_ID, "atorres", null, "ROLE_TECHNICIAN", "LOCAL", null, null, 0L, 0L,
          Instant.now());

      // Act
      var resource = SessionContextResourceFromResultAssembler.toResourceFromResult(context);

      // Assert
      assertThat(resource.ownerProfile()).isNull();
      assertThat(resource.dashboardConfig()).isNull();
      assertThat(resource.counts().sites()).isZero();
    }

    @Test
    @DisplayName("new SessionContext(null user id or server time) -> NullPointerException (AAA)")
    void sessionContextInvariants() {
      // Act & Assert
      assertThatThrownBy(() -> new SessionContext(null, "u", null, "r", "p", null, null, 0, 0, Instant.now()))
          .hasMessage("userId must not be null");
      assertThatThrownBy(() -> new SessionContext(USER_ID, "u", null, "r", "p", null, null, 0, 0, null))
          .hasMessage("serverTime must not be null");
    }
  }
}
