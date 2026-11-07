package pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects;

import jakarta.validation.constraints.Email;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * EmailAddress Value Object.
 *
 * <p>The address is trimmed and lowercased on construction. This value object is the single
 * point every stored address and every lookup goes through, so {@code Ana@x.com} and
 * {@code ana@x.com} are the same address everywhere: they can never back two profiles, and a
 * Google sign-in finds the profile whatever case either side used.</p>
 */
public record EmailAddress(@Email String address) {
  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

  public EmailAddress {
    if (address == null || address.isBlank()) {
      throw new IllegalArgumentException("Email address must not be null or blank");
    }
    address = address.trim().toLowerCase(Locale.ROOT);
    if (!EMAIL_PATTERN.matcher(address).matches()) {
      throw new IllegalArgumentException("Email address must be a valid format");
    }
  }

  public String getAddress() {
    return address;
  }
}
