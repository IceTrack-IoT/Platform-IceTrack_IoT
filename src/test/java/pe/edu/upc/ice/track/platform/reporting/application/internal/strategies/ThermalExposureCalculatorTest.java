package pe.edu.upc.ice.track.platform.reporting.application.internal.strategies;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class ThermalExposureCalculatorTest {

  private static double[] s(double celsius, double epochSeconds) {
    return new double[] {celsius, epochSeconds};
  }

  @Test
  @DisplayName("of(samples, min, max) with no samples -> IllegalArgumentException (AAA)")
  void empty() {
    // Act & Assert
    assertThatThrownBy(() -> ThermalExposureCalculator.of(List.of(), 2, 8))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("At least one sample is required");
  }

  @Test
  @DisplayName("of(constant 5 C samples) -> MKT equals the constant, zero deviation, fully in range (AAA)")
  void constantTemperature() {
    // Arrange
    var samples = List.of(s(5, 0), s(5, 60), s(5, 120));

    // Act
    var exposure = ThermalExposureCalculator.of(samples, 2, 8);

    // Assert
    assertThat(exposure.meanKineticTemperature()).isCloseTo(5.0, within(1e-9));
    assertThat(exposure.standardDeviation()).isZero();
    assertThat(exposure.degreeMinutes()).isZero();
    assertThat(exposure.percentInRange()).isEqualTo(100d);
  }

  @Test
  @DisplayName("of(0 C and 20 C samples) -> MKT above the arithmetic mean of 10 C (AAA)")
  void mktWeighsHighTemperatures() {
    // Act
    var exposure = ThermalExposureCalculator.of(List.of(s(0, 0), s(20, 60)), -50, 50);

    // Assert
    assertThat(exposure.meanKineticTemperature()).isGreaterThan(10.0).isLessThan(20.0);
  }

  @Test
  @DisplayName("of(samples) -> sample standard deviation (n-1) of the readings (AAA)")
  void standardDeviation() {
    // Arrange
    var samples = List.of(s(2, 0), s(4, 1), s(4, 2), s(4, 3), s(5, 4), s(5, 5), s(7, 6), s(9, 7));

    // Act
    var exposure = ThermalExposureCalculator.of(samples, -100, 100);

    // Assert
    assertThat(exposure.standardDeviation()).isCloseTo(Math.sqrt(32.0 / 7.0), within(1e-9));
  }

  @Test
  @DisplayName("of(single sample) -> zero deviation and the lone reading decides range (AAA)")
  void singleSample() {
    // Act & Assert
    assertThat(ThermalExposureCalculator.of(List.of(s(5, 0)), 2, 8).standardDeviation()).isZero();
    assertThat(ThermalExposureCalculator.of(List.of(s(5, 0)), 2, 8).percentInRange()).isEqualTo(100d);
    assertThat(ThermalExposureCalculator.of(List.of(s(12, 0)), 2, 8).percentInRange()).isZero();
    assertThat(ThermalExposureCalculator.of(List.of(s(12, 0)), 2, 8).degreeMinutes()).isZero();
  }

  @Test
  @DisplayName("of(samples above the band) -> degree-minutes = deviation x minutes since previous sample (AAA)")
  void degreeMinutesAbove() {
    // Arrange: 10 C is 2 degrees over 8 C, held for 10 minutes; 5 C afterwards is in range
    var samples = List.of(s(5, 0), s(10, 600), s(5, 1200));

    // Act
    var exposure = ThermalExposureCalculator.of(samples, 2, 8);

    // Assert
    assertThat(exposure.degreeMinutes()).isCloseTo(20.0, within(1e-9));
    assertThat(exposure.percentInRange()).isCloseTo(50.0, within(1e-9));
  }

  @Test
  @DisplayName("of(samples below the band) -> deviation counted from the lower limit (AAA)")
  void degreeMinutesBelow() {
    // Arrange: 0 C is 2 degrees under 2 C for 5 minutes
    var samples = List.of(s(5, 0), s(0, 300));

    // Act
    var exposure = ThermalExposureCalculator.of(samples, 2, 8);

    // Assert
    assertThat(exposure.degreeMinutes()).isCloseTo(10.0, within(1e-9));
    assertThat(exposure.percentInRange()).isZero();
  }

  @Test
  @DisplayName("of(readings exactly on the limits) -> counted as in range with zero deviation (AAA)")
  void limitsInclusive() {
    // Act
    var exposure = ThermalExposureCalculator.of(List.of(s(2, 0), s(8, 60), s(2, 120)), 2, 8);

    // Assert
    assertThat(exposure.percentInRange()).isEqualTo(100d);
    assertThat(exposure.degreeMinutes()).isZero();
  }

  @Test
  @DisplayName("of(samples with a gap over one hour) -> gap weight capped at 60 minutes (AAA)")
  void gapIsClamped() {
    // Arrange: 2 hours between samples, but only 60 minutes can be attributed
    var samples = List.of(s(5, 0), s(10, 7200));

    // Act
    var exposure = ThermalExposureCalculator.of(samples, 2, 8);

    // Assert
    assertThat(exposure.degreeMinutes()).isCloseTo(120.0, within(1e-9));
  }

  @Test
  @DisplayName("of(samples out of chronological order) -> negative gap weighs zero and the first reading decides range (AAA)")
  void negativeGap() {
    // Act
    var exposure = ThermalExposureCalculator.of(List.of(s(5, 100), s(10, 0)), 2, 8);

    // Assert
    assertThat(exposure.degreeMinutes()).isZero();
    assertThat(exposure.percentInRange()).isEqualTo(100d);
  }
}
