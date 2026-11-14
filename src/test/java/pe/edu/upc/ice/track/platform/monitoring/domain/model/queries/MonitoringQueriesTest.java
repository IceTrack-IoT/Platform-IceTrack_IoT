package pe.edu.upc.ice.track.platform.monitoring.domain.model.queries;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertSeverity;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertStatus;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertType;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.TelemetryBucketSize;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.CallerRole;

import java.time.Duration;
import java.util.*;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static pe.edu.upc.ice.track.platform.monitoring.test.fixtures.MonitoringDomainFixtures.*;

class MonitoringQueriesTest {

  private static GetAlertsQuery alerts(Set<AlertStatus> statuses, Set<AlertType> types,
                                       Set<AlertSeverity> severities, int page, int size,
                                       java.time.Instant from, java.time.Instant to) {
    return new GetAlertsQuery(USERNAME, statuses, null, null, types, severities, from, to, page, size);
  }

  // ---------- GetAlertsQuery ----------

  @Test
  @DisplayName("GetAlertsQuery(...) -> null filter sets become empty immutable sets (AAA)")
  void alertsNullFiltersBecomeEmpty() {
    var query = alerts(null, null, null, 0, 20, null, null);

    assertThat(query.statuses()).isEmpty();
    assertThat(query.types()).isEmpty();
    assertThat(query.severities()).isEmpty();
    assertThrows(UnsupportedOperationException.class, () -> query.statuses().add(AlertStatus.OPEN));
  }

  @Test
  @DisplayName("GetAlertsQuery(...) -> copies the filter sets defensively (AAA)")
  void alertsCopiesFilters() {
    var statuses = new HashSet<>(Set.of(AlertStatus.OPEN));

    var query = alerts(statuses, null, null, 0, 20, null, null);
    statuses.add(AlertStatus.RESOLVED);

    assertThat(query.statuses()).containsExactly(AlertStatus.OPEN);
  }

  @Test
  @DisplayName("GetAlertsQuery(...) -> rejects a negative page and a size below one (AAA)")
  void alertsPagingValidation() {
    var pageEx = assertThrows(IllegalArgumentException.class, () -> alerts(null, null, null, -1, 20, null, null));
    assertThat(pageEx).hasMessage("Parameter 'page' must not be negative");
    var sizeEx = assertThrows(IllegalArgumentException.class, () -> alerts(null, null, null, 0, 0, null, null));
    assertThat(sizeEx).hasMessage("Parameter 'size' must be at least 1");
  }

  @ParameterizedTest(name = "[{index}] size={0}")
  @ValueSource(ints = {101, 500, Integer.MAX_VALUE})
  @DisplayName("GetAlertsQuery(...) -> a size above the maximum is capped, not rejected (AAA)")
  void alertsSizeIsCapped(int size) {
    assertThat(alerts(null, null, null, 0, size, null, null).size()).isEqualTo(GetAlertsQuery.MAX_PAGE_SIZE);
  }

  @Test
  @DisplayName("GetAlertsQuery(...) -> keeps a size within the allowed range (AAA)")
  void alertsSizeWithinRange() {
    assertThat(alerts(null, null, null, 3, 100, null, null).size()).isEqualTo(100);
    assertThat(alerts(null, null, null, 3, 1, null, null).size()).isEqualTo(1);
  }

  @Test
  @DisplayName("GetAlertsQuery(...) -> rejects an opened_from after opened_to (AAA)")
  void alertsRangeValidation() {
    var ex = assertThrows(IllegalArgumentException.class,
        () -> alerts(null, null, null, 0, 20, T0.plusSeconds(1), T0));
    assertThat(ex).hasMessage("Parameter 'opened_from' must not be after 'opened_to'");
    assertDoesNotThrow(() -> alerts(null, null, null, 0, 20, T0, T0));
    assertDoesNotThrow(() -> alerts(null, null, null, 0, 20, T0, null));
  }

  // ---------- GetApplicableAlertPoliciesQuery ----------

  @Test
  @DisplayName("GetApplicableAlertPoliciesQuery(...) -> drops nulls and duplicates keeping order (AAA)")
  void applicableDeduplicates() {
    var query = new GetApplicableAlertPoliciesQuery(USERNAME, Arrays.asList(3L, null, 1L, 3L, 2L, 1L));

    assertThat(query.equipmentIds()).containsExactly(3L, 1L, 2L);
  }

  @Test
  @DisplayName("GetApplicableAlertPoliciesQuery(...) -> a null list becomes empty (AAA)")
  void applicableNullList() {
    assertThat(new GetApplicableAlertPoliciesQuery(USERNAME, null).equipmentIds()).isEmpty();
  }

  @Test
  @DisplayName("GetApplicableAlertPoliciesQuery(...) -> accepts exactly 100 ids and rejects 101 (AAA)")
  void applicableMaximumIds() {
    List<Long> hundred = new ArrayList<>(LongStream.rangeClosed(1, 100).boxed().toList());
    List<Long> hundredAndOne = new ArrayList<>(LongStream.rangeClosed(1, 101).boxed().toList());

    assertThat(new GetApplicableAlertPoliciesQuery(USERNAME, hundred).equipmentIds()).hasSize(100);
    var ex = assertThrows(IllegalArgumentException.class,
        () -> new GetApplicableAlertPoliciesQuery(USERNAME, hundredAndOne));
    assertThat(ex).hasMessage("Parameter 'equipment_id' must not name more than 100 units");
  }

