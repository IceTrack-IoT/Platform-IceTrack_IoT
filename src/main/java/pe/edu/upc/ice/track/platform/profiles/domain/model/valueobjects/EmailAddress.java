package pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects;

import jakarta.validation.constraints.Email;

import java.util.regex.Pattern;

/**
 * EmailAddress Value Object.
 */
public record EmailAddress(@Email String address) {
  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

  public EmailAddress {
    if (address == null || address.isBlank()) {
      throw new IllegalArgumentException("Email address must not be null or blank");
    }
    if (!EMAIL_PATTERN.matcher(address).matches()) {
      throw new IllegalArgumentException("Email address must be a valid format");
    }
  }

  public String getAddress() {
    return address;
  }
}
