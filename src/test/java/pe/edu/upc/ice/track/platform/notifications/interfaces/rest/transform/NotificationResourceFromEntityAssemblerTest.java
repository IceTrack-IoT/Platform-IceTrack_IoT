package pe.edu.upc.ice.track.platform.notifications.interfaces.rest.transform;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationPage;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationSeverity;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationType;
import pe.edu.upc.ice.track.platform.notifications.interfaces.rest.resources.NotificationSeverityResource;
import pe.edu.upc.ice.track.platform.notifications.interfaces.rest.resources.NotificationTypeResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static pe.edu.upc.ice.track.platform.notifications.test.fixtures.NotificationsDomainFixtures.*;

class NotificationResourceFromEntityAssemblerTest {

  @Test
  @DisplayName("toResourceFromEntity(Notification) -> maps ids, texts, enums names and timestamps (AAA)")
  void mapsUnreadNotification() {
    // Arrange
    var notification = persistedNotification();

    // Act
    var resource = NotificationResourceFromEntityAssembler.toResourceFromEntity(notification);

    // Assert
    assertThat(resource.id()).isEqualTo(NOTIFICATION_ID);
    assertThat(resource.recipientUserId()).isEqualTo(RECIPIENT_ID);
    assertThat(resource.equipmentId()).isEqualTo(EQUIPMENT_ID);
    assertThat(resource.sourceAlertId()).isEqualTo(ALERT_ID);
    assertThat(resource.message()).isEqualTo(MESSAGE);
    assertThat(resource.messageKey()).isEqualTo(notification.getTemplate().key());
    assertThat(resource.messageParams()).isEqualTo(notification.getTemplate().params());
    assertThat(resource.type()).isEqualTo("TEMPERATURE_EXCURSION");
    assertThat(resource.severity()).isEqualTo("CRITICAL");
    assertThat(resource.isRead()).isFalse();
    assertThat(resource.createdAt()).isEqualTo(notification.getCreatedAt());
    assertThat(resource.readAt()).isNull();
    assertThat(resource.dismissedAt()).isNull();
  }

  @Test
  @DisplayName("toResourceFromEntity(read Notification) -> exposes read flag and timestamp (AAA)")
  void mapsReadNotification() {
    // Arrange
    var notification = readNotification();

    // Act
    var resource = NotificationResourceFromEntityAssembler.toResourceFromEntity(notification);

    // Assert
    assertThat(resource.isRead()).isTrue();
    assertThat(resource.readAt()).isEqualTo(notification.getReadAt()).isNotNull();
  }

  @Test
  @DisplayName("toPagedResourceFromPage(NotificationPage) -> maps content and paging metadata (AAA)")
  void mapsPage() {
    // Arrange
    var page = new NotificationPage(List.of(persistedNotification(), readNotification()), 1, 2, 5L);

    // Act
    var resource = NotificationResourceFromEntityAssembler.toPagedResourceFromPage(page);

    // Assert
    assertThat(resource.items()).hasSize(2);
    assertThat(resource.page()).isEqualTo(1);
    assertThat(resource.size()).isEqualTo(2);
    assertThat(resource.totalElements()).isEqualTo(5L);
    assertThat(resource.totalPages()).isEqualTo(3);
  }

  @Test
  @DisplayName("toPagedResourceFromPage(empty page) -> empty content, one page (AAA)")
  void mapsEmptyPage() {
    // Act
    var resource = NotificationResourceFromEntityAssembler.toPagedResourceFromPage(NotificationPage.empty(0, 20));

    // Assert
    assertThat(resource.items()).isEmpty();
    assertThat(resource.totalPages()).isEqualTo(1);
  }

  @Test
  @DisplayName("NotificationSeverityResource.toDomain() -> same-named domain severity for every value (AAA)")
  void severityResourceToDomain() {
    for (var value : NotificationSeverityResource.values()) {
      assertThat(value.toDomain()).isEqualTo(NotificationSeverity.valueOf(value.name()));
    }
  }

  @Test
  @DisplayName("NotificationTypeResource.toDomain() -> same-named domain type for every value (AAA)")
  void typeResourceToDomain() {
    for (var value : NotificationTypeResource.values()) {
      assertThat(value.toDomain()).isEqualTo(NotificationType.valueOf(value.name()));
    }
    assertThat(NotificationTypeResource.values()).hasSameSizeAs(NotificationType.values());
  }
}
