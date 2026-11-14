package pe.edu.upc.ice.track.platform.service.request.application.internal.queryservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.service.request.application.internal.outboundservices.acl.ExternalUserProfileService;
import pe.edu.upc.ice.track.platform.service.request.application.internal.queryservices.ReviewQueryServiceImpl;
import pe.edu.upc.ice.track.platform.service.request.domain.model.queries.GetReviewByServiceRequestIdQuery;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceRequestId;
import pe.edu.upc.ice.track.platform.service.request.domain.repositories.ReviewRepository;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.CallerRole;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyNoInteractions;
import static pe.edu.upc.ice.track.platform.service.request.test.fixtures.ServiceRequestDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class ReviewQueryServiceImplTest {

  private static final ServiceRequestId REQUEST = new ServiceRequestId(REQUEST_ID);

  @Mock private ReviewRepository reviews;
  @Mock private ExternalUserProfileService profiles;
  @InjectMocks private ReviewQueryServiceImpl service;

  @Test
  @DisplayName("handle(query as OWNER) -> the review when the caller wrote it (AAA)")
  void ownerSeesOwnReview() {
    // Arrange
    var review = review();
    doReturn(Optional.of(OWNER_ID)).when(profiles).fetchOwnerIdByUsername(USERNAME);
    doReturn(Optional.of(review)).when(reviews).findByServiceRequestId(REQUEST);

    // Act & Assert
    assertThat(service.handle(new GetReviewByServiceRequestIdQuery(REQUEST_ID, USERNAME, CallerRole.OWNER))).containsSame(review);
  }

  @Test
  @DisplayName("handle(query as OWNER) -> empty for a review of another owner or an account without owner profile (AAA)")
  void ownerDoesNotSeeForeign() {
    // Arrange
    doReturn(Optional.of(OTHER_OWNER_ID)).when(profiles).fetchOwnerIdByUsername(USERNAME);
    doReturn(Optional.of(review())).when(reviews).findByServiceRequestId(REQUEST);
    doReturn(Optional.empty()).when(profiles).fetchOwnerIdByUsername("nobody");

    // Act & Assert
    assertThat(service.handle(new GetReviewByServiceRequestIdQuery(REQUEST_ID, USERNAME, CallerRole.OWNER))).isEmpty();
    assertThat(service.handle(new GetReviewByServiceRequestIdQuery(REQUEST_ID, "nobody", CallerRole.OWNER))).isEmpty();
  }

  @Test
  @DisplayName("handle(query as TECHNICIAN) -> the review when it concerns the caller, empty otherwise (AAA)")
  void technicianView() {
    // Arrange
    var review = review();
    doReturn(Optional.of(TECHNICIAN_ID)).when(profiles).fetchTechnicianIdByUsername(USERNAME);
    doReturn(Optional.of(OTHER_TECHNICIAN_ID)).when(profiles).fetchTechnicianIdByUsername("other");
    doReturn(Optional.of(review)).when(reviews).findByServiceRequestId(REQUEST);

    // Act & Assert
    assertThat(service.handle(new GetReviewByServiceRequestIdQuery(REQUEST_ID, USERNAME, CallerRole.TECHNICIAN))).containsSame(review);
    assertThat(service.handle(new GetReviewByServiceRequestIdQuery(REQUEST_ID, "other", CallerRole.TECHNICIAN))).isEmpty();
  }

  @Test
  @DisplayName("handle(query with null or non-positive request id) -> empty without any lookup (AAA)")
  void invalidId() {
    // Act & Assert
    assertThat(service.handle(new GetReviewByServiceRequestIdQuery(null, USERNAME, CallerRole.OWNER))).isEmpty();
    assertThat(service.handle(new GetReviewByServiceRequestIdQuery(0L, USERNAME, CallerRole.TECHNICIAN))).isEmpty();
    verifyNoInteractions(profiles, reviews);
  }
}
