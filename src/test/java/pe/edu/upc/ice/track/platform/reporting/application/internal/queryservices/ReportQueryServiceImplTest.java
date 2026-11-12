package pe.edu.upc.ice.track.platform.reporting.application.internal.queryservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.acl.ExternalProfilesServiceForReporting;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.storage.ReportFileStorage;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.storage.SignedDownloadUrl;
import pe.edu.upc.ice.track.platform.reporting.domain.model.queries.GetReportByIdQuery;
import pe.edu.upc.ice.track.platform.reporting.domain.model.queries.GetReportDownloadLinkQuery;
import pe.edu.upc.ice.track.platform.reporting.domain.model.queries.GetReportPageByUserQuery;
import pe.edu.upc.ice.track.platform.reporting.domain.model.queries.GetReportsByUserQuery;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportListFilter;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportPage;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportStatus;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportType;
import pe.edu.upc.ice.track.platform.reporting.domain.repositories.ReportRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static pe.edu.upc.ice.track.platform.shared.test.util.ResultTestUtils.failureError;
import static pe.edu.upc.ice.track.platform.shared.test.util.ResultTestUtils.successValue;
import static pe.edu.upc.ice.track.platform.reporting.test.fixtures.ReportingDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class ReportQueryServiceImplTest {

  @Mock private ReportRepository reportRepository;
  @Mock private ReportFileStorage fileStorage;
  @Mock private ExternalProfilesServiceForReporting profiles;
  @InjectMocks private ReportQueryServiceImpl service;

  private void caller() {
    doReturn(Optional.of(USER_ID)).when(profiles).fetchUserIdByUsername(USERNAME);
  }

  @Nested
  class ById {

    @Test
    @DisplayName("handle(GetReportByIdQuery) -> the caller's own report (AAA)")
    void owned() {
      // Arrange
      caller();
      var report = completedReport();
      doReturn(Optional.of(report)).when(reportRepository).findById(REPORT_ID);

      // Act & Assert
      assertThat(service.handle(new GetReportByIdQuery(REPORT_ID, USERNAME))).containsSame(report);
    }

    @Test
    @DisplayName("handle(GetReportByIdQuery) -> empty for another user's report (AAA)")
    void foreign() {
      // Arrange
      doReturn(Optional.of(OTHER_USER_ID)).when(profiles).fetchUserIdByUsername(USERNAME);
      doReturn(Optional.of(completedReport())).when(reportRepository).findById(REPORT_ID);

      // Act & Assert
      assertThat(service.handle(new GetReportByIdQuery(REPORT_ID, USERNAME))).isEmpty();
    }

    @Test
    @DisplayName("handle(GetReportByIdQuery) -> empty for unknown user or missing report (AAA)")
    void unknown() {
      // Arrange
      doReturn(Optional.empty()).when(profiles).fetchUserIdByUsername("ghost");
      caller();
      doReturn(Optional.empty()).when(reportRepository).findById(REPORT_ID);

      // Act & Assert
      assertThat(service.handle(new GetReportByIdQuery(REPORT_ID, "ghost"))).isEmpty();
      assertThat(service.handle(new GetReportByIdQuery(REPORT_ID, USERNAME))).isEmpty();
    }

    @Test
    @DisplayName("handle(GetReportByIdQuery with null id) -> empty without touching collaborators (AAA)")
    void nullId() {
      // Act & Assert
      assertThat(service.handle(new GetReportByIdQuery(null, USERNAME))).isEmpty();
      verifyNoInteractions(profiles, reportRepository);
    }

    @Test
    @DisplayName("handle(null queries) -> NullPointerException (AAA)")
    void nullQueries() {
      // Act & Assert
      assertThatThrownBy(() -> service.handle((GetReportByIdQuery) null)).hasMessage("query must not be null");
      assertThatThrownBy(() -> service.handle((GetReportsByUserQuery) null)).isInstanceOf(NullPointerException.class);
      assertThatThrownBy(() -> service.handle((GetReportPageByUserQuery) null)).isInstanceOf(NullPointerException.class);
      assertThatThrownBy(() -> service.handle((GetReportDownloadLinkQuery) null)).isInstanceOf(NullPointerException.class);
    }
  }

  @Nested
  class Lists {

    @Test
    @DisplayName("handle(GetReportsByUserQuery) -> repository list for the resolved user and filter (AAA)")
    void list() {
      // Arrange
      caller();
      var filter = new ReportListFilter("x", Set.of(ReportType.EQUIPMENT_UPTIME), Set.of(ReportStatus.COMPLETED), null, null);
      var reports = List.of(completedReport());
      doReturn(reports).when(reportRepository).findByUserId(USER_ID, filter);

      // Act & Assert
      assertThat(service.handle(new GetReportsByUserQuery(USERNAME, filter))).isEqualTo(reports);
    }

    @Test
    @DisplayName("handle(GetReportsByUserQuery) for unknown user -> empty list, repository untouched (AAA)")
    void listUnknown() {
      // Arrange
      doReturn(Optional.empty()).when(profiles).fetchUserIdByUsername("ghost");

      // Act & Assert
      assertThat(service.handle(new GetReportsByUserQuery("ghost", null))).isEmpty();
      verifyNoInteractions(reportRepository);
    }

    @Test
    @DisplayName("handle(GetReportPageByUserQuery) -> repository page for the resolved user (AAA)")
    void page() {
      // Arrange
      caller();
      var page = new ReportPage(List.of(completedReport()), 1, 5, 6);
      doReturn(page).when(reportRepository).findPageByUserId(USER_ID, ReportListFilter.NONE, 1, 5);

      // Act & Assert
      assertThat(service.handle(new GetReportPageByUserQuery(USERNAME, null, 1, 5))).isSameAs(page);
    }

    @Test
    @DisplayName("handle(GetReportPageByUserQuery) for unknown user -> empty page with the requested paging (AAA)")
    void pageUnknown() {
      // Arrange
      doReturn(Optional.empty()).when(profiles).fetchUserIdByUsername("ghost");

      // Act
      var page = service.handle(new GetReportPageByUserQuery("ghost", null, 2, 20));

      // Assert
      assertThat(page.content()).isEmpty();
      assertThat(page.page()).isEqualTo(2);
      assertThat(page.size()).isEqualTo(20);
      assertThat(page.totalElements()).isZero();
      verifyNoInteractions(reportRepository);
    }
  }

  @Nested
  class DownloadLink {

    @Test
    @DisplayName("handle(GetReportDownloadLinkQuery) on a COMPLETED report -> signed link valid 15 minutes with file metadata (AAA)")
    void link() {
      // Arrange
      caller();
      var expires = Instant.parse("2026-01-01T00:15:00Z");
      doReturn(Optional.of(completedReport())).when(reportRepository).findById(REPORT_ID);
      doReturn(new SignedDownloadUrl("https://signed/x", expires)).when(fileStorage)
          .generateDownloadUrl(FILE_KEY, Duration.ofMinutes(15), "monthly-report.pdf");

      // Act
      var link = successValue(service.handle(new GetReportDownloadLinkQuery(REPORT_ID, USERNAME)));

      // Assert
      assertThat(link.url()).isEqualTo("https://signed/x");
      assertThat(link.expiresAt()).isEqualTo(expires);
      assertThat(link.fileName()).isEqualTo("monthly-report.pdf");
      assertThat(link.contentType()).isEqualTo("application/pdf");
      assertThat(link.sizeBytes()).isEqualTo(2048L);
      verify(fileStorage, times(1)).generateDownloadUrl(FILE_KEY, Duration.ofMinutes(15), "monthly-report.pdf");
    }

    @Test
    @DisplayName("handle(GetReportDownloadLinkQuery) for a missing or foreign report -> REPORT_NOT_FOUND (AAA)")
    void notFound() {
      // Arrange
      caller();
      doReturn(Optional.empty()).when(reportRepository).findById(REPORT_ID);

      // Act
      var error = failureError(service.handle(new GetReportDownloadLinkQuery(REPORT_ID, USERNAME)));

      // Assert
      assertThat(error.code()).isEqualTo("REPORT_NOT_FOUND");
      verifyNoInteractions(fileStorage);
    }

    @Test
    @DisplayName("handle(GetReportDownloadLinkQuery) on a non-COMPLETED report -> INVALID_STATUS_TRANSITION naming the status (AAA)")
    void notCompleted() {
      // Arrange
      caller();
      doReturn(Optional.of(generatingReport())).when(reportRepository).findById(REPORT_ID);

      // Act
      var error = failureError(service.handle(new GetReportDownloadLinkQuery(REPORT_ID, USERNAME)));

      // Assert
      assertThat(error.code()).isEqualTo("INVALID_STATUS_TRANSITION");
      assertThat(error.message()).isEqualTo("Report has no file to download");
      assertThat(error.details()).isEqualTo("Report 500 is GENERATING; a download link is only issued for a COMPLETED report");
      verifyNoInteractions(fileStorage);
    }

    @Test
    @DisplayName("handle(GetReportDownloadLinkQuery) on a COMPLETED report without file -> dedicated details (AAA)")
    void completedWithoutFile() {
      // Arrange
      caller();
      var report = pe.edu.upc.ice.track.platform.reporting.domain.model.aggregates.Report.rehydrate(REPORT_ID, TITLE, ReportType.EQUIPMENT_UPTIME,
          pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportFormat.PDF, ReportStatus.COMPLETED, "[]",
          new pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId(USER_ID), siteFilters(),
          Instant.now(), Instant.now(), null);
      doReturn(Optional.of(report)).when(reportRepository).findById(REPORT_ID);

      // Act
      var error = failureError(service.handle(new GetReportDownloadLinkQuery(REPORT_ID, USERNAME)));

      // Assert
      assertThat(error.code()).isEqualTo("INVALID_STATUS_TRANSITION");
      assertThat(error.details()).isEqualTo("Report 500 is COMPLETED but has no stored file");
      verifyNoInteractions(fileStorage);
    }
  }
}
