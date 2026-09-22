package pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects;

/**
 * PersonName Value Object.
 *
 * <p>The family name is optional: identity providers are not required to supply one, and many
 * people do not have a family name at all. The given name is always required.</p>
 */
public record PersonName(String firstName, String lastName) {

  /**
   * Full name getter
   * @return Full name
   */
  public String getFullName() {
    return "%s %s".formatted(firstName, lastName).trim();
  }

  /**
   * Constructor with validation and normalisation of the optional family name.
   * @param firstName First name
   * @param lastName Last name, optional
   */
  public PersonName {
    if (firstName == null || firstName.isBlank()) {
      throw new IllegalArgumentException("First name must not be null or blank");
    }
    firstName = firstName.trim();
    lastName = lastName == null ? "" : lastName.trim();
  }

  /**
   * Builds a person name from a single display name, as supplied by an external identity
   * provider in the OIDC {@code name} claim.
   *
   * <p>The first whitespace separated token becomes the given name and the remainder, when
   * present, the family name.</p>
   *
   * @param displayName the display name to split; must not be null or blank
   * @return the resulting person name
   * @throws IllegalArgumentException when the display name is null or blank
   */
  public static PersonName fromDisplayName(String displayName) {
    if (displayName == null || displayName.isBlank()) {
      throw new IllegalArgumentException("Display name must not be null or blank");
    }
    var trimmed = displayName.trim();
    var separatorIndex = trimmed.indexOf(' ');
    if (separatorIndex < 0) {
      return new PersonName(trimmed, null);
    }
    return new PersonName(trimmed.substring(0, separatorIndex), trimmed.substring(separatorIndex + 1).trim());
  }

}
