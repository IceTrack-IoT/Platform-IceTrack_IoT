package pe.edu.upc.ice.track.platform.reporting.application.internal.strategies;

import java.util.List;

/**
 * Computes the cold chain metrics of a unit's temperature history: Mean Kinetic Temperature, standard
 * deviation, degree-minutes outside the threshold band, and the share of time inside the band.
 *
 * <p>Input samples are {@code {celsius, epochSeconds}} rows in time order, as the monitoring context
 * returns them, already without readings taken during maintenance.</p>
 *
 * <p><strong>Mean Kinetic Temperature</strong> follows the Arrhenius form used by pharmacopoeial
 * guidance, with an activation energy of 83.144 kJ/mol: {@code MKT = (dH/R) / -ln(mean(exp(-dH/(R*T_i))))},
 * where {@code T_i} are the sample temperatures in kelvin. Samples are weighted equally, because a
 * periodic telemetry stream records equal intervals.</p>
 *
 * <p><strong>Time weighting.</strong> Degree-minutes and the share in range weigh each sample by the
 * time since the previous one, capped at {@value #MAX_SAMPLE_GAP_MINUTES} minutes so that a long
 * telemetry outage is not credited as if the unit had been at its last temperature throughout.</p>
 */
public final class ThermalExposureCalculator {

  /** Activation energy divided by the gas constant, in kelvin: 83144 J/mol over 8.314 J/(mol*K). */
  static final double ACTIVATION_OVER_GAS_CONSTANT = 10_000.0;

  /** Offset between degrees Celsius and kelvin. */
  static final double KELVIN_OFFSET = 273.15;

  /** Largest time credited to one sample, in minutes. */
  static final long MAX_SAMPLE_GAP_MINUTES = 60;

  private static final long MAX_SAMPLE_GAP_SECONDS = MAX_SAMPLE_GAP_MINUTES * 60;

  private ThermalExposureCalculator() {
  }

  /**
   * The metrics of one unit over one range.
   *
   * @param meanKineticTemperature the MKT, in degrees Celsius
   * @param standardDeviation      the sample standard deviation of the temperatures, in degrees Celsius
   * @param degreeMinutes          the time-weighted degrees outside the band, in degree-minutes
   * @param percentInRange         the share of time inside the band, from 0 to 100
   */
  public record Exposure(double meanKineticTemperature, double standardDeviation,
                         double degreeMinutes, double percentInRange) {
  }

  /**
   * Computes the exposure metrics.
   *
   * @param samples time-ordered {@code {celsius, epochSeconds}} rows; must not be empty
   * @param minCelsius lower bound of the acceptable band
   * @param maxCelsius upper bound of the acceptable band
   * @return the metrics
   * @throws IllegalArgumentException when there are no samples
   */
  public static Exposure of(List<double[]> samples, double minCelsius, double maxCelsius) {
    if (samples.isEmpty()) {
      throw new IllegalArgumentException("At least one sample is required");
    }
    return new Exposure(
        meanKineticTemperature(samples),
        standardDeviation(samples),
        degreeMinutes(samples, minCelsius, maxCelsius),
        percentInRange(samples, minCelsius, maxCelsius));
  }

  private static double meanKineticTemperature(List<double[]> samples) {
    var sum = 0d;
    for (var sample : samples) {
      sum += Math.exp(-ACTIVATION_OVER_GAS_CONSTANT / (sample[0] + KELVIN_OFFSET));
    }
    var meanExponential = sum / samples.size();
    var mktKelvin = ACTIVATION_OVER_GAS_CONSTANT / -Math.log(meanExponential);
    return mktKelvin - KELVIN_OFFSET;
  }

  private static double standardDeviation(List<double[]> samples) {
    if (samples.size() < 2) {
      return 0d;
    }
    var mean = samples.stream().mapToDouble(sample -> sample[0]).average().orElse(0d);
    var squares = samples.stream().mapToDouble(sample -> Math.pow(sample[0] - mean, 2)).sum();
    return Math.sqrt(squares / (samples.size() - 1));
  }

  private static double degreeMinutes(List<double[]> samples, double minCelsius, double maxCelsius) {
    var total = 0d;
    for (var i = 1; i < samples.size(); i++) {
      var celsius = samples.get(i)[0];
      var deviation = Math.max(0, celsius - maxCelsius) + Math.max(0, minCelsius - celsius);
      total += deviation * weightMinutes(samples, i);
    }
    return total;
  }

  private static double percentInRange(List<double[]> samples, double minCelsius, double maxCelsius) {
    var totalWeight = 0d;
    var inRangeWeight = 0d;
    for (var i = 1; i < samples.size(); i++) {
      var weight = weightMinutes(samples, i);
      var celsius = samples.get(i)[0];
      totalWeight += weight;
      if (celsius >= minCelsius && celsius <= maxCelsius) {
        inRangeWeight += weight;
      }
    }
    if (totalWeight == 0d) {
      // A single sample, or samples recorded at the same instant: the only reading decides.
      var celsius = samples.getFirst()[0];
      return celsius >= minCelsius && celsius <= maxCelsius ? 100d : 0d;
    }
    return inRangeWeight / totalWeight * 100d;
  }

  /**
   * The minutes a sample is credited with: the time since the previous sample, capped.
   *
   * @param samples the samples
   * @param index   the index of a sample after the first
   * @return the credited minutes
   */
  private static double weightMinutes(List<double[]> samples, int index) {
    var seconds = samples.get(index)[1] - samples.get(index - 1)[1];
    return Math.clamp(seconds, 0d, MAX_SAMPLE_GAP_SECONDS) / 60d;
  }
}
