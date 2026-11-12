package pe.edu.upc.ice.track.platform.notifications.domain.model.queries;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pe.edu.upc.ice.track.platform.notifications.test.fixtures.NotificationsDomainFixtures.RECIPIENT_ID;

class NotificationsQueriesTest {

  @Test
  @DisplayName("new GetNotificationsByRecipientQuery(valid) -> keeps its values (AAA)")
  void keepsValues() {
    // Arrange & Act
    var query = new GetNotificationsByRecipientQuery(RECIPIENT_ID, true, NotificationFilter.NONE, 2, 30);

    // Assert
    assertThat(query.recipientUserId()).isEqualTo(RECIPIENT_ID);
    assertThat(query.unreadOnly()).isTrue();
    assertThat(query.page()).isEqualTo(2);
    assertThat(query.size()).isEqualTo(30);
  }

  @Test
  @DisplayName("new GetNotificationsByRecipientQuery(null filter) -> defaults to NONE (AAA)")
  void nullFilterDefaultsToNone() {
    // Arrange & Act & Assert
    assertThat(new GetNotificationsByRecipientQuery(RECIPIENT_ID, false, null, 0, 20).filter())
        .isSameAs(NotificationFilter.NONE);
  }

  @Test
  @DisplayName("new GetNotificationsByRecipientQuery(size above MAX_PAGE_SIZE) -> capped (AAA)")
  void sizeIsCapped() {
    // Arrange & Act
    var query = new GetNotificationsByRecipientQuery(RECIPIENT_ID, false, null, 0, 5000);
    var atLimit = new GetNotificationsByRecipientQuery(RECIPIENT_ID, false, null, 0,
        GetNotificationsByRecipientQuery.MAX_PAGE_SIZE);

    // Assert
    assertThat(query.size()).isEqualTo(GetNotificationsByRecipientQuery.MAX_PAGE_SIZE);
    assertThat(atLimit.size()).isEqualTo(100);
  }

  @Test
  @DisplayName("new GetNotificationsByRecipientQuery(null recipient) -> IllegalArgumentException (AAA)")
  void rejectsNullRecipient() {
    // Arrange & Act & Assert
    assertThatThrownBy(() -> new GetNotificationsByRecipientQuery(null, false, null, 0, 20))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("new GetNotificationsByRecipientQuery(negative page) -> IllegalArgumentException (AAA)")
  void rejectsNegativePage() {
    // Arrange & Act & Assert
    assertThatThrownBy(() -> new GetNotificationsByRecipientQuery(RECIPIENT_ID, false, null, -1, 20))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("new GetNotificationsByRecipientQuery(size < 1) -> IllegalArgumentException (AAA)")
  void rejectsNonPositiveSize() {
    // Arrange & Act & Assert
    assertThatThrownBy(() -> new GetNotificationsByRecipientQuery(RECIPIENT_ID, false, null, 0, 0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("new GetUnreadNotificationCountQuery(null recipient) -> IllegalArgumentException (AAA)")
  void unreadCountRejectsNullRecipient() {
    // Arrange & Act & Assert
    assertThatThrownBy(() -> new GetUnreadNotificationCountQuery(null)).isInstanceOf(IllegalArgumentException.class);
    assertThat(new GetUnreadNotificationCountQuery(RECIPIENT_ID).recipientUserId()).isEqualTo(RECIPIENT_ID);
  }
}
