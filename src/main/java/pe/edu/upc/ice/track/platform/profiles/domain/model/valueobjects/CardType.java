package pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Card Type Value Object.
 *
 * <p>The kind of widget a {@code DashboardCard} renders on a user's dashboard. Every dashboard
 * holds exactly one card of each type.</p>
 *
 * <p>The declaration order is the default dashboard layout: {@code DashboardConfig} provisions and
 * resets its cards in this sequence, at positions {@code 1..N}. Reordering the constants changes
 * that default; it never corrupts stored cards, which persist the type by name.</p>
 */
public enum CardType {
  MONITORED_EQUIPMENT,
  OPEN_ALERTS,
  ACTIVE_ORDERS,
  EQUIPMENT_STATUS;

  /**
   * Resolves a card type from its name, ignoring case and surrounding blanks.
   *
   * @param rawCardType the card type name, such as {@code "open_alerts"}; required
   * @return the matching card type
   * @throws IllegalArgumentException when the value is missing or matches no card type
   */
  public static CardType fromString(String rawCardType) {
    if (rawCardType == null || rawCardType.isBlank()) {
      throw new IllegalArgumentException("Card type must not be null or blank");
    }
    try {
      return CardType.valueOf(rawCardType.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Card type must be one of %s".formatted(
          Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", "))));
    }
  }
}