  // ---------- GetMaintenanceWindowsByEquipmentQuery ----------

  @ParameterizedTest(name = "[{index}] equipmentId={0}")
  @ValueSource(longs = {0, -1})
  @DisplayName("GetMaintenanceWindowsByEquipmentQuery(...) -> requires a positive equipment id (AAA)")
  void windowsRequirePositiveEquipment(long id) {
    assertThrows(IllegalArgumentException.class,
        () -> new GetMaintenanceWindowsByEquipmentQuery(id, null, null, USERNAME));
  }

  @Test
  @DisplayName("GetMaintenanceWindowsByEquipmentQuery(...) -> requires a null equipment id to fail too (AAA)")
  void windowsRejectNullEquipment() {
    assertThrows(IllegalArgumentException.class,
        () -> new GetMaintenanceWindowsByEquipmentQuery(null, null, null, USERNAME));
  }

  @Test
  @DisplayName("GetMaintenanceWindowsByEquipmentQuery(...) -> rejects from after to (AAA)")
  void windowsRangeValidation() {
    var ex = assertThrows(IllegalArgumentException.class,
        () -> new GetMaintenanceWindowsByEquipmentQuery(EQUIPMENT_ID, T0.plusSeconds(1), T0, USERNAME));
    assertThat(ex).hasMessage("from must not be after to");
    assertDoesNotThrow(() -> new GetMaintenanceWindowsByEquipmentQuery(EQUIPMENT_ID, T0, T0, USERNAME));
    assertDoesNotThrow(() -> new GetMaintenanceWindowsByEquipmentQuery(EQUIPMENT_ID, null, null, USERNAME));
  }

  // ---------- GetReadingBucketsByEquipmentQuery ----------

  private static GetReadingBucketsByEquipmentQuery buckets(java.time.Instant from, java.time.Instant to,
                                                           TelemetryBucketSize size, Integer maxPoints) {
    return new GetReadingBucketsByEquipmentQuery(EQUIPMENT_ID, from, to, size, maxPoints, USERNAME, CallerRole.OWNER);
  }

  @Test
  @DisplayName("GetReadingBucketsByEquipmentQuery(...) -> requires from and to (AAA)")
  void bucketsRequireRange() {
    assertThrows(NullPointerException.class, () -> buckets(null, T0, null, null));
    assertThrows(NullPointerException.class, () -> buckets(T0, null, null, null));
  }

  @Test
  @DisplayName("GetReadingBucketsByEquipmentQuery(...) -> rejects from after to (AAA)")
  void bucketsRejectInvertedRange() {
    var ex = assertThrows(IllegalArgumentException.class, () -> buckets(T0.plusSeconds(1), T0, null, null));
    assertThat(ex).hasMessage("Parameter 'from' must not be after 'to'");
  }

  @Test
  @DisplayName("GetReadingBucketsByEquipmentQuery(...) -> maxPoints defaults to 500 (AAA)")
  void bucketsDefaultMaxPoints() {
    assertThat(buckets(T0, T0.plusSeconds(60), null, null).maxPoints())
        .isEqualTo(GetReadingBucketsByEquipmentQuery.DEFAULT_MAX_POINTS);
  }

  @ParameterizedTest(name = "[{index}] maxPoints={0}")
  @ValueSource(ints = {9, 2001, -1})
  @DisplayName("GetReadingBucketsByEquipmentQuery(...) -> maxPoints must be between 10 and 2000 (AAA)")
  void bucketsRejectBadMaxPoints(int maxPoints) {
    assertThrows(IllegalArgumentException.class, () -> buckets(T0, T0.plusSeconds(60), null, maxPoints));
  }

  @Test
  @DisplayName("GetReadingBucketsByEquipmentQuery(...) -> accepts maxPoints at both limits (AAA)")
  void bucketsAcceptLimits() {
    assertDoesNotThrow(() -> buckets(T0, T0.plusSeconds(60), null, 10));
    assertDoesNotThrow(() -> buckets(T0, T0.plusSeconds(60), null, 2000));
  }

  @Test
  @DisplayName("GetReadingBucketsByEquipmentQuery(...) -> rejects a fixed bucket that would exceed 2000 buckets (AAA)")
  void bucketsRejectTooManyBuckets() {
    var ex = assertThrows(IllegalArgumentException.class,
        () -> buckets(T0, T0.plus(Duration.ofDays(30)), TelemetryBucketSize.ONE_MINUTE, null));
    assertThat(ex.getMessage()).contains("1m").contains("2000");
  }

  @Test
  @DisplayName("GetReadingBucketsByEquipmentQuery.effectiveBucketSize() -> the requested size wins (AAA)")
  void effectiveSizeExplicit() {
    var query = buckets(T0, T0.plus(Duration.ofHours(1)), TelemetryBucketSize.FIFTEEN_MINUTES, null);

    assertThat(query.effectiveBucketSize()).isEqualTo(TelemetryBucketSize.FIFTEEN_MINUTES);
  }

  @Test
  @DisplayName("GetReadingBucketsByEquipmentQuery.effectiveBucketSize() -> auto picks the smallest bucket within the budget (AAA)")
  void effectiveSizeAuto() {
    var query = buckets(T0, T0.plus(Duration.ofDays(2)), null, 100);

    assertThat(query.effectiveBucketSize()).isEqualTo(TelemetryBucketSize.ONE_HOUR);
  }
}
