package pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects;

/**
 * Speciality Value Object.
 *
 * <p>The maintenance field a technician practises, such as refrigeration or ice resurfacing.</p>
 *
 * @param name the speciality name; required
 */
public record Speciality(String name) {

  /**
   * Longest speciality name that can be stored.
   */
  public static final int MAX_LENGTH = 100;

  /**
   * Validates and normalises the speciality name.
   */
  public Speciality {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Speciality must not be null or blank");
    }
    name = name.trim();
    if (name.length() > MAX_LENGTH) {
      throw new IllegalArgumentException("Speciality must not exceed %d characters".formatted(MAX_LENGTH));
    }
  }
}
