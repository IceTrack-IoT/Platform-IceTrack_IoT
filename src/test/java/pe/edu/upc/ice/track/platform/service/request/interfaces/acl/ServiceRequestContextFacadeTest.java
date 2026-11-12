package pe.edu.upc.ice.track.platform.service.request.interfaces.acl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.*;
import pe.edu.upc.ice.track.platform.service.request.domain.repositories.EdgeGrantRepository;
import pe.edu.upc.ice.track.platform.service.request.domain.repositories.ReviewRepository;
import pe.edu.upc.ice.track.platform.service.request.domain.repositories.ServiceRequestRepository;
import pe.edu.upc.ice.track.platform.service.request.interfaces.acl.ActiveEdgeGrant;
import pe.edu.upc.ice.track.platform.service.request.interfaces.acl.InProgressServiceRequest;
import pe.edu.upc.ice.track.platform.service.request.interfaces.acl.ServiceRequestContextFacade;
import pe.edu.upc.ice.track.platform.service.request.interfaces.acl.ServiceRequestStatusCount;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.EquipmentId;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.service.request.test.fixtures.ServiceRequestDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class ServiceRequestContextFacadeTest {

  private static final Instant FROM = Instant.parse("2026-11-01T00:00:00Z");
  private static final Instant TO = Instant.parse("2026-11-30T00:00:00Z");
  private static final EnumSet<ServiceStatus> ACTIVE =
      EnumSet.of(ServiceStatus.PENDING, ServiceStatus.ACCEPTED, ServiceStatus.IN_PROGRESS);

  @Mock private ServiceRequestRepository requests;
  @Mock private ReviewRepository reviews;
  @Mock private EdgeGrantRepository grants;
  @InjectMocks private ServiceRequestContextFacade facade;

  @Nested
  class EdgeGrants {

    @Test
    @DisplayName("fetchActiveEdgeGrants(equipmentId) -> grants projected to primitives (AAA)")
    void active() {
      // Arrange
      var grant = grant();
      doReturn(List.of(grant)).when(grants).findActiveByEquipmentId(eq(EQUIPMENT_ID), any(Instant.class));

      // Act
      var result = facade.fetchActiveEdgeGrants(EQUIPMENT_ID);

      // Assert
      assertThat(result).containsExactly(new ActiveEdgeGrant(DIGEST, TECHNICIAN_ID, REQUEST_ID, grant.getExpiresAt()));
    }

    @Test
    @DisplayName("fetchActiveEdgeGrants(null / 0) -> empty without querying (AAA)")
    void invalid() {
      // Act & Assert
      assertThat(facade.fetchActiveEdgeGrants(null)).isEmpty();
      assertThat(facade.fetchActiveEdgeGrants(0L)).isEmpty();
      verifyNoInteractions(grants);
    }
  }

  @Nested
  class Metrics {

    @Test
    @DisplayName("fetchEquipmentIdsWithCompletedPreventiveMaintenanceInRange -> plain ids of preventive completions (AAA)")
    void preventive() {
      // Arrange
      doReturn(List.of(new EquipmentId(EQUIPMENT_ID), new EquipmentId(11L)))
          .when(requests).findEquipmentIdsCompletedBySiteIdAndTypeInRange(SITE_ID, ServiceType.PREVENTIVE_MAINTENANCE, FROM, TO);

      // Act & Assert
      assertThat(facade.fetchEquipmentIdsWithCompletedPreventiveMaintenanceInRange(SITE_ID, FROM, TO))
          .containsExactly(EQUIPMENT_ID, 11L);
    }

    @Test
    @DisplayName("fetchEquipmentIdsWithCompletedPreventiveMaintenanceInRange -> empty for null site or invalid window (AAA)")
    void preventiveInvalid() {
      // Act & Assert
      assertThat(facade.fetchEquipmentIdsWithCompletedPreventiveMaintenanceInRange(null, FROM, TO)).isEmpty();
      assertThat(facade.fetchEquipmentIdsWithCompletedPreventiveMaintenanceInRange(SITE_ID, null, TO)).isEmpty();
      assertThat(facade.fetchEquipmentIdsWithCompletedPreventiveMaintenanceInRange(SITE_ID, FROM, null)).isEmpty();
      assertThat(facade.fetchEquipmentIdsWithCompletedPreventiveMaintenanceInRange(SITE_ID, TO, FROM)).isEmpty();
      verifyNoInteractions(requests);
    }

    @Test
    @DisplayName("fetchEquipmentIdsWithCompletedPreventiveMaintenanceInRange -> an instant window (from == to) is valid (AAA)")
    void preventiveInstantWindow() {
      // Arrange
      doReturn(List.of()).when(requests).findEquipmentIdsCompletedBySiteIdAndTypeInRange(SITE_ID, ServiceType.PREVENTIVE_MAINTENANCE, FROM, FROM);

      // Act
      facade.fetchEquipmentIdsWithCompletedPreventiveMaintenanceInRange(SITE_ID, FROM, FROM);

      // Assert
      verify(requests, times(1)).findEquipmentIdsCompletedBySiteIdAndTypeInRange(SITE_ID, ServiceType.PREVENTIVE_MAINTENANCE, FROM, FROM);
    }

    @Test
    @DisplayName("countCompletedServiceRequestsByTechnicianInRange -> repository count; invalid input -> 0 (AAA)")
    void count() {
      // Arrange
      doReturn(4L).when(requests).countCompletedByTechnicianIdInRange(new TechnicianProfileId(TECHNICIAN_ID), FROM, TO);

      // Act & Assert
      assertThat(facade.countCompletedServiceRequestsByTechnicianInRange(TECHNICIAN_ID, FROM, TO)).isEqualTo(4L);
      assertThat(facade.countCompletedServiceRequestsByTechnicianInRange(0L, FROM, TO)).isZero();
      assertThat(facade.countCompletedServiceRequestsByTechnicianInRange(TECHNICIAN_ID, TO, FROM)).isZero();
      assertThat(facade.countCompletedServiceRequestsByTechnicianInRange(TECHNICIAN_ID, null, TO)).isZero();
    }

    @Test
    @DisplayName("fetchAverageRatingByTechnician -> the average, null when none or id invalid (AAA)")
    void average() {
      // Arrange
      doReturn(Optional.of(4.5)).when(reviews).findAverageRatingByTechnicianId(new TechnicianProfileId(TECHNICIAN_ID));
      doReturn(Optional.empty()).when(reviews).findAverageRatingByTechnicianId(new TechnicianProfileId(OTHER_TECHNICIAN_ID));

      // Act & Assert
      assertThat(facade.fetchAverageRatingByTechnician(TECHNICIAN_ID)).isEqualTo(4.5);
      assertThat(facade.fetchAverageRatingByTechnician(OTHER_TECHNICIAN_ID)).isNull();
      assertThat(facade.fetchAverageRatingByTechnician(null)).isNull();
      assertThat(facade.fetchAverageRatingByTechnician(-1L)).isNull();
    }
  }

  @Nested
  class Assignments {

    @Test
    @DisplayName("hasActiveAssignmentOnEquipment -> repository answer over PENDING/ACCEPTED/IN_PROGRESS; invalid ids -> false (AAA)")
    void onEquipment() {
      // Arrange
      doReturn(true).when(requests).existsByEquipmentIdAndTechnicianIdAndStatusIn(
          new EquipmentId(EQUIPMENT_ID), new TechnicianProfileId(TECHNICIAN_ID), ACTIVE);

      // Act & Assert
      assertThat(facade.hasActiveAssignmentOnEquipment(TECHNICIAN_ID, EQUIPMENT_ID)).isTrue();
      assertThat(facade.hasActiveAssignmentOnEquipment(OTHER_TECHNICIAN_ID, EQUIPMENT_ID)).isFalse();
      assertThat(facade.hasActiveAssignmentOnEquipment(null, EQUIPMENT_ID)).isFalse();
      assertThat(facade.hasActiveAssignmentOnEquipment(TECHNICIAN_ID, 0L)).isFalse();
    }

    @Test
    @DisplayName("hasActiveAssignmentAtSite -> repository answer over active statuses; invalid ids -> false (AAA)")
    void atSite() {
      // Arrange
      doReturn(true).when(requests).existsBySiteIdAndTechnicianIdAndStatusIn(SITE_ID, new TechnicianProfileId(TECHNICIAN_ID), ACTIVE);

      // Act & Assert
      assertThat(facade.hasActiveAssignmentAtSite(TECHNICIAN_ID, SITE_ID)).isTrue();
      assertThat(facade.hasActiveAssignmentAtSite(TECHNICIAN_ID, null)).isFalse();
      assertThat(facade.hasActiveAssignmentAtSite(-5L, SITE_ID)).isFalse();
    }

    @Test
    @DisplayName("hasActiveEngagementBetween -> repository answer over active statuses; invalid ids -> false (AAA)")
    void between() {
      // Arrange
      doReturn(true).when(requests).existsByOwnerIdAndTechnicianIdAndStatusIn(
          new OwnerId(OWNER_ID), new TechnicianProfileId(TECHNICIAN_ID), ACTIVE);

      // Act & Assert
      assertThat(facade.hasActiveEngagementBetween(OWNER_ID, TECHNICIAN_ID)).isTrue();
      assertThat(facade.hasActiveEngagementBetween(null, TECHNICIAN_ID)).isFalse();
      assertThat(facade.hasActiveEngagementBetween(OWNER_ID, 0L)).isFalse();
    }

    @Test
    @DisplayName("findInProgressServiceRequestAt -> request and technician ids of the work in progress (AAA)")
    void inProgressAt() {
      // Arrange
      var at = now();
      doReturn(Optional.of(inProgress())).when(requests).findInProgressAt(new EquipmentId(EQUIPMENT_ID), at);

      // Act & Assert
      assertThat(facade.findInProgressServiceRequestAt(EQUIPMENT_ID, at))
          .contains(new InProgressServiceRequest(REQUEST_ID, TECHNICIAN_ID));
    }

    @Test
    @DisplayName("findInProgressServiceRequestAt -> empty for nothing found, null instant or invalid equipment (AAA)")
    void inProgressAtEmpty() {
      // Arrange
      var at = now();
      doReturn(Optional.empty()).when(requests).findInProgressAt(new EquipmentId(EQUIPMENT_ID), at);

      // Act & Assert
      assertThat(facade.findInProgressServiceRequestAt(EQUIPMENT_ID, at)).isEmpty();
      assertThat(facade.findInProgressServiceRequestAt(EQUIPMENT_ID, null)).isEmpty();
      assertThat(facade.findInProgressServiceRequestAt(0L, at)).isEmpty();
    }
  }

  @Nested
  class Counts {

    @Test
    @DisplayName("countServiceRequestsByStatus -> one entry per status with its active flag and count (AAA)")
    void perStatus() {
      // Arrange
      doReturn(3L).when(requests).countByOwnerIdAndStatus(new OwnerId(OWNER_ID), SITE_ID, ServiceStatus.PENDING);
      doReturn(0L).when(requests).countByOwnerIdAndStatus(new OwnerId(OWNER_ID), SITE_ID, ServiceStatus.ACCEPTED);
      doReturn(0L).when(requests).countByOwnerIdAndStatus(new OwnerId(OWNER_ID), SITE_ID, ServiceStatus.IN_PROGRESS);
      doReturn(8L).when(requests).countByOwnerIdAndStatus(new OwnerId(OWNER_ID), SITE_ID, ServiceStatus.COMPLETED);
      doReturn(1L).when(requests).countByOwnerIdAndStatus(new OwnerId(OWNER_ID), SITE_ID, ServiceStatus.CANCELED);

      // Act
      var counts = facade.countServiceRequestsByStatus(OWNER_ID, SITE_ID);

      // Assert
      assertThat(counts).hasSize(ServiceStatus.values().length);
      assertThat(counts).contains(
          new ServiceRequestStatusCount("PENDING", ServiceStatus.PENDING.isActive(), 3L),
          new ServiceRequestStatusCount("COMPLETED", ServiceStatus.COMPLETED.isActive(), 8L),
          new ServiceRequestStatusCount("CANCELED", ServiceStatus.CANCELED.isActive(), 1L));
    }

    @Test
    @DisplayName("countServiceRequestsByStatus(invalid owner) -> every status with count 0, repository untouched (AAA)")
    void invalidOwner() {
      // Act
      var counts = facade.countServiceRequestsByStatus(null, SITE_ID);

      // Assert
      assertThat(counts).hasSize(ServiceStatus.values().length).allMatch(c -> c.count() == 0L);
      verifyNoInteractions(requests);
    }
  }

  @Nested
  class InterventionRecord {

    @Test
    @DisplayName("fetchServiceInterventionRecord(id) -> record with history, intervention and review projected (AAA)")
    void full() {
      // Arrange
      var completed = completed();
      var review = review();
      doReturn(Optional.of(completed)).when(requests).findById(new ServiceRequestId(REQUEST_ID));
      doReturn(Optional.of(review)).when(reviews).findByServiceRequestId(new ServiceRequestId(REQUEST_ID));

      // Act
      var record = facade.fetchServiceInterventionRecord(REQUEST_ID).orElseThrow();

      // Assert
      assertThat(record.serviceRequestId()).isEqualTo(REQUEST_ID);
      assertThat(record.ownerProfileId()).isEqualTo(OWNER_ID);
      assertThat(record.equipmentId()).isEqualTo(EQUIPMENT_ID);
      assertThat(record.siteId()).isEqualTo(SITE_ID);
      assertThat(record.technicianProfileId()).isEqualTo(TECHNICIAN_ID);
      assertThat(record.type()).isEqualTo(completed.getType().name());
      assertThat(record.priority()).isEqualTo(completed.getPriority().name());
      assertThat(record.status()).isEqualTo("COMPLETED");
      assertThat(record.history()).hasSize(completed.getHistory().size());
      assertThat(record.history().get(0).fromStatus()).isNull();
      assertThat(record.history().get(record.history().size() - 1).toStatus()).isEqualTo("COMPLETED");
      assertThat(record.intervention().diagnosis()).isEqualTo(completed.getIntervention().getDiagnosis());
      assertThat(record.review().average()).isEqualTo(4.0);
      assertThat(record.review().communication()).isEqualTo(5);
      assertThat(record.review().efficacy()).isEqualTo(4);
      assertThat(record.review().performance()).isEqualTo(3);
      assertThat(record.review().comment()).isEqualTo("Good job");
    }

    @Test
    @DisplayName("fetchServiceInterventionRecord(id) -> intervention and review are null while missing; no technician allowed (AAA)")
    void withoutInterventionOrReview() {
      // Arrange
      doReturn(Optional.of(pending())).when(requests).findById(new ServiceRequestId(REQUEST_ID));
      doReturn(Optional.empty()).when(reviews).findByServiceRequestId(new ServiceRequestId(REQUEST_ID));

      // Act
      var record = facade.fetchServiceInterventionRecord(REQUEST_ID).orElseThrow();

      // Assert
      assertThat(record.technicianProfileId()).isNull();
      assertThat(record.intervention()).isNull();
      assertThat(record.review()).isNull();
      assertThat(record.status()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("fetchServiceInterventionRecord(id) -> empty when missing or id invalid (AAA)")
    void missing() {
      // Arrange
      doReturn(Optional.empty()).when(requests).findById(new ServiceRequestId(REQUEST_ID));

      // Act & Assert
      assertThat(facade.fetchServiceInterventionRecord(REQUEST_ID)).isEmpty();
      assertThat(facade.fetchServiceInterventionRecord(null)).isEmpty();
      assertThat(facade.fetchServiceInterventionRecord(0L)).isEmpty();
      verify(requests, times(1)).findById(any());
    }
  }
}
