package pe.edu.upc.ice.track.platform.reporting.application.internal.queryservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.KpiValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static pe.edu.upc.ice.track.platform.reporting.test.fixtures.ReportingDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class KpiPayloadReaderTest {

  @Mock private ObjectMapper objectMapper;
  @InjectMocks private KpiPayloadReader reader;

  @Test
  @DisplayName("read(COMPLETED report) -> parsed KPIs with null entries dropped (AAA)")
  void parses() {
    // Arrange
    var report = completedReport();
    var kpi = new KpiValue("k", "Label", 1.0, "%");
    doReturn(new KpiValue[] {kpi, null}).when(objectMapper).readValue(report.getResultPayload(), KpiValue[].class);

    // Act
    var result = reader.read(report);

    // Assert
    assertThat(result).hasValueSatisfying(values -> assertThat(values).containsExactly(kpi));
  }

  @Test
  @DisplayName("read(COMPLETED report) when the mapper answers null -> present and empty (AAA)")
  void nullArray() {
    // Arrange
    var report = completedReport();
    doReturn(null).when(objectMapper).readValue(anyString(), eq(KpiValue[].class));

    // Act & Assert
    assertThat(reader.read(report)).hasValueSatisfying(values -> assertThat(values).isEmpty());
  }

  @Test
  @DisplayName("read(null / non-COMPLETED report) -> empty, mapper untouched (AAA)")
  void notReadable() {
    // Act & Assert
    assertThat(reader.read(null)).isEmpty();
    assertThat(reader.read(pendingReport())).isEmpty();
    assertThat(reader.read(failedReport())).isEmpty();
    verifyNoInteractions(objectMapper);
  }

  @Test
  @DisplayName("read(COMPLETED report with unparsable payload) -> empty instead of failing (AAA)")
  void unparsable() {
    // Arrange
    var report = completedReport();
    doThrow(mock(JacksonException.class)).when(objectMapper).readValue(anyString(), eq(KpiValue[].class));

    // Act & Assert
    assertThat(reader.read(report)).isEmpty();
  }
}
