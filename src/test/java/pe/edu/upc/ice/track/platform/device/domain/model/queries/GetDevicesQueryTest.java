package pe.edu.upc.ice.track.platform.device.domain.model.queries;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.upc.ice.track.platform.device.domain.model.valueobjects.DeviceConnectivityState;
import pe.edu.upc.ice.track.platform.device.domain.model.valueobjects.DeviceStatus;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static pe.edu.upc.ice.track.platform.device.test.fixtures.DeviceDomainFixtures.USERNAME;

class GetDevicesQueryTest {

  @Test
  @DisplayName("GetDevicesQuery(...) -> null sets become empty and a blank search becomes null (AAA)")
  void constructorNormalizesNulls() {
    // Act
    var query = new GetDevicesQuery(USERNAME, null, null, null, "   ", null, null, null);

    // Assert
    assertThat(query.statuses()).isEmpty();
    assertThat(query.connectivity()).isEmpty();
    assertThat(query.search()).isNull();
    assertThat(query.isPaged()).isFalse();
    assertThat(query.filtersByConnectivity()).isFalse();
  }

  @Test
  @DisplayName("GetDevicesQuery(...) -> strips the search text (AAA)")
  void constructorStripsSearch() {
    // Act
    var query = new GetDevicesQuery(USERNAME, null, null, null, "  esp32  ", null, null);

    // Assert
    assertThat(query.search()).isEqualTo("esp32");
  }

  @Test
  @DisplayName("GetDevicesQuery(...) -> rejects a search longer than 64 characters (AAA)")
  void constructorRejectsLongSearch() {
    // Act & Assert
    assertThrows(IllegalArgumentException.class,
        () -> new GetDevicesQuery(USERNAME, null, null, null, "x".repeat(65), null, null));
  }

  @Test
  @DisplayName("GetDevicesQuery(...) -> rejects a negative page and a size below 1 (AAA)")
  void constructorRejectsInvalidPaging() {
    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> new GetDevicesQuery(USERNAME, null, null, null, null, -1, 10));
    assertThrows(IllegalArgumentException.class, () -> new GetDevicesQuery(USERNAME, null, null, null, null, 0, 0));
  }

  @Test
  @DisplayName("GetDevicesQuery(...) -> caps the page size at 100 instead of rejecting it (AAA)")
  void constructorCapsPageSize() {
    // Act
    var query = new GetDevicesQuery(USERNAME, null, null, null, null, 0, 5000);

    // Assert
    assertThat(query.size()).isEqualTo(GetDevicesQuery.MAX_PAGE_SIZE);
  }

  @Test
  @DisplayName("isPaged() -> true only when both page and size are present (AAA)")
  void isPagedNeedsBoth() {
    // Act & Assert
    assertThat(new GetDevicesQuery(USERNAME, null, null, null, null, 0, 10).isPaged()).isTrue();
    assertThat(new GetDevicesQuery(USERNAME, null, null, null, null, 0, null).isPaged()).isFalse();
    assertThat(new GetDevicesQuery(USERNAME, null, null, null, null, null, 10).isPaged()).isFalse();
  }

  @Test
  @DisplayName("filtersByConnectivity() -> true when at least one state is requested (AAA)")
  void filtersByConnectivity() {
    // Act
    var query = new GetDevicesQuery(USERNAME, null, null, Set.of(DeviceStatus.PAIRED), null, null, null,
        Set.of(DeviceConnectivityState.SILENT));

    // Assert
    assertThat(query.filtersByConnectivity()).isTrue();
    assertThat(query.statuses()).containsExactly(DeviceStatus.PAIRED);
  }
}
