package pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects;

/**
 * ReviewRating Value Object.
 *
 * <p>The three scores an owner gives the technician who completed a service request. Each score is
 * an integer from {@value #MIN_SCORE} to {@value #MAX_SCORE}; the same rule is re-asserted by a
 * {@code CHECK} constraint on the {@code service_request_reviews} table.</p>
 *
 * @param communication how well the technician kept the owner informed; required
 * @param efficacy      whether the intervention solved the problem; required
 * @param performance   how well the work itself was carried out; required
 */
public record ReviewRating(Integer communication, Integer efficacy, Integer performance) {

  /** Lowest score a criterion may receive. */
  public static final int MIN_SCORE = 1;

  /** Highest score a criterion may receive. */
  public static final int MAX_SCORE = 5;

  /**
   * Validates every score.
   *
   * @throws IllegalArgumentException when a score is missing or outside {@code MIN_SCORE..MAX_SCORE}
   */
  public ReviewRating {
    requireScore(communication, "communication");
    requireScore(efficacy, "efficacy");
    requireScore(performance, "performance");
  }

  /**
   * Computes the mean of the three scores.
   *
   * @return the average score, between {@value #MIN_SCORE} and {@value #MAX_SCORE}
   */
  public Double average() {
    return (communication + efficacy + performance) / 3.0;
  }

  private static void requireScore(Integer score, String criterion) {
    if (score == null || score < MIN_SCORE || score > MAX_SCORE) {
      throw new IllegalArgumentException(
          "Rating %s must be between %d and %d".formatted(criterion, MIN_SCORE, MAX_SCORE));
    }
  }
}
