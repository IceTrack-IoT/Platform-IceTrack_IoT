package pe.edu.upc.ice.track.platform.reporting.application.internal.eventhandlers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.storage.ReportFileStorage;
import pe.edu.upc.ice.track.platform.reporting.domain.model.events.ReportDeletedEvent;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static pe.edu.upc.ice.track.platform.reporting.test.fixtures.ReportingDomainFixtures.FILE_KEY;
import static pe.edu.upc.ice.track.platform.reporting.test.fixtures.ReportingDomainFixtures.REPORT_ID;

@ExtendWith(MockitoExtension.class)
class ReportFileCleanupEventHandlerTest {

  @Mock private ReportFileStorage fileStorage;
  @InjectMocks private ReportFileCleanupEventHandler handler;

  @Test
  @DisplayName("on(ReportDeletedEvent) with a file key -> deletes that file once (AAA)")
  void deletesFile() {
    // Act
    handler.on(new ReportDeletedEvent(REPORT_ID, FILE_KEY));

    // Assert
    verify(fileStorage, times(1)).delete(FILE_KEY);
  }

  @Test
  @DisplayName("on(ReportDeletedEvent) without a file key -> storage untouched (AAA)")
  void nullKey() {
    // Act
    handler.on(new ReportDeletedEvent(REPORT_ID, null));

    // Assert
    verifyNoInteractions(fileStorage);
  }

  @Test
  @DisplayName("on(ReportDeletedEvent) when storage throws -> failure is swallowed (AAA)")
  void storageFailureSwallowed() {
    // Arrange
    doThrow(new RuntimeException("gcs down")).when(fileStorage).delete(FILE_KEY);

    // Act & Assert
    assertThatCode(() -> handler.on(new ReportDeletedEvent(REPORT_ID, FILE_KEY))).doesNotThrowAnyException();
    verify(fileStorage, times(1)).delete(FILE_KEY);
  }
}
