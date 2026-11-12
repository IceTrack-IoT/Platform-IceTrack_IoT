package pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import pe.edu.upc.ice.track.platform.profiles.domain.model.events.OwnerCreatedEvent;
import pe.edu.upc.ice.track.platform.profiles.domain.model.events.TechnicianCreatedEvent;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.PersonName;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.Phone;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.Ruc;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.Speciality;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.StreetAddress;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.CERTIFICATION;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.EMAIL;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.OWNER_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.RUC;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.TECHNICIAN_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.USER_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.address;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.email;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.owner;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.persistedOwner;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.persistedTechnician;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.personName;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.phone;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.ruc;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.speciality;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.technician;

class ProfileAggregatesTest {

  @Nested
  class OwnerProfileTest {

    @Test
    @DisplayName("new OwnerProfile(...) -> keeps every value and has no id yet (AAA)")
    void construction() {
      // Act
      var owner = owner();

      // Assert
      assertThat(owner.getUserProfileId()).isNull();
      assertThat(owner.getUserId()).isEqualTo(new UserId(USER_ID));
      assertThat(owner.getFullName()).isEqualTo(personName());
      assertThat(owner.getEmail()).isEqualTo(email());
      assertThat(owner.getPhone()).isEqualTo(phone());
      assertThat(owner.getAddress()).isEqualTo(address());
      assertThat(owner.getRuc()).isEqualTo(ruc());
      assertThat(owner.getVersion()).isNull();
    }

    @Test
    @DisplayName("new OwnerProfile(null ruc) -> NullPointerException (AAA)")
    void nullRucRejected() {
      // Act & Assert
      assertThatThrownBy(() -> new OwnerProfile(new UserId(USER_ID), personName(), email(), phone(), address(), null))
          .isInstanceOf(NullPointerException.class).hasMessage("ruc must not be null");
    }

    @Test
    @DisplayName("new OwnerProfile(user without identifier) -> NullPointerException (AAA)")
    void invalidUserRejected() {
      // Act & Assert
      assertThatThrownBy(() -> new UserId(null))
          .isInstanceOf(IllegalArgumentException.class).hasMessage("User identifier must be a positive number");
      assertThatThrownBy(() -> new OwnerProfile(null, personName(), email(), phone(), address(), ruc()))
          .isInstanceOf(NullPointerException.class).hasMessage("userId must not be null");
    }

    @Test
    @DisplayName("new OwnerProfile(null name, email, phone or address) -> NullPointerException (AAA)")
    void nullCommonFieldsRejected() {
      var user = new UserId(USER_ID);

      // Act & Assert
      assertThatThrownBy(() -> new OwnerProfile(user, null, email(), phone(), address(), ruc()))
          .hasMessage("fullName must not be null");
      assertThatThrownBy(() -> new OwnerProfile(user, personName(), null, phone(), address(), ruc()))
          .hasMessage("email must not be null");
      assertThatThrownBy(() -> new OwnerProfile(user, personName(), email(), null, address(), ruc()))
          .hasMessage("phone must not be null");
      assertThatThrownBy(() -> new OwnerProfile(user, personName(), email(), phone(), null, ruc()))
          .hasMessage("address must not be null");
    }

    @Test
    @DisplayName("updateInfo(name, phone, address) -> replaces them and keeps the email (AAA)")
    void updateInfo() {
      // Arrange
      var owner = persistedOwner();
      var newName = new PersonName("Maria", "Lopez");
      var newPhone = new Phone("+51", "911222333");
      var newAddress = new StreetAddress("Calle Sol", "Cusco", "08000", "Peru");

      // Act
      owner.updateInfo(newName, newPhone, newAddress);

      // Assert
      assertThat(owner.getFullName()).isEqualTo(newName);
      assertThat(owner.getPhone()).isEqualTo(newPhone);
      assertThat(owner.getAddress()).isEqualTo(newAddress);
      assertThat(owner.getEmail().address()).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("updateInfo(null ...) -> NullPointerException and the profile is not partially changed (AAA)")
    void updateInfoRejectsNull() {
      // Arrange
      var owner = persistedOwner();

      // Act & Assert
      assertThatThrownBy(() -> owner.updateInfo(null, phone(), address())).hasMessage("fullName must not be null");
      assertThatThrownBy(() -> owner.updateInfo(personName(), null, address())).hasMessage("phone must not be null");
      assertThatThrownBy(() -> owner.updateInfo(personName(), phone(), null)).hasMessage("address must not be null");
      assertThat(owner.getFullName()).isEqualTo(personName());
    }

    @Test
    @DisplayName("updateTaxRegistration(Ruc) -> replaces the RUC (AAA)")
    void updateTaxRegistration() {
      // Arrange
      var owner = persistedOwner();

      // Act
      owner.updateTaxRegistration(new Ruc(10987654321L));

      // Assert
      assertThat(owner.getRuc().value()).isEqualTo(10987654321L);
      assertThatThrownBy(() -> owner.updateTaxRegistration(null)).hasMessage("ruc must not be null");
    }

    @Test
    @DisplayName("restoreVersion(Long) -> exposes the version read from storage (AAA)")
    void restoreVersion() {
      // Arrange
      var owner = persistedOwner();

      // Act
      owner.restoreVersion(7L);

      // Assert
      assertThat(owner.getVersion()).isEqualTo(7L);
    }

    @Test
    @DisplayName("onCreated() -> registers one OwnerCreatedEvent describing the owner (AAA)")
    void onCreatedRegistersEvent() {
      // Arrange
      var owner = persistedOwner();

      // Act
      owner.onCreated();

      // Assert
      assertThat(owner.domainEvents()).containsExactly(
          new OwnerCreatedEvent(OWNER_ID, USER_ID, "Ana Torres", EMAIL, RUC));
    }
  }

