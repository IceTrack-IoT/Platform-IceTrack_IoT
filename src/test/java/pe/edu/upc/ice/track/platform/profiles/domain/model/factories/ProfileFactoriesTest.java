package pe.edu.upc.ice.track.platform.profiles.domain.model.factories;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.OwnerProfile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.TechnicianProfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.CERTIFICATION;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.USER_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.address;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.email;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.ownerData;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.personName;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.phone;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.ruc;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.speciality;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.technicianData;

class ProfileFactoriesTest {

  @Test
  @DisplayName("ProfileCreationData.forOwner(...) -> carries the RUC and no technician data (AAA)")
  void forOwner() {
    // Act
    var data = ownerData();

    // Assert
    assertThat(data.ruc()).isEqualTo(ruc());
    assertThat(data.speciality()).isNull();
    assertThat(data.certificationNumber()).isNull();
  }

  @Test
  @DisplayName("ProfileCreationData.forTechnician(...) -> carries speciality and certification, no RUC (AAA)")
  void forTechnician() {
    // Act
    var data = technicianData();

    // Assert
    assertThat(data.speciality()).isEqualTo(speciality());
    assertThat(data.certificationNumber()).isEqualTo(CERTIFICATION);
    assertThat(data.ruc()).isNull();
  }

  @Test
  @DisplayName("ProfileCreationData(null common field) / forOwner(null ruc) / forTechnician(null speciality) -> NullPointerException (AAA)")
  void creationDataInvalid() {
    // Act & Assert
    assertThatThrownBy(() -> new ProfileCreationData(null, email(), phone(), address(), null, null, null))
        .hasMessage("fullName must not be null");
    assertThatThrownBy(() -> new ProfileCreationData(personName(), null, phone(), address(), null, null, null))
        .hasMessage("email must not be null");
    assertThatThrownBy(() -> new ProfileCreationData(personName(), email(), null, address(), null, null, null))
        .hasMessage("phone must not be null");
    assertThatThrownBy(() -> new ProfileCreationData(personName(), email(), phone(), null, null, null, null))
        .hasMessage("address must not be null");
    assertThatThrownBy(() -> ProfileCreationData.forOwner(personName(), email(), phone(), address(), null))
        .hasMessage("ruc must not be null");
    assertThatThrownBy(() -> ProfileCreationData.forTechnician(personName(), email(), phone(), address(), null, CERTIFICATION))
        .hasMessage("speciality must not be null");
  }

  @Test
  @DisplayName("OwnerProfileFactory.createProfile(user, data) -> new OwnerProfile without id built from the data (AAA)")
  void ownerFactory() {
    // Act
    OwnerProfile owner = new OwnerProfileFactory().createProfile(USER_ID, ownerData());

    // Assert
    assertThat(owner.getUserProfileId()).isNull();
    assertThat(owner.getUserId().userId()).isEqualTo(USER_ID);
    assertThat(owner.getRuc()).isEqualTo(ruc());
    assertThat(owner.getEmail()).isEqualTo(email());
  }

  @Test
  @DisplayName("OwnerProfileFactory.createProfile(null data or data without ruc) -> NullPointerException (AAA)")
  void ownerFactoryInvalid() {
    // Arrange
    var factory = new OwnerProfileFactory();
    var dataWithoutRuc = technicianData();

    // Act & Assert
    assertThatThrownBy(() -> factory.createProfile(USER_ID, null)).hasMessage("data must not be null");
    assertThatThrownBy(() -> factory.createProfile(USER_ID, dataWithoutRuc)).hasMessage("An owner profile requires a RUC");
  }

  @Test
  @DisplayName("TechnicianProfileFactory.createProfile(user, data) -> new TechnicianProfile built from the data (AAA)")
  void technicianFactory() {
    // Act
    TechnicianProfile technician = new TechnicianProfileFactory().createProfile(USER_ID, technicianData());

    // Assert
    assertThat(technician.getUserProfileId()).isNull();
    assertThat(technician.getSpeciality()).isEqualTo(speciality());
    assertThat(technician.getCertificationNumber()).isEqualTo(CERTIFICATION);
  }

  @Test
  @DisplayName("TechnicianProfileFactory.createProfile(null data or data without speciality) -> NullPointerException (AAA)")
  void technicianFactoryInvalid() {
    // Arrange
    var factory = new TechnicianProfileFactory();
    var dataWithoutSpeciality = new ProfileCreationData(personName(), email(), phone(), address(), ruc(), null, CERTIFICATION);

    // Act & Assert
    assertThatThrownBy(() -> factory.createProfile(USER_ID, null)).hasMessage("data must not be null");
    assertThatThrownBy(() -> factory.createProfile(USER_ID, dataWithoutSpeciality))
        .hasMessage("A technician profile requires a speciality");
  }

  @Test
  @DisplayName("createProfile(null user id) -> IllegalArgumentException from UserId (AAA)")
  void factoryRejectsNullUser() {
    // Act & Assert
    assertThatThrownBy(() -> new OwnerProfileFactory().createProfile(null, ownerData()))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
