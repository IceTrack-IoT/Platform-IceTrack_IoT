package pe.edu.upc.ice.track.platform.reporting.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.upc.ice.track.platform.reporting.domain.model.commands.DeleteReportCommand;
import pe.edu.upc.ice.track.platform.reporting.domain.model.commands.RequestReportCommand;
import pe.edu.upc.ice.track.platform.reporting.domain.model.events.ReportDeletedEvent;
import pe.edu.upc.ice.track.platform.reporting.domain.model.events.ReportRequestedEvent;
import pe.edu.upc.ice.track.platform.reporting.domain.model.queries.GetReportByIdQuery;
import pe.edu.upc.ice.track.platform.reporting.domain.model.queries.GetReportDownloadLinkQuery;
import pe.edu.upc.ice.track.platform.reporting.domain.model.queries.GetReportPageByUserQuery;
import pe.edu.upc.ice.track.platform.reporting.domain.model.queries.GetReportsByUserQuery;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportFormat;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportListFilter;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportStatus;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportType;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pe.edu.upc.ice.track.platform.reporting.test.fixtures.ReportingDomainFixtures.FROM;
import static pe.edu.upc.ice.track.platform.reporting.test.fixtures.ReportingDomainFixtures.TO;
import static pe.edu.upc.ice.track.platform.reporting.test.fixtures.ReportingDomainFixtures.USERNAME;

class ReportCommandsQueriesEventsTest {

  @Test
  @DisplayName("RequestReportCommand 9-arg constructor -> serviceRequestId defaults to null (AAA)")
  void requestCommandShortForm() {
    // Act
    var command = new RequestReportCommand("t", ReportType.EQUIPMENT_UPTIME, ReportFormat.PDF, USERNAME, 3L, 4L, 5L, FROM, TO);

    // Assert
    assertThat(command.serviceRequestId()).isNull();
    assertThat(command.siteId()).isEqualTo(3L);
    assertThat(command.equipmentId()).isEqualTo(4L);
    assertThat(command.technicianId()).isEqualTo(5L);
    assertThat(command.from()).isEqualTo(FROM);
  }

  @Test
  @DisplayName("RequestReportCommand 10-arg constructor -> keeps serviceRequestId (AAA)")
  void requestCommandFullForm() {
    // Act
    var command = new RequestReportCommand("t", ReportType.SERVICE_INTERVENTION, ReportFormat.CSV, USERNAME, null, null, null, null, null, 77L);

    // Assert
    assertThat(command.serviceRequestId()).isEqualTo(77L);
    assertThat(command.format()).isEqualTo(ReportFormat.CSV);
  }

  @Test
  @DisplayName("DeleteReportCommand / GetReportByIdQuery / GetReportDownloadLinkQuery -> expose their components (AAA)")
  void simpleRecords() {
    // Act & Assert
    assertThat(new DeleteReportCommand(1L, USERNAME)).extracting("reportId", "username").containsExactly(1L, USERNAME);
    assertThat(new GetReportByIdQuery(2L, USERNAME)).extracting("reportId", "username").containsExactly(2L, USERNAME);
    assertThat(new GetReportDownloadLinkQuery(3L, USERNAME)).extracting("reportId", "username").containsExactly(3L, USERNAME);
  }

  @Test
  @DisplayName("GetReportsByUserQuery(null filter) -> ReportListFilter.NONE (AAA)")
  void listQueryDefaultsFilter() {
    // Act & Assert
    assertThat(new GetReportsByUserQuery(USERNAME, null).filter()).isSameAs(ReportListFilter.NONE);
    var filter = new ReportListFilter("x", Set.of(ReportType.EQUIPMENT_UPTIME), Set.of(ReportStatus.FAILED), null, null);
    assertThat(new GetReportsByUserQuery(USERNAME, filter).filter()).isSameAs(filter);
  }

  @Test
  @DisplayName("GetReportPageByUserQuery(page, size) -> defaults filter, caps size at 100 (AAA)")
  void pageQueryNormalizes() {
    // Act
    var query = new GetReportPageByUserQuery(USERNAME, null, 2, 500);

    // Assert
    assertThat(query.filter()).isSameAs(ReportListFilter.NONE);
    assertThat(query.size()).isEqualTo(GetReportPageByUserQuery.MAX_PAGE_SIZE).isEqualTo(100);
    assertThat(query.page()).isEqualTo(2);
    assertThat(new GetReportPageByUserQuery(USERNAME, null, 0, 100).size()).isEqualTo(100);
    assertThat(new GetReportPageByUserQuery(USERNAME, null, 0, 1).size()).isEqualTo(1);
  }

  @Test
  @DisplayName("GetReportPageByUserQuery(negative page / size < 1) -> IllegalArgumentException (AAA)")
  void pageQueryRejects() {
    // Act & Assert
    assertThatThrownBy(() -> new GetReportPageByUserQuery(USERNAME, null, -1, 10))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("Parameter 'page' must not be negative");
    assertThatThrownBy(() -> new GetReportPageByUserQuery(USERNAME, null, 0, 0))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("Parameter 'size' must be at least 1");
  }

  @Test
  @DisplayName("ReportRequestedEvent / ReportDeletedEvent -> expose their components (AAA)")
  void events() {
    // Act & Assert
    assertThat(new ReportRequestedEvent(5L).reportId()).isEqualTo(5L);
    var deleted = new ReportDeletedEvent(5L, "k");
    assertThat(deleted.reportId()).isEqualTo(5L);
    assertThat(deleted.fileKey()).isEqualTo("k");
    assertThat(new ReportDeletedEvent(5L, null).fileKey()).isNull();
  }
}
