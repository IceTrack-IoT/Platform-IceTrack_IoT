package pe.edu.upc.ice.track.platform.profiles.application.internal.queryservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetAllOwnersQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetAllTechniciansQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetDashboardConfigByIdQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetDashboardConfigByUserIdQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetOwnerByIdQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetOwnerByUserIdQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetTechnicianByIdQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetTechnicianByUserIdQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetTechnicianMetricsQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.readmodels.TechnicianMetrics;
import pe.edu.upc.ice.track.platform.profiles.domain.repositories.DashboardConfigRepository;
import pe.edu.upc.ice.track.platform.profiles.domain.repositories.OwnerProfileRepository;
import pe.edu.upc.ice.track.platform.profiles.domain.repositories.TechnicianMetricsRepository;
import pe.edu.upc.ice.track.platform.profiles.domain.repositories.TechnicianProfileRepository;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.DASHBOARD_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.OWNER_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.TECHNICIAN_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.USER_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.persistedDashboard;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.persistedOwner;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.persistedTechnician;

class ProfilesSimpleQueryServicesTest {

  private static final UserId USER = new UserId(USER_ID);

  @Nested
  @ExtendWith(MockitoExtension.class)
  class DashboardConfigQueryServiceImplTest {

    @Mock
    private DashboardConfigRepository repository;
    @InjectMocks
    private DashboardConfigQueryServiceImpl service;

    @Test
    @DisplayName("handle(GetDashboardConfigByUserIdQuery) -> the user's dashboard looked up by UserId (AAA)")
    void byUser() {
      // Arrange
      var dashboard = persistedDashboard();
      when(repository.findByUserId(USER)).thenReturn(Optional.of(dashboard));

      // Act & Assert
      assertThat(service.handle(new GetDashboardConfigByUserIdQuery(USER_ID))).containsSame(dashboard);
    }

    @Test
    @DisplayName("handle(GetDashboardConfigByIdQuery) -> dashboard by id, empty when absent (AAA)")
    void byId() {
      // Arrange
      var dashboard = persistedDashboard();
      when(repository.findById(DASHBOARD_ID)).thenReturn(Optional.of(dashboard));
      doReturn(Optional.empty()).when(repository).findById(99L);

      // Act & Assert
      assertThat(service.handle(new GetDashboardConfigByIdQuery(DASHBOARD_ID))).containsSame(dashboard);
      assertThat(service.handle(new GetDashboardConfigByIdQuery(99L))).isEmpty();
    }
  }

  @Nested
  @ExtendWith(MockitoExtension.class)
  class OwnerQueryServiceImplTest {

    @Mock
    private OwnerProfileRepository repository;
    @InjectMocks
    private OwnerQueryServiceImpl service;

    @Test
    @DisplayName("handle(GetOwnerByIdQuery) -> owner by id, empty when absent (AAA)")
    void byId() {
      // Arrange
      var owner = persistedOwner();
      when(repository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
      doReturn(Optional.empty()).when(repository).findById(99L);

      // Act & Assert
      assertThat(service.handle(new GetOwnerByIdQuery(OWNER_ID))).containsSame(owner);
      assertThat(service.handle(new GetOwnerByIdQuery(99L))).isEmpty();
    }

    @Test
    @DisplayName("handle(GetOwnerByUserIdQuery) -> owner owned by that account (AAA)")
    void byUser() {
      // Arrange
      var owner = persistedOwner();
      when(repository.findByUserId(USER)).thenReturn(Optional.of(owner));

      // Act & Assert
      assertThat(service.handle(new GetOwnerByUserIdQuery(USER))).containsSame(owner);
    }

    @Test
    @DisplayName("handle(GetAllOwnersQuery) -> every owner from the repository (AAA)")
    void all() {
      // Arrange
      var owners = List.of(persistedOwner());
      when(repository.findAll()).thenReturn(owners);

      // Act & Assert
      assertThat(service.handle(new GetAllOwnersQuery())).isEqualTo(owners);
      verify(repository, times(1)).findAll();
    }
  }

  @Nested
  @ExtendWith(MockitoExtension.class)
  class TechnicianQueryServiceSimpleLookups {

    @Mock
    private TechnicianProfileRepository technicianProfileRepository;
    @Mock
    private TechnicianMetricsRepository technicianMetricsRepository;
    @InjectMocks
    private TechnicianQueryServiceImpl service;

    @Test
    @DisplayName("handle(GetTechnicianByIdQuery) -> technician by id, empty when absent (AAA)")
    void byId() {
      // Arrange
      var technician = persistedTechnician();
      when(technicianProfileRepository.findById(TECHNICIAN_ID)).thenReturn(Optional.of(technician));
      doReturn(Optional.empty()).when(technicianProfileRepository).findById(99L);

      // Act & Assert
      assertThat(service.handle(new GetTechnicianByIdQuery(TECHNICIAN_ID))).containsSame(technician);
      assertThat(service.handle(new GetTechnicianByIdQuery(99L))).isEmpty();
    }

    @Test
    @DisplayName("handle(GetTechnicianByUserIdQuery) -> technician of that account (AAA)")
    void byUser() {
      // Arrange
      var technician = persistedTechnician();
      when(technicianProfileRepository.findByUserId(USER)).thenReturn(Optional.of(technician));

      // Act & Assert
      assertThat(service.handle(new GetTechnicianByUserIdQuery(USER))).containsSame(technician);
    }

    @Test
    @DisplayName("handle(GetAllTechniciansQuery) -> every technician from the repository (AAA)")
    void all() {
      // Arrange
      var technicians = List.of(persistedTechnician());
      when(technicianProfileRepository.findAll()).thenReturn(technicians);

      // Act & Assert
      assertThat(service.handle(new GetAllTechniciansQuery())).isEqualTo(technicians);
    }

    @Test
    @DisplayName("handle(GetTechnicianMetricsQuery) -> metrics of the technician, empty when none (AAA)")
    void metrics() {
      // Arrange
      var metrics = TechnicianMetrics.empty(TECHNICIAN_ID);
      when(technicianMetricsRepository.findByTechnicianProfileId(TECHNICIAN_ID)).thenReturn(Optional.of(metrics));
      doReturn(Optional.empty()).when(technicianMetricsRepository).findByTechnicianProfileId(99L);

      // Act & Assert
      assertThat(service.handle(new GetTechnicianMetricsQuery(TECHNICIAN_ID))).containsSame(metrics);
      assertThat(service.handle(new GetTechnicianMetricsQuery(99L))).isEmpty();
    }
  }
}
