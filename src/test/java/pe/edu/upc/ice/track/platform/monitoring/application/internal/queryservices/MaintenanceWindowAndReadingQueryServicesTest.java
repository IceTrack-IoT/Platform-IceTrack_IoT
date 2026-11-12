package pe.edu.upc.ice.track.platform.monitoring.application.internal.queryservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.accesspolicies.EquipmentAccessPolicy;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.SensorReading;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetMaintenanceWindowsByEquipmentQuery;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetReadingBucketsByEquipmentQuery;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetReadingsByEquipmentQuery;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.TelemetryBucket;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.TelemetryBucketSize;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.MaintenanceWindowRepository;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.SensorReadingRepository;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.CallerRole;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.monitoring.test.fixtures.MonitoringDomainFixtures.*;

/** Both services are thin: an access check in front of one repository read. */
class MaintenanceWindowAndReadingQueryServicesTest {

  @Nested
  @ExtendWith(MockitoExtension.class)
  class MaintenanceWindowQueryServiceTest {

    @Mock
    private MaintenanceWindowRepository maintenanceWindowRepository;
    @Mock
    private EquipmentAccessPolicy equipmentAccessPolicy;

    @InjectMocks
    private MaintenanceWindowQueryServiceImpl service;

    private final GetMaintenanceWindowsByEquipmentQuery query =
        new GetMaintenanceWindowsByEquipmentQuery(EQUIPMENT_ID, T0, T0.plus(Duration.ofDays(1)), USERNAME);

    @Test
    @DisplayName("handle(GetMaintenanceWindowsByEquipmentQuery) manageable equipment -> its windows (AAA)")
    void returnsWindows() {
      // Arrange
      var windows = List.of(persistedButtonWindow());
      when(equipmentAccessPolicy.canManage(EQUIPMENT_ID, USERNAME)).thenReturn(true);
      when(maintenanceWindowRepository.findByEquipmentIdAndStartedAtBetween(
          EQUIPMENT_ID, query.from(), query.to())).thenReturn(windows);

      // Act
      var result = service.handle(query);

      // Assert
      assertThat(result).contains(windows);
    }

    @Test
    @DisplayName("handle(GetMaintenanceWindowsByEquipmentQuery) unmanageable equipment -> empty, nothing read (AAA)")
    void deniedReturnsEmpty() {
      // Arrange
      when(equipmentAccessPolicy.canManage(EQUIPMENT_ID, USERNAME)).thenReturn(false);

      // Act
      var result = service.handle(query);

      // Assert
      assertThat(result).isEmpty();
      verifyNoInteractions(maintenanceWindowRepository);
    }

    @Test
    @DisplayName("handle(GetMaintenanceWindowsByEquipmentQuery) manageable with no windows -> present but empty (AAA)")
    void noWindowsIsNotDenied() {
      // Arrange
      when(equipmentAccessPolicy.canManage(EQUIPMENT_ID, USERNAME)).thenReturn(true);
      when(maintenanceWindowRepository.findByEquipmentIdAndStartedAtBetween(
          EQUIPMENT_ID, query.from(), query.to())).thenReturn(List.of());

      // Act
      var result = service.handle(query);

      // Assert
      assertThat(result).isPresent();
      assertThat(result.get()).isEmpty();
    }
  }

  @Nested
  @ExtendWith(MockitoExtension.class)
  class SensorReadingQueryServiceTest {

    @Mock
    private SensorReadingRepository sensorReadingRepository;
    @Mock
    private EquipmentAccessPolicy equipmentAccessPolicy;

    @InjectMocks
    private SensorReadingQueryServiceImpl service;

    private final java.time.Instant from = T0;
    private final java.time.Instant to = T0.plus(Duration.ofMinutes(30));

    @Test
    @DisplayName("handle(GetReadingsByEquipmentQuery) readable -> the readings of the range (AAA)")
    void returnsReadings() {
      // Arrange
      var readings = List.of(sensorReading());
      when(equipmentAccessPolicy.canRead(EQUIPMENT_ID, USERNAME, CallerRole.OWNER)).thenReturn(true);
      when(sensorReadingRepository.findByEquipmentIdAndRecordedAtBetween(EQUIPMENT_ID, from, to)).thenReturn(readings);

      // Act
      var result = service.handle(new GetReadingsByEquipmentQuery(EQUIPMENT_ID, from, to, USERNAME, CallerRole.OWNER));

      // Assert
      assertThat(result).contains(readings);
    }

