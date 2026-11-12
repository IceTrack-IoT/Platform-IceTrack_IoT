package pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.SiteId;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pe.edu.upc.ice.track.platform.reporting.test.fixtures.ReportingDomainFixtures.FROM;
import static pe.edu.upc.ice.track.platform.reporting.test.fixtures.ReportingDomainFixtures.TO;
import static pe.edu.upc.ice.track.platform.reporting.test.fixtures.ReportingDomainFixtures.dateRange;
import static pe.edu.upc.ice.track.platform.reporting.test.fixtures.ReportingDomainFixtures.pendingReport;

class ReportValueObjectsTest {

  @Nested
  class DateRangeTests {

    @Test
    @DisplayName("new DateRange(from, to) -> keeps both bounds and reports total hours (AAA)")
    void valid() {
      // Act
      var range = new DateRange(FROM, TO);

      // Assert
      assertThat(range.from()).isEqualTo(FROM);
      assertThat(range.to()).isEqualTo(TO);
      assertThat(range.totalHours()).isEqualTo(24.0);
    }

    @Test
    @DisplayName("new DateRange(t, t) -> accepted, zero hours (AAA)")
    void emptyRangeAllowed() {
      // Act & Assert
      assertThat(new DateRange(FROM, FROM).totalHours()).isZero();
    }

    @Test
    @DisplayName("totalHours() -> fractional hours kept (AAA)")
    void fractionalHours() {
      // Act & Assert
      assertThat(new DateRange(FROM, FROM.plusSeconds(5400)).totalHours()).isEqualTo(1.5);
    }

    @Test
    @DisplayName("new DateRange(null / inverted) -> NPE or IllegalArgumentException (AAA)")
    void invalid() {
      // Act & Assert
      assertThatThrownBy(() -> new DateRange(null, TO)).isInstanceOf(NullPointerException.class).hasMessage("from must not be null");
      assertThatThrownBy(() -> new DateRange(FROM, null)).isInstanceOf(NullPointerException.class).hasMessage("to must not be null");
      assertThatThrownBy(() -> new DateRange(TO, FROM)).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("from must not be after to");
    }
  }

  @Nested
  class KpiValueTests {

    @Test
    @DisplayName("new KpiValue(key, ...) -> keeps explicit key (AAA)")
    void explicitKey() {
      // Act & Assert
      assertThat(new KpiValue("my_key", "Label", 1.0, "%").key()).isEqualTo("my_key");
    }

    @ParameterizedTest
    @CsvSource(value = {"'', Average Uptime %, average_uptime", "'   ', --Hola Mundo--, hola_mundo", "'', !!!, kpi"})
    @DisplayName("new KpiValue(blank key, label, ...) -> key derived from the label (AAA)")
    void derivedKey(String key, String label, String expected) {
      // Act
      var kpi = new KpiValue(key, label, 1.0, "x");

      // Assert
      assertThat(kpi.key()).isEqualTo(expected);
    }

    @Test
    @DisplayName("new KpiValue(null key, ...) -> key derived from the label (AAA)")
    void nullKey() {
      // Act & Assert
      assertThat(new KpiValue(null, "Excursion count", 2.0, "count").key()).isEqualTo("excursion_count");
    }

    @Test
    @DisplayName("new KpiValue(null label / value / unit) -> NullPointerException (AAA)")
    void nulls() {
      // Act & Assert
      assertThatThrownBy(() -> new KpiValue("k", null, 1.0, "u")).hasMessage("label must not be null");
      assertThatThrownBy(() -> new KpiValue("k", "l", null, "u")).hasMessage("value must not be null");
      assertThatThrownBy(() -> new KpiValue("k", "l", 1.0, null)).hasMessage("unit must not be null");
    }

    @Test
    @DisplayName("perUnitKey(metric, id) -> metric.unit_<id> (AAA)")
    void perUnit() {
      // Act & Assert
      assertThat(KpiValue.perUnitKey("uptime_pct", 10L)).isEqualTo("uptime_pct.unit_10");
    }
  }

  @Nested
  class ReportFileTests {

    @Test
    @DisplayName("new ReportFile(key, type, size) -> valid, zero size allowed (AAA)")
    void valid() {
      // Act
      var file = new ReportFile("a/b.pdf", "application/pdf", 0);

      // Assert
      assertThat(file.key()).isEqualTo("a/b.pdf");
      assertThat(file.sizeBytes()).isZero();
    }

