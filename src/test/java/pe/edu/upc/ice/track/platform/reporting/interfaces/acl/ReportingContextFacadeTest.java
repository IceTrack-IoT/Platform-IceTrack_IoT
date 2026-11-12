package pe.edu.upc.ice.track.platform.reporting.interfaces.acl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportListFilter;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportPage;
import pe.edu.upc.ice.track.platform.reporting.domain.repositories.ReportRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static pe.edu.upc.ice.track.platform.reporting.test.fixtures.ReportingDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class ReportingContextFacadeTest {

  @Mock private ReportRepository reportRepository;
  @InjectMocks private ReportingContextFacade facade;

  @Test
  @DisplayName("countReportsByUser(userId) -> total of a one-row page, reports never loaded (AAA)")
  void count() {
    // Arrange
    doReturn(new ReportPage(List.of(completedReport()), 0, 1, 42)).when(reportRepository)
        .findPageByUserId(USER_ID, ReportListFilter.NONE, 0, 1);

    // Act & Assert
    assertThat(facade.countReportsByUser(USER_ID)).isEqualTo(42);
    verify(reportRepository, times(1)).findPageByUserId(USER_ID, ReportListFilter.NONE, 0, 1);
  }

  @Test
  @DisplayName("countReportsByUser(total beyond int) -> ArithmeticException instead of silent overflow (AAA)")
  void countOverflow() {
    // Arrange
    doReturn(new ReportPage(List.of(), 0, 1, Integer.MAX_VALUE + 1L)).when(reportRepository)
        .findPageByUserId(USER_ID, ReportListFilter.NONE, 0, 1);

    // Act & Assert
    assertThatThrownBy(() -> facade.countReportsByUser(USER_ID)).isInstanceOf(ArithmeticException.class);
  }

  @Test
  @DisplayName("countReportsByUser(null) -> 0 without querying (AAA)")
  void countNull() {
    // Act & Assert
    assertThat(facade.countReportsByUser(null)).isZero();
    verifyNoInteractions(reportRepository);
  }

  @Test
  @DisplayName("fetchLatestReportStatusByUser(userId) -> status name of the first report (AAA)")
  void latest() {
    // Arrange
    doReturn(new ReportPage(List.of(failedReport()), 0, 1, 3)).when(reportRepository)
        .findPageByUserId(USER_ID, ReportListFilter.NONE, 0, 1);

    // Act & Assert
    assertThat(facade.fetchLatestReportStatusByUser(USER_ID)).isEqualTo("FAILED");
  }

  @Test
  @DisplayName("fetchLatestReportStatusByUser(user without reports / null) -> null (AAA)")
  void latestNone() {
    // Arrange
    doReturn(ReportPage.empty(0, 1)).when(reportRepository).findPageByUserId(OTHER_USER_ID, ReportListFilter.NONE, 0, 1);

    // Act & Assert
    assertThat(facade.fetchLatestReportStatusByUser(OTHER_USER_ID)).isNull();
    assertThat(facade.fetchLatestReportStatusByUser(null)).isNull();
  }
}