  @Nested
  class TechnicianProfileTest {

    @Test
    @DisplayName("new TechnicianProfile(...) -> keeps speciality and trimmed certification number (AAA)")
    void construction() {
      // Act
      var technician = new TechnicianProfile(new UserId(USER_ID), personName(), email(), phone(), address(),
          speciality(), "  CERT-001 ");

      // Assert
      assertThat(technician.getSpeciality()).isEqualTo(speciality());
      assertThat(technician.getCertificationNumber()).isEqualTo(CERTIFICATION);
    }

    @Test
    @DisplayName("new TechnicianProfile(null speciality) -> NullPointerException (AAA)")
    void nullSpecialityRejected() {
      // Act & Assert
      assertThatThrownBy(() -> new TechnicianProfile(new UserId(USER_ID), personName(), email(), phone(), address(),
          null, CERTIFICATION)).hasMessage("speciality must not be null");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("new TechnicianProfile(null or blank certification) -> IllegalArgumentException (AAA)")
    void blankCertificationRejected(String certification) {
      // Act & Assert
      assertThatThrownBy(() -> new TechnicianProfile(new UserId(USER_ID), personName(), email(), phone(), address(),
          speciality(), certification))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Certification number must not be null or blank");
    }

    @ParameterizedTest
    @ValueSource(strings = {"AB", "-ABC", ".ABC", "AB C", "AB#1", "A11111111111111111111111111111111111111111111111111"})
    @DisplayName("new TechnicianProfile(malformed certification) -> IllegalArgumentException (AAA)")
    void malformedCertificationRejected(String certification) {
      // Act & Assert
      assertThatThrownBy(() -> new TechnicianProfile(new UserId(USER_ID), personName(), email(), phone(), address(),
          speciality(), certification))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageStartingWith("Certification number must be 3 to 50");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC", "a1/2-3.4", "A1111111111111111111111111111111111111111111111111"})
    @DisplayName("new TechnicianProfile(well-formed certification) -> accepted (AAA)")
    void wellFormedCertificationAccepted(String certification) {
      // Act & Assert
      assertThat(new TechnicianProfile(new UserId(USER_ID), personName(), email(), phone(), address(), speciality(),
          certification).getCertificationNumber()).isEqualTo(certification);
    }

    @Test
    @DisplayName("updateCertification(speciality, number) -> replaces both (AAA)")
    void updateCertification() {
      // Arrange
      var technician = persistedTechnician();

      // Act
      technician.updateCertification(new Speciality("HVAC"), "HV-77");

      // Assert
      assertThat(technician.getSpeciality().name()).isEqualTo("HVAC");
      assertThat(technician.getCertificationNumber()).isEqualTo("HV-77");
    }

    @Test
    @DisplayName("updateCertification(speciality, invalid number) -> rejected without changing the speciality (AAA)")
    void updateCertificationIsAtomic() {
      // Arrange
      var technician = persistedTechnician();

      // Act & Assert
      assertThatThrownBy(() -> technician.updateCertification(new Speciality("HVAC"), "x"))
          .isInstanceOf(IllegalArgumentException.class);
      assertThat(technician.getSpeciality()).isEqualTo(speciality());
      assertThat(technician.getCertificationNumber()).isEqualTo(CERTIFICATION);
    }

    @Test
    @DisplayName("updateCertification(null speciality, number) -> NullPointerException (AAA)")
    void updateCertificationNullSpeciality() {
      // Arrange
      var technician = persistedTechnician();

      // Act & Assert
      assertThatThrownBy(() -> technician.updateCertification(null, "HV-77")).hasMessage("speciality must not be null");
      assertThat(technician.getCertificationNumber()).isEqualTo(CERTIFICATION);
    }

    @Test
    @DisplayName("isAvailableFor(speciality) -> true ignoring case, false otherwise or for null (AAA)")
    void availability() {
      // Arrange
      var technician = technician();

      // Act & Assert
      assertThat(technician.isAvailableFor(new Speciality("REFRIGERATION"))).isTrue();
      assertThat(technician.isAvailableFor(new Speciality("HVAC"))).isFalse();
      assertThat(technician.isAvailableFor(null)).isFalse();
    }

    @Test
    @DisplayName("onCreated() -> registers one TechnicianCreatedEvent describing the technician (AAA)")
    void onCreatedRegistersEvent() {
      // Arrange
      var technician = persistedTechnician();

      // Act
      technician.onCreated();

      // Assert
      assertThat(technician.domainEvents()).containsExactly(new TechnicianCreatedEvent(
          TECHNICIAN_ID, USER_ID, "Ana Torres", EMAIL, "Refrigeration", CERTIFICATION));
    }
  }
}
