package pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pe.edu.upc.ice.track.platform.notifications.test.fixtures.NotificationsDomainFixtures.notification;

class NotificationsValueObjectsTest {

  @Nested
  class Identifiers {

    @Test
    @DisplayName("new AlertId(id) -> wraps the id (AAA)")
    void alertIdWrapsValue() {
      // Arrange & Act & Assert
      assertThat(new AlertId(5L).alertId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("new AlertId(null) -> IllegalArgumentException (AAA)")
    void alertIdRejectsNull() {
      // Arrange & Act & Assert
      assertThatThrownBy(() -> new AlertId(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("new DeviceId(id) -> wraps the id (AAA)")
    void deviceIdWrapsValue() {
      // Arrange & Act & Assert
      assertThat(new DeviceId(7L).deviceId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("new DeviceId(null) -> IllegalArgumentException (AAA)")
    void deviceIdRejectsNull() {
      // Arrange & Act & Assert
      assertThatThrownBy(() -> new DeviceId(null)).isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  class BulkReadResultTest {

    @Test
    @DisplayName("new BulkReadResult(updated, unread) -> keeps both counts (AAA)")
    void keepsCounts() {
      // Arrange & Act
      var result = new BulkReadResult(3L, 2L);

      // Assert
      assertThat(result.updatedCount()).isEqualTo(3L);
      assertThat(result.unreadCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("new BulkReadResult(negative count) -> IllegalArgumentException (AAA)")
    void rejectsNegativeCounts() {
      // Arrange & Act & Assert
      assertThatThrownBy(() -> new BulkReadResult(-1L, 0L)).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new BulkReadResult(0L, -1L)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("BulkReadResult.nothing() -> zero updated and zero unread (AAA)")
    void nothingIsZero() {
      // Arrange & Act & Assert
      assertThat(BulkReadResult.nothing()).isEqualTo(new BulkReadResult(0L, 0L));
    }
  }

  @Nested
  class MessageTemplateTest {

    @ParameterizedTest(name = "[{index}] key <{0}>")
    @MethodSource("pe.edu.upc.ice.track.platform.shared.test.fixtures.CommonCommandFixtures#invalidBlankStrings")
    @DisplayName("new MessageTemplate(blank key) -> IllegalArgumentException (AAA)")
    void rejectsBlankKey(String blank) {
      // Arrange & Act & Assert
      assertThatThrownBy(() -> new MessageTemplate(blank, Map.of())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("new MessageTemplate(key, params with nulls) -> drops null names and values, keeps order (AAA)")
    void dropsNullEntries() {
      // Arrange
      var params = new HashMap<String, Object>();
      params.put("kept", 1);
      params.put("nullValue", null);
      params.put(null, "nullName");

      // Act
      var template = new MessageTemplate("k", params);

      // Assert
      assertThat(template.params()).containsOnlyKeys("kept").containsEntry("kept", 1);
    }

    @Test
    @DisplayName("new MessageTemplate(key, null params) -> empty params (AAA)")
    void nullParamsBecomeEmpty() {
      // Arrange & Act & Assert
      assertThat(new MessageTemplate("k", null).params()).isEmpty();
    }

    @Test
    @DisplayName("params() -> unmodifiable and detached from the source map (AAA)")
    void paramsAreImmutable() {
      // Arrange
      var source = new HashMap<String, Object>(Map.of("a", 1));
      var template = new MessageTemplate("k", source);
      source.put("b", 2);

      // Act & Assert
      assertThat(template.params()).containsOnlyKeys("a");
      assertThatThrownBy(() -> template.params().put("c", 3)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("untemplated() -> the notification.untemplated key without params (AAA)")
    void untemplatedHasReservedKey() {
      // Arrange & Act
      var template = MessageTemplate.untemplated();

      // Assert
      assertThat(template.key()).isEqualTo(MessageTemplate.UNTEMPLATED);
      assertThat(template.params()).isEmpty();
    }

    @Test
    @DisplayName("of(key).with(...).build() -> builds the template and skips null values (AAA)")
    void builderBuildsTemplate() {
      // Arrange & Act
      var template = MessageTemplate.of(MessageTemplate.SERVICE_REQUEST_ASSIGNED)
          .with("service_request_id", 77L)
          .with("equipment_name", null)
          .with("technician_name", "Ana")
          .build();

      // Assert
      assertThat(template.key()).isEqualTo("service_request.assigned");
      assertThat(template.params()).containsOnlyKeys("service_request_id", "technician_name");
    }

    @Test
    @DisplayName("of(null) -> NullPointerException (AAA)")
    void builderRejectsNullKey() {
      // Arrange & Act & Assert
      assertThatThrownBy(() -> MessageTemplate.of(null)).isInstanceOf(NullPointerException.class);
    }
  }

  @Nested
  class NotificationFilterTest {

    @Test
    @DisplayName("new NotificationFilter(null, null, null) -> empty sets (AAA)")
    void nullSetsBecomeEmpty() {
      // Arrange & Act
      var filter = new NotificationFilter(null, null, null);

      // Assert
      assertThat(filter.severities()).isEmpty();
      assertThat(filter.types()).isEmpty();
      assertThat(filter.equipmentId()).isNull();
    }

    @Test
    @DisplayName("new NotificationFilter(sets) -> defensive immutable copies (AAA)")
    void setsAreCopied() {
      // Arrange
      var severities = new java.util.HashSet<>(Set.of(NotificationSeverity.CRITICAL));

      // Act
      var filter = new NotificationFilter(severities, Set.of(NotificationType.DEVICE_OFFLINE), 10L);
      severities.add(NotificationSeverity.INFO);

      // Assert
      assertThat(filter.severities()).containsExactly(NotificationSeverity.CRITICAL);
      assertThat(filter.types()).containsExactly(NotificationType.DEVICE_OFFLINE);
      assertThat(filter.equipmentId()).isEqualTo(10L);
      assertThatThrownBy(() -> filter.severities().add(NotificationSeverity.INFO))
          .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("NotificationFilter.NONE -> filters nothing (AAA)")
    void noneFiltersNothing() {
      // Arrange & Act & Assert
      assertThat(NotificationFilter.NONE.severities()).isEmpty();
      assertThat(NotificationFilter.NONE.types()).isEmpty();
      assertThat(NotificationFilter.NONE.equipmentId()).isNull();
    }
  }

  @Nested
  class NotificationPageTest {

    @Test
    @DisplayName("new NotificationPage(content, page, size, total) -> immutable page (AAA)")
    void buildsPage() {
      // Arrange
      var content = new java.util.ArrayList<>(List.of(notification()));

      // Act
      var page = new NotificationPage(content, 1, 20, 41);
      content.clear();

      // Assert
      assertThat(page.content()).hasSize(1);
      assertThat(page.page()).isEqualTo(1);
      assertThat(page.size()).isEqualTo(20);
      assertThat(page.totalElements()).isEqualTo(41);
      assertThat(page.totalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("new NotificationPage(null content) -> IllegalArgumentException (AAA)")
    void rejectsNullContent() {
      // Arrange & Act & Assert
      assertThatThrownBy(() -> new NotificationPage(null, 0, 20, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("new NotificationPage(negative page) -> IllegalArgumentException (AAA)")
    void rejectsNegativePage() {
      // Arrange & Act & Assert
      assertThatThrownBy(() -> new NotificationPage(List.of(), -1, 20, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("new NotificationPage(size < 1) -> IllegalArgumentException (AAA)")
    void rejectsNonPositiveSize() {
      // Arrange & Act & Assert
      assertThatThrownBy(() -> new NotificationPage(List.of(), 0, 0, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("NotificationPage.empty(page, size) -> no content, one total page (AAA)")
    void emptyPage() {
      // Arrange & Act
      var page = NotificationPage.empty(2, 10);

      // Assert
      assertThat(page.content()).isEmpty();
      assertThat(page.page()).isEqualTo(2);
      assertThat(page.totalElements()).isZero();
      assertThat(page.totalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("totalPages() -> rounds up and is never below 1 (AAA)")
    void totalPagesRoundsUp() {
      // Arrange & Act & Assert
      assertThat(new NotificationPage(List.of(), 0, 10, 10).totalPages()).isEqualTo(1);
      assertThat(new NotificationPage(List.of(), 0, 10, 11).totalPages()).isEqualTo(2);
      assertThat(new NotificationPage(List.of(), 0, 10, 0).totalPages()).isEqualTo(1);
    }
  }
}
