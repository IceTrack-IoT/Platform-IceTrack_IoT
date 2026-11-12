package pe.edu.upc.ice.track.platform.notifications.application.internal.queryservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.notifications.domain.model.queries.GetNotificationByIdQuery;
import pe.edu.upc.ice.track.platform.notifications.domain.model.queries.GetNotificationsByRecipientQuery;
import pe.edu.upc.ice.track.platform.notifications.domain.model.queries.GetUnreadNotificationCountQuery;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationFilter;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationPage;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationSeverity;
import pe.edu.upc.ice.track.platform.notifications.domain.repositories.NotificationRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.notifications.test.fixtures.NotificationsDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class NotificationQueryServiceImplTest {

  @Mock
  private NotificationRepository notificationRepository;

  @InjectMocks
  private NotificationQueryServiceImpl service;

  @Test
  @DisplayName("handle(GetNotificationByIdQuery) addressed to the caller -> returns it (AAA)")
  void byIdAddressedToCaller() {
    // Arrange
    var notification = persistedNotification();
    when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));

    // Act
    var result = service.handle(new GetNotificationByIdQuery(NOTIFICATION_ID, RECIPIENT_ID));

    // Assert
    assertThat(result).containsSame(notification);
  }

  @Test
  @DisplayName("handle(GetNotificationByIdQuery) of another recipient -> empty (AAA)")
  void byIdOfAnotherRecipient() {
    // Arrange
    when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(persistedNotification()));

    // Act
    var result = service.handle(new GetNotificationByIdQuery(NOTIFICATION_ID, OTHER_RECIPIENT_ID));

    // Assert
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("handle(GetNotificationByIdQuery) unknown id -> empty (AAA)")
  void byIdUnknown() {
    // Arrange
    when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.empty());

    // Act & Assert
    assertThat(service.handle(new GetNotificationByIdQuery(NOTIFICATION_ID, RECIPIENT_ID))).isEmpty();
  }

  @Test
  @DisplayName("handle(GetNotificationsByRecipientQuery) -> delegates every criterion to the repository page (AAA)")
  void listDelegates() {
    // Arrange
    var filter = new NotificationFilter(Set.of(NotificationSeverity.CRITICAL), Set.of(), 10L);
    var page = new NotificationPage(List.of(persistedNotification()), 2, 5, 11L);
    when(notificationRepository.findActivePageByRecipientUserId(RECIPIENT_ID, true, filter, 2, 5)).thenReturn(page);

    // Act
    var result = service.handle(new GetNotificationsByRecipientQuery(RECIPIENT_ID, true, filter, 2, 5));

    // Assert
    assertThat(result).isSameAs(page);
    verify(notificationRepository, times(1)).findActivePageByRecipientUserId(RECIPIENT_ID, true, filter, 2, 5);
  }

  @Test
  @DisplayName("handle(GetNotificationsByRecipientQuery) oversized page -> repository receives the capped size (AAA)")
  void listCapsPageSize() {
    // Arrange
    var page = NotificationPage.empty(0, 100);
    when(notificationRepository.findActivePageByRecipientUserId(RECIPIENT_ID, false, NotificationFilter.NONE, 0, 100))
        .thenReturn(page);

    // Act
    var result = service.handle(new GetNotificationsByRecipientQuery(RECIPIENT_ID, false, null, 0, 500));

    // Assert
    assertThat(result).isSameAs(page);
  }

  @Test
  @DisplayName("handle(GetUnreadNotificationCountQuery) -> returns the repository count (AAA)")
  void unreadCount() {
    // Arrange
    when(notificationRepository.countActiveUnreadByRecipientUserId(RECIPIENT_ID)).thenReturn(7L);

    // Act
    var result = service.handle(new GetUnreadNotificationCountQuery(RECIPIENT_ID));

    // Assert
    assertThat(result).isEqualTo(7L);
  }
}
