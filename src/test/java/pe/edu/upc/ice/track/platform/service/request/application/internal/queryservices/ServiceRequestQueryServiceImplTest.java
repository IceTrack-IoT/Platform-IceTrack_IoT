package pe.edu.upc.ice.track.platform.service.request.application.internal.queryservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.service.request.application.internal.outboundservices.acl.EquipmentRef;
import pe.edu.upc.ice.track.platform.service.request.application.internal.outboundservices.acl.ExternalEquipmentService;
import pe.edu.upc.ice.track.platform.service.request.application.internal.outboundservices.acl.ExternalUserProfileService;
import pe.edu.upc.ice.track.platform.service.request.application.internal.queryservices.ServiceRequestQueryServiceImpl;
import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.ServiceRequest;
import pe.edu.upc.ice.track.platform.service.request.domain.model.queries.GetServiceRequestByIdQuery;
import pe.edu.upc.ice.track.platform.service.request.domain.model.queries.GetServiceRequestDisplayDetailsQuery;
import pe.edu.upc.ice.track.platform.service.request.domain.model.queries.ListEquipmentInterventionsQuery;
import pe.edu.upc.ice.track.platform.service.request.domain.model.queries.ListServiceRequestsQuery;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.*;
import pe.edu.upc.ice.track.platform.service.request.domain.repositories.ReviewRepository;
import pe.edu.upc.ice.track.platform.service.request.domain.repositories.ServiceRequestRepository;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.CallerRole;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.EquipmentId;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.service.request.test.fixtures.ServiceRequestDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class ServiceRequestQueryServiceImplTest {

  private static final ServiceRequestId REQUEST = new ServiceRequestId(REQUEST_ID);

  @Mock private ServiceRequestRepository requests;
  @Mock private ReviewRepository reviews;
  @Mock private ExternalEquipmentService equipment;
  @Mock private ExternalUserProfileService profiles;
  @InjectMocks private ServiceRequestQueryServiceImpl service;

  private void asOwner() {
    doReturn(Optional.of(OWNER_ID)).when(profiles).fetchOwnerIdByUsername(USERNAME);
  }

  private void asTechnician() {
    doReturn(Optional.of(TECHNICIAN_ID)).when(profiles).fetchTechnicianIdByUsername(USERNAME);
  }

  @Nested
  class ById {

    @Test
    @DisplayName("handle(GetServiceRequestByIdQuery as OWNER) -> own request; foreign owner -> empty (AAA)")
    void owner() {
      // Arrange
      var request = pending();
      asOwner();
      doReturn(Optional.of(OTHER_OWNER_ID)).when(profiles).fetchOwnerIdByUsername("other");
      doReturn(Optional.of(request)).when(requests).findById(REQUEST);

      // Act & Assert
      assertThat(service.handle(new GetServiceRequestByIdQuery(REQUEST_ID, USERNAME, CallerRole.OWNER))).containsSame(request);
      assertThat(service.handle(new GetServiceRequestByIdQuery(REQUEST_ID, "other", CallerRole.OWNER))).isEmpty();
    }

    @Test
    @DisplayName("handle(GetServiceRequestByIdQuery as TECHNICIAN) -> only requests assigned to the caller (AAA)")
    void technician() {
      // Arrange
      var assigned = assigned();
      asTechnician();
      doReturn(Optional.of(OTHER_TECHNICIAN_ID)).when(profiles).fetchTechnicianIdByUsername("other");
      doReturn(Optional.of(assigned)).when(requests).findById(REQUEST);

      // Act & Assert
      assertThat(service.handle(new GetServiceRequestByIdQuery(REQUEST_ID, USERNAME, CallerRole.TECHNICIAN))).containsSame(assigned);
      assertThat(service.handle(new GetServiceRequestByIdQuery(REQUEST_ID, "other", CallerRole.TECHNICIAN))).isEmpty();
    }

    @Test
    @DisplayName("handle(GetServiceRequestByIdQuery) with invalid id or no profile -> empty (AAA)")
    void invalid() {
      // Arrange
      doReturn(Optional.empty()).when(profiles).fetchOwnerIdByUsername("nobody");
      doReturn(Optional.empty()).when(profiles).fetchTechnicianIdByUsername("nobody");

      // Act & Assert
      assertThat(service.handle(new GetServiceRequestByIdQuery(null, USERNAME, CallerRole.OWNER))).isEmpty();
      assertThat(service.handle(new GetServiceRequestByIdQuery(-1L, USERNAME, CallerRole.TECHNICIAN))).isEmpty();
      assertThat(service.handle(new GetServiceRequestByIdQuery(REQUEST_ID, "nobody", CallerRole.OWNER))).isEmpty();
      assertThat(service.handle(new GetServiceRequestByIdQuery(REQUEST_ID, "nobody", CallerRole.TECHNICIAN))).isEmpty();
      verifyNoInteractions(requests);
    }
  }

  @Nested
  class Listing {

    private final ServiceRequestFilter filter = new ServiceRequestFilter(Set.of(ServiceStatus.PENDING), null, null, null, null, null, null, null);

    @Test
    @DisplayName("handle(ListServiceRequestsQuery as OWNER) -> the owner's page with filter and paging (AAA)")
    void ownerPage() {
      // Arrange
      asOwner();
      var page = new ServiceRequestPage(List.of(pending()), 1, 5, 6);
      doReturn(page).when(requests).findPageByOwnerId(new OwnerId(OWNER_ID), filter, 1, 5);

      // Act & Assert
      assertThat(service.handle(new ListServiceRequestsQuery(USERNAME, CallerRole.OWNER, filter, false, 1, 5))).isSameAs(page);
    }

    @Test
    @DisplayName("handle(ListServiceRequestsQuery as TECHNICIAN) -> the technician's page (AAA)")
    void technicianPage() {
      // Arrange
      asTechnician();
      var page = new ServiceRequestPage(List.of(assigned()), 0, 20, 1);
      doReturn(page).when(requests).findPageByTechnicianId(new TechnicianProfileId(TECHNICIAN_ID), ServiceRequestFilter.NONE, 0, 20);

      // Act & Assert
      assertThat(service.handle(new ListServiceRequestsQuery(USERNAME, CallerRole.TECHNICIAN, null, true, null, null))).isSameAs(page);
    }

    @Test
    @DisplayName("handle(ListServiceRequestsQuery) for an account without the profile -> empty page with the requested paging (AAA)")
    void emptyPage() {
      // Arrange
      doReturn(Optional.empty()).when(profiles).fetchOwnerIdByUsername("nobody");
      doReturn(Optional.empty()).when(profiles).fetchTechnicianIdByUsername("nobody");

      // Act
      var owner = service.handle(new ListServiceRequestsQuery("nobody", CallerRole.OWNER, null, false, 2, 10));
      var technician = service.handle(new ListServiceRequestsQuery("nobody", CallerRole.TECHNICIAN, null, false, 0, 30));

      // Assert
      assertThat(owner.content()).isEmpty();
      assertThat(owner.page()).isEqualTo(2);
      assertThat(owner.size()).isEqualTo(10);
      assertThat(technician.size()).isEqualTo(30);
      verifyNoInteractions(requests);
    }
  }

  @Nested
  class DisplayDetails {

    @Test
    @DisplayName("handle(GetServiceRequestDisplayDetailsQuery with no requests) -> NONE without any lookup (AAA)")
    void empty() {
      // Act & Assert
      assertThat(service.handle(new GetServiceRequestDisplayDetailsQuery(List.of(), true))).isSameAs(pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceRequestDisplayDetails.NONE);
      verifyNoInteractions(equipment, profiles, reviews);
    }

    @Test
    @DisplayName("handle(GetServiceRequestDisplayDetailsQuery) -> names resolved once per owner, technicians in one call, reviews on demand (AAA)")
    void fullDetails() {
      // Arrange
      var first = assigned();
      var second = completed();
      doReturn(Map.of(EQUIPMENT_ID, "Freezer 1")).when(equipment).fetchEquipmentNamesByOwner(OWNER_ID);
      doReturn(Map.of(SITE_ID, "Lima Norte")).when(equipment).fetchSiteNamesByOwner(OWNER_ID);
      doReturn(Map.of(TECHNICIAN_ID, "Luis Rojas")).when(profiles).fetchTechnicianFullNames(Set.of(TECHNICIAN_ID));
      doReturn(Set.of(REQUEST_ID)).when(reviews).findReviewedServiceRequestIds(List.of(REQUEST_ID, REQUEST_ID));

      // Act
      var details = service.handle(new GetServiceRequestDisplayDetailsQuery(List.of(first, second), true));

      // Assert
      assertThat(details.equipmentNameOf(first)).isEqualTo("Freezer 1");
      assertThat(details.siteNameOf(first)).isEqualTo("Lima Norte");
      assertThat(details.technicianNameOf(first)).isEqualTo("Luis Rojas");
      assertThat(details.isReviewed(second)).isTrue();
      verify(equipment, times(1)).fetchEquipmentNamesByOwner(OWNER_ID);
      verify(equipment, times(1)).fetchSiteNamesByOwner(OWNER_ID);
    }

    @Test
    @DisplayName("handle(GetServiceRequestDisplayDetailsQuery without reviews) -> review repository untouched; no technician -> empty names (AAA)")
    void withoutReviews() {
      // Arrange
      var request = pending();
      doReturn(Map.of()).when(equipment).fetchEquipmentNamesByOwner(OWNER_ID);
      doReturn(Map.of()).when(equipment).fetchSiteNamesByOwner(OWNER_ID);
      doReturn(Map.of()).when(profiles).fetchTechnicianFullNames(Set.of());

      // Act
      var details = service.handle(new GetServiceRequestDisplayDetailsQuery(List.of(request), false));

      // Assert
      assertThat(details.reviewedServiceRequestIds()).isEmpty();
      assertThat(details.technicianNameOf(request)).isNull();
      verifyNoInteractions(reviews);
    }

    @Test
    @DisplayName("handle(GetServiceRequestDisplayDetailsQuery) with requests of two owners -> names merged from both (AAA)")
    void twoOwners() {
      // Arrange
      var mine = pending();
      var theirs = new ServiceRequest(new ServiceRequestId(78L), new OwnerId(OTHER_OWNER_ID), new EquipmentId(11L),
          new pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.SiteId(4L), null, mine.getType(), mine.getPriority(),
          "d", null, ServiceStatus.PENDING, mine.getCreatedAt(), null, null, null, null, null, 1L, mine.getHistory(), null, List.of());
      doReturn(Map.of(EQUIPMENT_ID, "Freezer 1")).when(equipment).fetchEquipmentNamesByOwner(OWNER_ID);
      doReturn(Map.of(11L, "Chamber")).when(equipment).fetchEquipmentNamesByOwner(OTHER_OWNER_ID);
      doReturn(Map.of()).when(equipment).fetchSiteNamesByOwner(OWNER_ID);
      doReturn(Map.of(4L, "Callao")).when(equipment).fetchSiteNamesByOwner(OTHER_OWNER_ID);
      doReturn(Map.of()).when(profiles).fetchTechnicianFullNames(Set.of());

      // Act
      var details = service.handle(new GetServiceRequestDisplayDetailsQuery(List.of(mine, theirs), false));

      // Assert
      assertThat(details.equipmentNameOf(mine)).isEqualTo("Freezer 1");
      assertThat(details.equipmentNameOf(theirs)).isEqualTo("Chamber");
      assertThat(details.siteNameOf(theirs)).isEqualTo("Callao");
      assertThat((Collection<Long>) details.reviewedServiceRequestIds()).isEmpty();
    }
  }

  @Nested
  class EquipmentInterventions {

    @Test
    @DisplayName("handle(ListEquipmentInterventionsQuery as OWNER) -> completed requests when the owner owns the unit (AAA)")
    void owner() {
      // Arrange
      asOwner();
      doReturn(Optional.of(new EquipmentRef(EQUIPMENT_ID, OWNER_ID, SITE_ID))).when(equipment).fetchOwnedEquipment(OWNER_ID, EQUIPMENT_ID);
      var done = List.of(completed());
      doReturn(done).when(requests).findCompletedByEquipmentId(new EquipmentId(EQUIPMENT_ID));

      // Act & Assert
      assertThat(service.handle(new ListEquipmentInterventionsQuery(EQUIPMENT_ID, USERNAME, CallerRole.OWNER))).contains(done);
    }

    @Test
    @DisplayName("handle(ListEquipmentInterventionsQuery as OWNER) for a unit not owned or no profile -> empty (AAA)")
    void ownerNotReachable() {
      // Arrange
      asOwner();
      doReturn(Optional.empty()).when(equipment).fetchOwnedEquipment(OWNER_ID, EQUIPMENT_ID);
      doReturn(Optional.empty()).when(profiles).fetchOwnerIdByUsername("nobody");

      // Act & Assert
      assertThat(service.handle(new ListEquipmentInterventionsQuery(EQUIPMENT_ID, USERNAME, CallerRole.OWNER))).isEmpty();
      assertThat(service.handle(new ListEquipmentInterventionsQuery(EQUIPMENT_ID, "nobody", CallerRole.OWNER))).isEmpty();
      verify(requests, times(0)).findCompletedByEquipmentId(any());
    }

    @Test
    @DisplayName("handle(ListEquipmentInterventionsQuery as TECHNICIAN) -> allowed with an engaged assignment (any non-canceled status) (AAA)")
    void technician() {
      // Arrange
      asTechnician();
      var engaged = Set.of(ServiceStatus.PENDING, ServiceStatus.ACCEPTED, ServiceStatus.IN_PROGRESS, ServiceStatus.COMPLETED);
      doReturn(true).when(requests).existsByEquipmentIdAndTechnicianIdAndStatusIn(
          new EquipmentId(EQUIPMENT_ID), new TechnicianProfileId(TECHNICIAN_ID), engaged);
      doReturn(List.of()).when(requests).findCompletedByEquipmentId(new EquipmentId(EQUIPMENT_ID));

      // Act & Assert
      assertThat(service.handle(new ListEquipmentInterventionsQuery(EQUIPMENT_ID, USERNAME, CallerRole.TECHNICIAN))).contains(List.of());
    }

    @Test
    @DisplayName("handle(ListEquipmentInterventionsQuery as TECHNICIAN) without any engagement -> empty (AAA)")
    void technicianNotEngaged() {
      // Arrange
      asTechnician();
      doReturn(false).when(requests).existsByEquipmentIdAndTechnicianIdAndStatusIn(any(), any(), anySet());

      // Act & Assert
      assertThat(service.handle(new ListEquipmentInterventionsQuery(EQUIPMENT_ID, USERNAME, CallerRole.TECHNICIAN))).isEmpty();
    }

    @Test
    @DisplayName("handle(ListEquipmentInterventionsQuery with null / non-positive equipment id) -> empty, no lookups (AAA)")
    void invalidId() {
      // Act & Assert
      assertThat(service.handle(new ListEquipmentInterventionsQuery(null, USERNAME, CallerRole.OWNER))).isEmpty();
      assertThat(service.handle(new ListEquipmentInterventionsQuery(0L, USERNAME, CallerRole.TECHNICIAN))).isEmpty();
      verifyNoInteractions(profiles, requests, equipment);
    }
  }
}