    @Test
    @DisplayName("new ReportFile(invalid) -> exception per rule (AAA)")
    void invalid() {
      // Act & Assert
      assertThatThrownBy(() -> new ReportFile(null, "t", 1)).isInstanceOf(NullPointerException.class);
      assertThatThrownBy(() -> new ReportFile("k", null, 1)).isInstanceOf(NullPointerException.class);
      assertThatThrownBy(() -> new ReportFile("  ", "t", 1)).hasMessage("key must not be blank");
      assertThatThrownBy(() -> new ReportFile("k", "t", -1)).hasMessage("sizeBytes must not be negative");
    }
  }

  @Nested
  class ReportFiltersTests {

    @Test
    @DisplayName("4-arg constructor -> serviceRequestId is null (AAA)")
    void fourArgs() {
      // Act
      var filters = new ReportFilters(new SiteId(3L), null, null, dateRange());

      // Assert
      assertThat(filters.serviceRequestId()).isNull();
      assertThat(filters.siteId()).isEqualTo(new SiteId(3L));
    }

    @Test
    @DisplayName("new ReportFilters(...) with null range or non-positive request id -> exception (AAA)")
    void invalid() {
      // Act & Assert
      assertThatThrownBy(() -> new ReportFilters(null, null, null, null)).isInstanceOf(NullPointerException.class)
          .hasMessage("dateRange must not be null");
      assertThatThrownBy(() -> new ReportFilters(null, null, null, dateRange(), 0L))
          .isInstanceOf(IllegalArgumentException.class).hasMessage("serviceRequestId must be positive");
      assertThatThrownBy(() -> new ReportFilters(null, null, null, dateRange(), -4L))
          .isInstanceOf(IllegalArgumentException.class);
      assertThat(new ReportFilters(null, null, null, dateRange(), 1L).serviceRequestId()).isEqualTo(1L);
    }
  }

  @Nested
  class ReportListFilterTests {

    @Test
    @DisplayName("ReportListFilter.NONE -> no criteria at all (AAA)")
    void none() {
      // Assert
      assertThat(ReportListFilter.NONE.search()).isNull();
      assertThat(ReportListFilter.NONE.types()).isEmpty();
      assertThat(ReportListFilter.NONE.statuses()).isEmpty();
      assertThat(ReportListFilter.NONE.requestedFrom()).isNull();
      assertThat(ReportListFilter.NONE.requestedTo()).isNull();
    }

    @Test
    @DisplayName("new ReportListFilter(search, ...) -> search stripped, blank becomes null, null sets become empty (AAA)")
    void normalizes() {
      // Act
      var stripped = new ReportListFilter("  uptime  ", null, null, null, null);
      var blank = new ReportListFilter("   ", Set.of(), Set.of(), null, null);

      // Assert
      assertThat(stripped.search()).isEqualTo("uptime");
      assertThat(stripped.types()).isEmpty();
      assertThat(stripped.statuses()).isEmpty();
      assertThat(blank.search()).isNull();
    }

