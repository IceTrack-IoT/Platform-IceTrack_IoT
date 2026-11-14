package pe.edu.upc.ice.track.platform.profiles.interfaces.acl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.profiles.application.commandservices.OwnerCommandService;
import pe.edu.upc.ice.track.platform.profiles.application.commandservices.TechnicianCommandService;
import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.OwnerProfile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.TechnicianProfile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.CreateOwnerCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.CreateTechnicianCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.repositories.ProfileRepository;
import pe.edu.upc.ice.track.platform.profiles.domain.repositories.TechnicianProfileRepository;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.EMAIL;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.OWNER_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.TECHNICIAN_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.USER_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.owner;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.persistedOwner;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.persistedTechnician;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.technician;

@ExtendWith(MockitoExtension.class)
class ProfilesContextFacadeTest {

  private static final UserId USER = new UserId(USER_ID);

  @Mock
  private OwnerCommandService ownerCommandService;
  @Mock
  private TechnicianCommandService technicianCommandService;
  @Mock
  private ProfileRepository profileRepository;
  @Mock
  private TechnicianProfileRepository technicianProfileRepository;

  @InjectMocks
  private ProfilesContextFacade facade;

  private Long createOwner(Long userId, String email) {
    return facade.createOwnerProfile(userId, "Ana Maria Torres", email, "+51 987654321", "Av. Primavera", "120",
        "Lima", "15023", "Peru", 20123456789L);
  }

  private Long createTechnician(Long userId, String certification) {
    return facade.createTechnicianProfile(userId, "Luis Perez", "luis@example.com", "987654321", "Jr. Luna", null,
        "Arequipa", "04000", "Peru", "HVAC", certification);
  }

  @Nested
  class CreateProfiles {

    @Test
    @DisplayName("createOwnerProfile(...) -> builds the command from raw values and returns the new profile id (AAA)")
    void createOwnerSucceeds() {
      // Arrange
      when(ownerCommandService.handle(any(CreateOwnerCommand.class))).thenReturn(Result.success(persistedOwner()));

      // Act
      var id = createOwner(USER_ID, " Ana.Torres@Example.com ");

      // Assert
      assertThat(id).isEqualTo(OWNER_ID);
      var captor = ArgumentCaptor.forClass(CreateOwnerCommand.class);
      verify(ownerCommandService).handle(captor.capture());
      var command = captor.getValue();
      assertThat(command.userId()).isEqualTo(USER);
      assertThat(command.data().fullName().firstName()).isEqualTo("Ana");
      assertThat(command.data().fullName().lastName()).isEqualTo("Maria Torres");
      assertThat(command.data().email().address()).isEqualTo(EMAIL);
      assertThat(command.data().phone().countryCode()).isEqualTo("+51");
      assertThat(command.data().address().number()).isEqualTo("120");
      assertThat(command.data().ruc().value()).isEqualTo(20123456789L);
    }

    @Test
    @DisplayName("createOwnerProfile(null user / null e-mail / invalid RUC) -> IllegalArgumentException, command service untouched (AAA)")
    void createOwnerInvalidInput() {
      // Act & Assert
      assertThatThrownBy(() -> createOwner(null, EMAIL))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("A profile cannot be created without a user identifier");
      assertThatThrownBy(() -> createOwner(USER_ID, null))
          .isInstanceOf(IllegalArgumentException.class).hasMessage("Email address must not be null or blank");
      assertThatThrownBy(() -> facade.createOwnerProfile(USER_ID, "Ana", EMAIL, "987", "S", null, "C", "1", "P", 5L))
          .isInstanceOf(IllegalArgumentException.class).hasMessage("RUC must be an 11 digit number");
      verifyNoInteractions(ownerCommandService);
    }

    @Test
    @DisplayName("createOwnerProfile(...) conflict failure -> IllegalStateException with the details (AAA)")
    void createOwnerConflict() {
      // Arrange
      when(ownerCommandService.handle(any(CreateOwnerCommand.class)))
          .thenReturn(Result.failure(ApplicationError.conflict("Owner", "User '9' already has a profile")));

      // Act & Assert
      assertThatThrownBy(() -> createOwner(USER_ID, EMAIL))
          .isInstanceOf(IllegalStateException.class).hasMessage("User '9' already has a profile");
    }

