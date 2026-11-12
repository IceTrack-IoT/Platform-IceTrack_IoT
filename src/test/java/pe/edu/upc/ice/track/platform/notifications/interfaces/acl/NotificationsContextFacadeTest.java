package pe.edu.upc.ice.track.platform.notifications.interfaces.acl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.notifications.application.queryservices.NotificationQueryService;
import pe.edu.upc.ice.track.platform.notifications.domain.model.queries.GetUnreadNotificationCountQuery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.notifications.test.fixtures.NotificationsDomainFixtures.RECIPIENT_ID;

@ExtendWith(MockitoExtension.class)
class NotificationsContextFacadeTest {

  @Mock
  private NotificationQueryService notificationQueryService;

  @InjectMocks
  private NotificationsContextFacade facade;

  @Test
  @DisplayName("countUnreadNotificationsByRecipient(null) -> 0 without querying (AAA)")
  void nullRecipient() {
    // Act & Assert
    assertThat(facade.countUnreadNotificationsByRecipient(null)).isZero();
    verifyNoInteractions(notificationQueryService);
  }

  @Test
  @DisplayName("countUnreadNotificationsByRecipient(Long) -> unread count of the recipient (AAA)")
  void countsUnread() {
    // Arrange
    when(notificationQueryService.handle(new GetUnreadNotificationCountQuery(RECIPIENT_ID))).thenReturn(5L);

    // Act & Assert
    assertThat(facade.countUnreadNotificationsByRecipient(RECIPIENT_ID)).isEqualTo(5);
    verify(notificationQueryService).handle(new GetUnreadNotificationCountQuery(RECIPIENT_ID));
  }

  @Test
  @DisplayName("countUnreadNotificationsByRecipient(Long) beyond int range -> saturates at Integer.MAX_VALUE (AAA)")
  void saturates() {
    // Arrange
    when(notificationQueryService.handle(new GetUnreadNotificationCountQuery(RECIPIENT_ID)))
        .thenReturn(Integer.MAX_VALUE + 10L);

    // Act & Assert
    assertThat(facade.countUnreadNotificationsByRecipient(RECIPIENT_ID)).isEqualTo(Integer.MAX_VALUE);
  }
}
