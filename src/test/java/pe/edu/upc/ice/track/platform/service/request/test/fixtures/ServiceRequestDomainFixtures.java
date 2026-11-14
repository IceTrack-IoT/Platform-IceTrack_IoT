package pe.edu.upc.ice.track.platform.service.request.test.fixtures;

import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.EdgeGrant;
import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.Review;
import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.ServiceRequest;
import pe.edu.upc.ice.track.platform.service.request.domain.model.entities.StatusTransition;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.*;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.EquipmentId;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.SiteId;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

public final class ServiceRequestDomainFixtures {

  public static final Long OWNER_ID = 5L;
  public static final Long OTHER_OWNER_ID = 6L;
  public static final Long TECHNICIAN_ID = 7L;
  public static final Long OTHER_TECHNICIAN_ID = 8L;
  public static final Long EQUIPMENT_ID = 10L;
  public static final Long SITE_ID = 3L;
  public static final Long REQUEST_ID = 77L;
  public static final Long REVIEW_ID = 40L;
  public static final Long GRANT_ID = 90L;
  public static final Long WINDOW_ID = 300L;
  public static final Long ALERT_ID = 55L;
  public static final String USERNAME = "atorres";
  public static final String DESCRIPTION = "Compressor makes a loud noise";
  public static final UUID CLIENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  public static final String DIGEST = "a".repeat(64);

  private ServiceRequestDomainFixtures() {
  }

  public static Instant now() {
    return Instant.now().truncatedTo(ChronoUnit.MICROS);
  }

  public static OwnerId owner() {
    return new OwnerId(OWNER_ID);
  }

  public static TechnicianProfileId technician() {
    return new TechnicianProfileId(TECHNICIAN_ID);
  }

  /** A persisted PENDING request with a single creation transition and no pending domain events. */
  public static ServiceRequest pending() {
    var createdAt = now().minusSeconds(7200);
    return new ServiceRequest(new ServiceRequestId(REQUEST_ID), owner(), new EquipmentId(EQUIPMENT_ID),
        new SiteId(SITE_ID), null, ServiceType.REPAIR, ServicePriority.HIGH, DESCRIPTION, null,
        ServiceStatus.PENDING, createdAt, null, null, null, null, null, 1L,
        List.of(StatusTransition.of(null, ServiceStatus.PENDING, OWNER_ID, createdAt, null, null)), null, List.of());
  }

  public static ServiceRequest assigned() {
    var request = pending();
    request.assignTechnician(technician(), owner());
    request.clearDomainEvents();
    return request;
  }

  public static ServiceRequest accepted() {
    var request = assigned();
    request.accept(technician());
    request.clearDomainEvents();
    return request;
  }

  public static ServiceRequest inProgress() {
    var request = accepted();
    request.start(technician(), now().minusSeconds(3600));
    request.clearDomainEvents();
    return request;
  }

  public static ServiceRequest completed() {
    var request = inProgress();
    request.registerIntervention(technician(), CLIENT_ID, "Worn bearing", "Replaced bearing",
        List.of("bearing"), List.of("photo-1.jpg"), now().minusSeconds(60));
    request.clearDomainEvents();
    return request;
  }

  public static ServiceRequest canceled() {
    var request = assigned();
    request.cancel(owner(), "No longer needed");
    request.clearDomainEvents();
    return request;
  }

  public static ReviewRating rating() {
    return new ReviewRating(5, 4, 3);
  }

  /** A persisted review submitted now over the completed request. */
  public static Review review() {
    var submitted = Review.submit(completed(), owner(), rating(), "Good job", now());
    return new Review(new ReviewId(REVIEW_ID), submitted.getServiceRequestId(), submitted.getOwnerId(),
        submitted.getTechnicianId(), submitted.getRating(), submitted.getComment(), submitted.getCreatedAt(),
        submitted.getEditableUntil());
  }

  public static EdgeGrant grant() {
    var issuedAt = now();
    return new EdgeGrant(GRANT_ID, REQUEST_ID, TECHNICIAN_ID, EQUIPMENT_ID, DIGEST, issuedAt,
        issuedAt.plusSeconds(86400), null, null);
  }
}