    @Test
    @DisplayName("createOwnerProfile(...) validation failure -> IllegalArgumentException with the details (AAA)")
    void createOwnerValidation() {
      // Arrange
      when(ownerCommandService.handle(any(CreateOwnerCommand.class)))
          .thenReturn(Result.failure(ApplicationError.validationError("Owner", "bad value")));

      // Act & Assert
      assertThatThrownBy(() -> createOwner(USER_ID, EMAIL))
          .isInstanceOf(IllegalArgumentException.class).hasMessage("bad value");
    }

    @Test
    @DisplayName("createOwnerProfile(...) other failure -> RuntimeException, falling back to the message without details (AAA)")
    void createOwnerUnexpected() {
      // Arrange
      when(ownerCommandService.handle(any(CreateOwnerCommand.class)))
          .thenReturn(Result.failure(ApplicationError.unexpected("Owner creation", "db down")))
          .thenReturn(Result.failure(ApplicationError.notFound("Owner", "9")));

      // Act & Assert
      assertThatThrownBy(() -> createOwner(USER_ID, EMAIL))
          .isExactlyInstanceOf(RuntimeException.class).hasMessage("db down");
      assertThatThrownBy(() -> createOwner(USER_ID, EMAIL))
          .isExactlyInstanceOf(RuntimeException.class).hasMessage("Owner not found: 9");
    }

    @Test
    @DisplayName("createTechnicianProfile(...) -> builds the command and returns the new profile id (AAA)")
    void createTechnicianSucceeds() {
      // Arrange
      when(technicianCommandService.handle(any(CreateTechnicianCommand.class)))
          .thenReturn(Result.success(persistedTechnician()));

      // Act
      var id = createTechnician(USER_ID, "HV-77");

      // Assert
      assertThat(id).isEqualTo(TECHNICIAN_ID);
      var captor = ArgumentCaptor.forClass(CreateTechnicianCommand.class);
      verify(technicianCommandService).handle(captor.capture());
      assertThat(captor.getValue().data().speciality().name()).isEqualTo("HVAC");
      assertThat(captor.getValue().data().certificationNumber()).isEqualTo("HV-77");
      assertThat(captor.getValue().data().address().number()).isNull();
    }

    @Test
    @DisplayName("createTechnicianProfile(null user / blank certification) -> IllegalArgumentException, service untouched (AAA)")
    void createTechnicianInvalidInput() {
      // Act & Assert
      assertThatThrownBy(() -> createTechnician(null, "HV-77")).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> createTechnician(USER_ID, " "))
          .isInstanceOf(IllegalArgumentException.class).hasMessage("Certification number must not be null or blank");
      verifyNoInteractions(technicianCommandService);
    }

