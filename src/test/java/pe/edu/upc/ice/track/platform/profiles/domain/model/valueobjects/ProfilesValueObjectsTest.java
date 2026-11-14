package pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfilesValueObjectsTest {

  @Nested
  class EmailAddressTest {

    @Test
    @DisplayName("new EmailAddress(mixed case with spaces) -> trimmed and lower-cased (AAA)")
    void normalizes() {
      // Act
      var email = new EmailAddress("  Ana.Torres@Example.COM ");

      // Assert
      assertThat(email.address()).isEqualTo("ana.torres@example.com");
      assertThat(email.getAddress()).isEqualTo("ana.torres@example.com");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("new EmailAddress(null or blank) -> IllegalArgumentException (AAA)")
    void rejectsBlank(String raw) {
      // Act & Assert
      assertThatThrownBy(() -> new EmailAddress(raw))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Email address must not be null or blank");
    }

    @ParameterizedTest
    @ValueSource(strings = {"plain", "a@b", "a@b.c", "@example.com", "a b@example.com"})
    @DisplayName("new EmailAddress(malformed) -> IllegalArgumentException (AAA)")
    void rejectsMalformed(String raw) {
      // Act & Assert
      assertThatThrownBy(() -> new EmailAddress(raw))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Email address must be a valid format");
    }
  }

  @Nested
  class PersonNameTest {

    @Test
    @DisplayName("new PersonName(first, last) -> trims both and builds the full name (AAA)")
    void trimsAndBuildsFullName() {
      // Act
      var name = new PersonName("  Ana ", " Torres ");

      // Assert
      assertThat(name.firstName()).isEqualTo("Ana");
      assertThat(name.lastName()).isEqualTo("Torres");
      assertThat(name.getFullName()).isEqualTo("Ana Torres");
    }

    @Test
    @DisplayName("new PersonName(first, null) -> empty last name and full name without trailing space (AAA)")
    void nullLastName() {
      // Act
      var name = new PersonName("Ana", null);

      // Assert
      assertThat(name.lastName()).isEmpty();
      assertThat(name.getFullName()).isEqualTo("Ana");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("new PersonName(null or blank first name) -> IllegalArgumentException (AAA)")
    void rejectsBlankFirstName(String first) {
      // Act & Assert
      assertThatThrownBy(() -> new PersonName(first, "Torres"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("First name must not be null or blank");
    }

    @Test
    @DisplayName("PersonName.fromDisplayName(\"Ana Maria Torres\") -> first word is the first name, rest the last name (AAA)")
    void fromDisplayNameSplitsAtFirstSpace() {
      // Act
      var name = PersonName.fromDisplayName("  Ana Maria Torres ");

      // Assert
      assertThat(name.firstName()).isEqualTo("Ana");
      assertThat(name.lastName()).isEqualTo("Maria Torres");
    }

    @Test
    @DisplayName("PersonName.fromDisplayName(single word) -> only a first name (AAA)")
    void fromDisplayNameSingleWord() {
      // Act & Assert
      assertThat(PersonName.fromDisplayName("Ana").lastName()).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("PersonName.fromDisplayName(null or blank) -> IllegalArgumentException (AAA)")
    void fromDisplayNameRejectsBlank(String raw) {
      // Act & Assert
      assertThatThrownBy(() -> PersonName.fromDisplayName(raw))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Display name must not be null or blank");
    }
  }

  @Nested
  class PhoneTest {

    @Test
    @DisplayName("new Phone(code, number) -> trims and joins them in the full number (AAA)")
    void trimsAndJoins() {
      // Act
      var phone = new Phone(" +51 ", " 987654321 ");

      // Assert
      assertThat(phone.countryCode()).isEqualTo("+51");
      assertThat(phone.getFullNumber()).isEqualTo("+51 987654321");
    }

    @Test
    @DisplayName("Phone.withoutCountryCode(number) -> empty country code, full number is the number (AAA)")
    void withoutCountryCode() {
      // Act
      var phone = Phone.withoutCountryCode("987654321");

      // Assert
      assertThat(phone.countryCode()).isEmpty();
      assertThat(phone.getFullNumber()).isEqualTo("987654321");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("new Phone(code, null or blank number) -> IllegalArgumentException (AAA)")
    void rejectsBlankNumber(String number) {
      // Act & Assert
      assertThatThrownBy(() -> new Phone("+51", number))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Phone number must not be null or blank");
    }

    @Test
    @DisplayName("Phone.fromString(\"+51 987654321\") -> splits country code and number (AAA)")
    void fromStringWithCountryCode() {
      // Act
      var phone = Phone.fromString(" +51 987 654 321 ");

      // Assert
      assertThat(phone.countryCode()).isEqualTo("+51");
      assertThat(phone.phoneNumber()).isEqualTo("987 654 321");
    }

    @ParameterizedTest
    @ValueSource(strings = {"987654321", "+51", "+ 51", "51 987"})
    @DisplayName("Phone.fromString(without a leading +code and number) -> everything is the number (AAA)")
    void fromStringWithoutCountryCode(String raw) {
      // Act
      var phone = Phone.fromString(raw);

      // Assert
      assertThat(phone.countryCode()).isEmpty();
      assertThat(phone.phoneNumber()).isEqualTo(raw);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("Phone.fromString(null or blank) -> IllegalArgumentException (AAA)")
    void fromStringRejectsBlank(String raw) {
      // Act & Assert
      assertThatThrownBy(() -> Phone.fromString(raw))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Phone number must not be null or blank");
    }
  }

  @Nested
  class RucTest {

    @ParameterizedTest
    @ValueSource(longs = {10_000_000_000L, 20123456789L, 99_999_999_999L})
    @DisplayName("new Ruc(11 digits) -> accepted at both boundaries (AAA)")
    void acceptsElevenDigits(long value) {
      // Act & Assert
      assertThat(new Ruc(value).value()).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 9_999_999_999L, 100_000_000_000L, -20123456789L})
    @DisplayName("new Ruc(not 11 digits) -> IllegalArgumentException (AAA)")
    void rejectsOutOfRange(long value) {
      // Act & Assert
      assertThatThrownBy(() -> new Ruc(value))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("RUC must be an 11 digit number");
    }

    @Test
    @DisplayName("new Ruc(null) -> IllegalArgumentException (AAA)")
    void rejectsNull() {
      // Act & Assert
      assertThatThrownBy(() -> new Ruc(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("RUC must not be null");
    }
  }

  @Nested
  class SpecialityTest {

    @Test
    @DisplayName("new Speciality(name) -> trimmed (AAA)")
    void trims() {
      // Act & Assert
      assertThat(new Speciality("  HVAC ").name()).isEqualTo("HVAC");
    }

    @Test
    @DisplayName("new Speciality(100 characters) -> accepted, 101 rejected (AAA)")
    void lengthBoundary() {
      // Act & Assert
      assertThat(new Speciality("a".repeat(Speciality.MAX_LENGTH)).name()).hasSize(100);
      assertThatThrownBy(() -> new Speciality("a".repeat(Speciality.MAX_LENGTH + 1)))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Speciality must not exceed 100 characters");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("new Speciality(null or blank) -> IllegalArgumentException (AAA)")
    void rejectsBlank(String raw) {
      // Act & Assert
      assertThatThrownBy(() -> new Speciality(raw))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Speciality must not be null or blank");
    }
  }

  @Nested
  class StreetAddressTest {

    @Test
    @DisplayName("new StreetAddress(all fields) -> trimmed and formatted with the number (AAA)")
    void formatsWithNumber() {
      // Act
      var address = new StreetAddress(" Av. Primavera ", " 120 ", " Lima ", " 15023 ", " Peru ");

      // Assert
      assertThat(address.getStreetAddress()).isEqualTo("Av. Primavera 120, Lima, 15023, Peru");
    }

    @Test
    @DisplayName("new StreetAddress(four-argument constructor) -> no number in the formatted address (AAA)")
    void formatsWithoutNumber() {
      // Act
      var address = new StreetAddress("Av. Primavera", "Lima", "15023", "Peru");

      // Assert
      assertThat(address.number()).isNull();
      assertThat(address.getStreetAddress()).isEqualTo("Av. Primavera, Lima, 15023, Peru");
    }

    @Test
    @DisplayName("new StreetAddress(blank number) -> number normalized to null (AAA)")
    void blankNumberBecomesNull() {
      // Act & Assert
      assertThat(new StreetAddress("Av. Primavera", "  ", "Lima", "15023", "Peru").number()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"street", "city", "postal code", "country"})
    @DisplayName("new StreetAddress(blank required field) -> IllegalArgumentException naming the field (AAA)")
    void rejectsBlankRequiredFields(String field) {
      // Arrange
      var street = field.equals("street") ? " " : "Av. Primavera";
      var city = field.equals("city") ? null : "Lima";
      var postal = field.equals("postal code") ? "" : "15023";
      var country = field.equals("country") ? " " : "Peru";

      // Act & Assert
      assertThatThrownBy(() -> new StreetAddress(street, "1", city, postal, country))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining(field.substring(0, 1).toUpperCase() + field.substring(1) + " must not be null or blank");
    }
  }

  @Nested
  class TemperatureRangeTest {

    @Test
    @DisplayName("new TemperatureRange(c, label) -> unit upper-cased and label trimmed (AAA)")
    void normalizes() {
      // Act
      var range = new TemperatureRange(2, 8, " c ", " Cold chain ");

      // Assert
      assertThat(range.unit()).isEqualTo("C");
      assertThat(range.label()).isEqualTo("Cold chain");
    }

    @Test
    @DisplayName("new TemperatureRange(min == max) -> accepted (AAA)")
    void equalBoundsAreValid() {
      // Act & Assert
      assertThat(new TemperatureRange(5, 5, "F", "Exact").min()).isEqualTo(5);
    }

    @Test
    @DisplayName("new TemperatureRange(min > max) -> IllegalArgumentException (AAA)")
    void rejectsInvertedBounds() {
      // Act & Assert
      assertThatThrownBy(() -> new TemperatureRange(9, 8, "C", "x"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Temperature range min must not be greater than max");
    }

    @Test
    @DisplayName("new TemperatureRange(null bounds) -> IllegalArgumentException (AAA)")
    void rejectsNullBounds() {
      // Act & Assert
      assertThatThrownBy(() -> new TemperatureRange(null, 8, "C", "x")).hasMessage("Temperature range min must not be null");
      assertThatThrownBy(() -> new TemperatureRange(2, null, "C", "x")).hasMessage("Temperature range max must not be null");
    }

    @Test
    @DisplayName("new TemperatureRange(unsupported unit) -> IllegalArgumentException (AAA)")
    void rejectsUnsupportedUnit() {
      // Act & Assert
      assertThatThrownBy(() -> new TemperatureRange(2, 8, "K", "x"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Temperature range unit must be C or F");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("new TemperatureRange(blank unit or label) -> IllegalArgumentException (AAA)")
    void rejectsBlankUnitAndLabel(String blank) {
      // Act & Assert
      assertThatThrownBy(() -> new TemperatureRange(2, 8, blank, "x"))
          .hasMessage("Temperature range unit must not be null or blank");
      assertThatThrownBy(() -> new TemperatureRange(2, 8, "C", blank))
          .hasMessage("Temperature range label must not be null or blank");
    }
  }

  @Nested
  class CardTypeTest {

    @ParameterizedTest
    @ValueSource(strings = {"open_alerts", " OPEN_ALERTS ", "Open_Alerts"})
    @DisplayName("CardType.fromString(any case, padded) -> OPEN_ALERTS (AAA)")
    void parsesIgnoringCaseAndSpaces(String raw) {
      // Act & Assert
      assertThat(CardType.fromString(raw)).isEqualTo(CardType.OPEN_ALERTS);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("CardType.fromString(null or blank) -> IllegalArgumentException (AAA)")
    void rejectsBlank(String raw) {
      // Act & Assert
      assertThatThrownBy(() -> CardType.fromString(raw))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Card type must not be null or blank");
    }

    @Test
    @DisplayName("CardType.fromString(unknown) -> IllegalArgumentException listing the valid values (AAA)")
    void rejectsUnknown() {
      // Act & Assert
      assertThatThrownBy(() -> CardType.fromString("nope"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Card type must be one of MONITORED_EQUIPMENT, OPEN_ALERTS, ACTIVE_ORDERS, EQUIPMENT_STATUS");
    }
  }

  @Nested
  class CardLayoutItemTest {

    @Test
    @DisplayName("new CardLayoutItem(null id or order) -> IllegalArgumentException (AAA)")
    void rejectsNulls() {
      // Act & Assert
      assertThatThrownBy(() -> new CardLayoutItem(null, 1, true))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Layout card identifier must not be null");
      assertThatThrownBy(() -> new CardLayoutItem(1L, null, true))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Layout card order must not be null");
    }

    @Test
    @DisplayName("new CardLayoutItem(valid) -> keeps its values (AAA)")
    void keepsValues() {
      // Act
      var item = new CardLayoutItem(4L, 2, false);

      // Assert
      assertThat(item.cardId()).isEqualTo(4L);
      assertThat(item.order()).isEqualTo(2);
      assertThat(item.visible()).isFalse();
    }
  }
}