    @Test
    @DisplayName("new ReportListFilter(...) -> sets are defensively copied and immutable (AAA)")
    void immutableCopies() {
      // Arrange
      var types = EnumSet.of(ReportType.EQUIPMENT_UPTIME);

      // Act
      var filter = new ReportListFilter(null, types, EnumSet.of(ReportStatus.FAILED), null, null);
      types.add(ReportType.SERVICE_INTERVENTION);

      // Assert
      assertThat(filter.types()).containsExactly(ReportType.EQUIPMENT_UPTIME);
      assertThatThrownBy(() -> filter.statuses().add(ReportStatus.PENDING)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("new ReportListFilter(search of 150 / 151 chars) -> accepted / rejected (AAA)")
    void searchLength() {
      // Act & Assert
      assertThat(new ReportListFilter("a".repeat(150), null, null, null, null).search()).hasSize(150);
      assertThatThrownBy(() -> new ReportListFilter("a".repeat(151), null, null, null, null))
          .isInstanceOf(IllegalArgumentException.class).hasMessage("Parameter 'search' must not exceed 150 characters");
    }

    @Test
    @DisplayName("new ReportListFilter(from after to) -> IllegalArgumentException; equal bounds accepted (AAA)")
    void rangeOrder() {
      // Act & Assert
      assertThatThrownBy(() -> new ReportListFilter(null, null, null, TO, FROM))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Parameter 'requested_from' must not be after 'requested_to'");
      assertThat(new ReportListFilter(null, null, null, FROM, FROM).requestedFrom()).isEqualTo(FROM);
      assertThat(new ReportListFilter(null, null, null, FROM, null).requestedTo()).isNull();
    }
  }

  @Nested
  class ReportPageTests {

    @Test
    @DisplayName("new ReportPage(content, page, size, total) -> copies content (AAA)")
    void valid() {
      // Arrange
      var content = new java.util.ArrayList<>(List.of(pendingReport()));

      // Act
      var page = new ReportPage(content, 1, 10, 11);
      content.clear();

      // Assert
      assertThat(page.content()).hasSize(1);
      assertThat(page.page()).isEqualTo(1);
      assertThat(page.totalElements()).isEqualTo(11);
    }

    @Test
    @DisplayName("new ReportPage(invalid) -> IllegalArgumentException (AAA)")
    void invalid() {
      // Act & Assert
      assertThatThrownBy(() -> new ReportPage(null, 0, 1, 0)).hasMessage("Page content must not be null");
      assertThatThrownBy(() -> new ReportPage(List.of(), -1, 1, 0)).hasMessage("Page index must not be negative");
      assertThatThrownBy(() -> new ReportPage(List.of(), 0, 0, 0))
          .hasMessage("Page size must be a positive number of reports");
    }

    @Test
    @DisplayName("ReportPage.empty(page, size) -> no content and zero total (AAA)")
    void empty() {
      // Act
      var page = ReportPage.empty(2, 20);

      // Assert
      assertThat(page.content()).isEmpty();
      assertThat(page.page()).isEqualTo(2);
      assertThat(page.size()).isEqualTo(20);
      assertThat(page.totalElements()).isZero();
    }

    @ParameterizedTest
    @CsvSource({"0,10,1", "1,10,1", "10,10,1", "11,10,2", "25,10,3", "100,100,1", "101,100,2"})
    @DisplayName("totalPages() -> ceil(total/size), at least 1 (AAA)")
    void totalPages(long total, int size, int expected) {
      // Act & Assert
      assertThat(new ReportPage(List.of(), 0, size, total).totalPages()).isEqualTo(expected);
    }
  }

  @Nested
  class ReportDownloadLinkTests {

    @Test
    @DisplayName("new ReportDownloadLink(...) -> exposes every component (AAA)")
    void valid() {
      // Act
      var link = new ReportDownloadLink("http://x", TO, "r.pdf", "application/pdf", 5);

      // Assert
      assertThat(link.url()).isEqualTo("http://x");
      assertThat(link.expiresAt()).isEqualTo(TO);
      assertThat(link.fileName()).isEqualTo("r.pdf");
      assertThat(link.sizeBytes()).isEqualTo(5);
    }

    @Test
    @DisplayName("new ReportDownloadLink(null component) -> NullPointerException (AAA)")
    void nulls() {
      // Act & Assert
      assertThatThrownBy(() -> new ReportDownloadLink(null, TO, "f", "c", 1)).hasMessage("url must not be null");
      assertThatThrownBy(() -> new ReportDownloadLink("u", null, "f", "c", 1)).hasMessage("expiresAt must not be null");
      assertThatThrownBy(() -> new ReportDownloadLink("u", TO, null, "c", 1)).hasMessage("fileName must not be null");
      assertThatThrownBy(() -> new ReportDownloadLink("u", TO, "f", null, 1)).hasMessage("contentType must not be null");
    }
  }

  @Nested
  class SmallTypes {

    @Test
    @DisplayName("new TechnicianId(null) -> IllegalArgumentException (AAA)")
    void technicianId() {
      // Act & Assert
      assertThatThrownBy(() -> new TechnicianId(null)).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("TechnicianId cannot be null");
      assertThat(new TechnicianId(6L).technicianId()).isEqualTo(6L);
    }

    @ParameterizedTest
    @EnumSource(ReportType.class)
    @DisplayName("isScopedByServiceRequest() -> true only for SERVICE_INTERVENTION (AAA)")
    void scoped(ReportType type) {
      // Act & Assert
      assertThat(type.isScopedByServiceRequest()).isEqualTo(type == ReportType.SERVICE_INTERVENTION);
    }

    @Test
    @DisplayName("enums -> expose the contract values (AAA)")
    void enums() {
      // Assert
      assertThat(ReportFormat.values()).containsExactly(ReportFormat.PDF, ReportFormat.XLSX, ReportFormat.CSV);
      assertThat(ReportStatus.values()).containsExactly(
          ReportStatus.PENDING, ReportStatus.GENERATING, ReportStatus.COMPLETED, ReportStatus.FAILED);
      assertThat(ReportType.values()).hasSize(5);
    }
  }
}