    @Test
    @DisplayName("createTechnicianProfile(...) conflict failure -> IllegalStateException (AAA)")
    void createTechnicianConflict() {
      // Arrange
      when(technicianCommandService.handle(any(CreateTechnicianCommand.class)))
          .thenReturn(Result.failure(ApplicationError.conflict("Technician", "duplicate e-mail")));

      // Act & Assert
      assertThatThrownBy(() -> createTechnician(USER_ID, "HV-77"))
          .isInstanceOf(IllegalStateException.class).hasMessage("duplicate e-mail");
    }
  }

  @Nested
  class Lookups {

    @Test
    @DisplayName("existsProfileByUserId(Long) -> false for null, repository answer otherwise (AAA)")
    void existsProfile() {
      // Arrange
      when(profileRepository.existsByUserId(USER)).thenReturn(true);

      // Act & Assert
      assertThat(facade.existsProfileByUserId(null)).isFalse();
      assertThat(facade.existsProfileByUserId(USER_ID)).isTrue();
    }

    @Test
    @DisplayName("fetchEmailByUserId(Long) -> e-mail, empty text for null or unknown user (AAA)")
    void fetchEmail() {
      // Arrange
      when(profileRepository.findByUserId(USER)).thenReturn(Optional.of(owner()));
      doReturn(Optional.empty()).when(profileRepository).findByUserId(new UserId(77L));

      // Act & Assert
      assertThat(facade.fetchEmailByUserId(USER_ID)).isEqualTo(EMAIL);
      assertThat(facade.fetchEmailByUserId(null)).isEmpty();
      assertThat(facade.fetchEmailByUserId(77L)).isEmpty();
    }

    @Test
    @DisplayName("fetchUserIdByEmail(text) -> account of the profile; 0 for blank, malformed or unknown e-mail (AAA)")
    void fetchUserIdByEmail() {
      // Arrange
      when(profileRepository.findByEmailAddress(new pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.EmailAddress(EMAIL)))
          .thenReturn(Optional.of(owner()));
      doReturn(Optional.empty()).when(profileRepository)
          .findByEmailAddress(new pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.EmailAddress("other@example.com"));

      // Act & Assert
      assertThat(facade.fetchUserIdByEmail("  ANA.TORRES@example.com ")).isEqualTo(USER_ID);
      assertThat(facade.fetchUserIdByEmail(null)).isZero();
      assertThat(facade.fetchUserIdByEmail("   ")).isZero();
      assertThat(facade.fetchUserIdByEmail("not-an-email")).isZero();
      assertThat(facade.fetchUserIdByEmail("other@example.com")).isZero();
    }

    @Test
    @DisplayName("fetchOwnerIdByUserId(Long) -> owner profile id; 0 for null, unknown or a technician account (AAA)")
    void fetchOwnerId() {
      // Arrange
      when(profileRepository.findByUserId(USER)).thenReturn(Optional.of(persistedOwner()));
      doReturn(Optional.of(persistedTechnician())).when(profileRepository).findByUserId(new UserId(11L));
      doReturn(Optional.empty()).when(profileRepository).findByUserId(new UserId(12L));

      // Act & Assert
      assertThat(facade.fetchOwnerIdByUserId(USER_ID)).isEqualTo(OWNER_ID);
      assertThat(facade.fetchOwnerIdByUserId(null)).isZero();
      assertThat(facade.fetchOwnerIdByUserId(11L)).isZero();
      assertThat(facade.fetchOwnerIdByUserId(12L)).isZero();
    }

    @Test
    @DisplayName("fetchTechnicianIdByUserId(Long) -> technician profile id; 0 for null, unknown or an owner account (AAA)")
    void fetchTechnicianId() {
      // Arrange
      when(profileRepository.findByUserId(USER)).thenReturn(Optional.of(persistedTechnician()));
      doReturn(Optional.of(persistedOwner())).when(profileRepository).findByUserId(new UserId(11L));
      doReturn(Optional.empty()).when(profileRepository).findByUserId(new UserId(12L));

      // Act & Assert
      assertThat(facade.fetchTechnicianIdByUserId(USER_ID)).isEqualTo(TECHNICIAN_ID);
      assertThat(facade.fetchTechnicianIdByUserId(null)).isZero();
      assertThat(facade.fetchTechnicianIdByUserId(11L)).isZero();
      assertThat(facade.fetchTechnicianIdByUserId(12L)).isZero();
    }

    @Test
    @DisplayName("isTechnicianEligibleForAssignment(Long) -> true only when the technician profile exists (AAA)")
    void eligibility() {
      // Arrange
      when(technicianProfileRepository.findById(TECHNICIAN_ID)).thenReturn(Optional.of(persistedTechnician()));
      doReturn(Optional.empty()).when(technicianProfileRepository).findById(99L);

      // Act & Assert
      assertThat(facade.isTechnicianEligibleForAssignment(TECHNICIAN_ID)).isTrue();
      assertThat(facade.isTechnicianEligibleForAssignment(99L)).isFalse();
      assertThat(facade.isTechnicianEligibleForAssignment(null)).isFalse();
    }

    @Test
    @DisplayName("fetchTechnicianFullNames(ids) -> names of the existing technicians; null, empty and duplicates handled (AAA)")
    void fullNames() {
      // Arrange
      when(technicianProfileRepository.findById(TECHNICIAN_ID)).thenReturn(Optional.of(persistedTechnician()));
      doReturn(Optional.empty()).when(technicianProfileRepository).findById(99L);
      var ids = new java.util.ArrayList<Long>(List.of(TECHNICIAN_ID, TECHNICIAN_ID, 99L));
      ids.add(null);

      // Act & Assert
      assertThat(facade.fetchTechnicianFullNames(ids)).isEqualTo(Map.of(TECHNICIAN_ID, "Ana Torres"));
      assertThat(facade.fetchTechnicianFullNames(null)).isEmpty();
      assertThat(facade.fetchTechnicianFullNames(List.of())).isEmpty();
      verify(technicianProfileRepository, org.mockito.Mockito.times(1)).findById(TECHNICIAN_ID);
      verify(technicianProfileRepository, never()).findById(null);
    }
  }
}