    @Test
    @DisplayName("handle(GetReadingsByEquipmentQuery) unreadable -> empty, nothing read (AAA)")
    void readingsDenied() {
      // Arrange
      when(equipmentAccessPolicy.canRead(EQUIPMENT_ID, USERNAME, CallerRole.OWNER)).thenReturn(false);

      // Act
      var result = service.handle(new GetReadingsByEquipmentQuery(EQUIPMENT_ID, from, to, USERNAME, CallerRole.OWNER));

      // Assert
      assertThat(result).isEmpty();
      verifyNoInteractions(sensorReadingRepository);
    }

    @Test
    @DisplayName("handle(GetReadingBucketsByEquipmentQuery) explicit size -> aggregates readings into that size (AAA)")
    void bucketsWithExplicitSize() {
      // Arrange
      var readings = List.<SensorReading>of(
          sensorReading(T0, 4.0, 5.0, 6.0, 10, false),
          sensorReading(T0.plusSeconds(60), 5.0, 6.0, 7.0, 10, false),
          sensorReading(T0.plus(Duration.ofMinutes(7)), 3.0, 4.0, 5.0, 10, false));
      when(equipmentAccessPolicy.canRead(EQUIPMENT_ID, USERNAME, CallerRole.OWNER)).thenReturn(true);
      when(sensorReadingRepository.findByEquipmentIdAndRecordedAtBetween(EQUIPMENT_ID, from, to)).thenReturn(readings);
      var query = new GetReadingBucketsByEquipmentQuery(
          EQUIPMENT_ID, from, to, TelemetryBucketSize.FIVE_MINUTES, null, USERNAME, CallerRole.OWNER);

      // Act
      var result = service.handle(query);

      // Assert
      assertThat(result).isPresent();
      assertThat(result.get().bucketSize()).isEqualTo(TelemetryBucketSize.FIVE_MINUTES);
      assertThat(result.get().buckets())
          .isEqualTo(TelemetryBucket.aggregate(readings, TelemetryBucketSize.FIVE_MINUTES));
      assertThat(result.get().buckets()).hasSize(2);
    }

    @Test
    @DisplayName("handle(GetReadingBucketsByEquipmentQuery) no size -> picks the smallest size fitting maxPoints (AAA)")
    void bucketsWithAutomaticSize() {
      // Arrange
      when(equipmentAccessPolicy.canRead(EQUIPMENT_ID, USERNAME, CallerRole.OWNER)).thenReturn(true);
      when(sensorReadingRepository.findByEquipmentIdAndRecordedAtBetween(EQUIPMENT_ID, from, to)).thenReturn(List.of());
      var query = new GetReadingBucketsByEquipmentQuery(
          EQUIPMENT_ID, from, to, null, 10, USERNAME, CallerRole.OWNER);

      // Act
      var result = service.handle(query);

      // Assert
      assertThat(result).isPresent();
      assertThat(result.get().bucketSize()).isEqualTo(query.effectiveBucketSize());
      assertThat(result.get().buckets()).isEmpty();
      verify(sensorReadingRepository, times(1)).findByEquipmentIdAndRecordedAtBetween(EQUIPMENT_ID, from, to);
    }

    @Test
    @DisplayName("handle(GetReadingBucketsByEquipmentQuery) unreadable -> empty, nothing read (AAA)")
    void bucketsDenied() {
      // Arrange
      when(equipmentAccessPolicy.canRead(EQUIPMENT_ID, USERNAME, CallerRole.OWNER)).thenReturn(false);
      var query = new GetReadingBucketsByEquipmentQuery(
          EQUIPMENT_ID, from, to, TelemetryBucketSize.ONE_MINUTE, null, USERNAME, CallerRole.OWNER);

      // Act
      var result = service.handle(query);

      // Assert
      assertThat(result).isEmpty();
      verifyNoInteractions(sensorReadingRepository);
    }
  }
}
