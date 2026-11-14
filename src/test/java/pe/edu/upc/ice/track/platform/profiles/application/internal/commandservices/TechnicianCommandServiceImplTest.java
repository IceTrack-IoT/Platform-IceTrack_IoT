package pe.edu.upc.ice.track.platform.profiles.application.internal.commandservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.TechnicianProfile;
import pe.edu.upc.ice.track.platform.profiles.domain.repositories.ProfileRepository;
import pe.edu.upc.ice.track.platform.profiles.domain.repositories.TechnicianProfileRepository;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesCommandFixtures.createTechnicianCommand;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesCommandFixtures.updateTechnicianCommand;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.CERTIFICATION;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.TECHNICIAN_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.USER_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.email;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.personName;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.phone;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.address;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.speciality;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.persistedTechnician;
import static pe.edu.upc.ice.track.platform.shared.test.util.ResultTestUtils.failureError;
import static pe.edu.upc.ice.track.platform.shared.test.util.ResultTestUtils.successValue;

@ExtendWith(MockitoExtension.class)
class TechnicianCommandServiceImplTest {

  @Mock
  private TechnicianProfileRepository technicianProfileRepository;
  @Mock
  private ProfileRepository profileRepository;

  @InjectMocks
  private TechnicianCommandServiceImpl service;

  @Test
  @DisplayName("handle(CreateTechnicianCommand) -> saves a new technician built from the data (AAA)")
  void createSucceeds() {
    // Arrange
    when(profileRepository.existsByUserId(new UserId(USER_ID))).thenReturn(false);
    when(profileRepository.existsByEmailAddress(email())).thenReturn(false);
    when(technicianProfileRepository.save(any(TechnicianProfile.class))).thenAnswer(i -> i.getArgument(0));

    // Act
    var technician = successValue(service.handle(createTechnicianCommand()));

    // Assert
    assertThat(technician.getCertificationNumber()).isEqualTo(CERTIFICATION);
    assertThat(technician.getSpeciality().name()).isEqualTo("Refrigeration");
    verify(technicianProfileRepository, times(1)).save(technician);
  }

  @Test
  @DisplayName("handle(CreateTechnicianCommand) user already has a profile -> TECHNICIAN_CONFLICT (AAA)")
  void createRejectsExistingUser() {
    // Arrange
    when(profileRepository.existsByUserId(new UserId(USER_ID))).thenReturn(true);

    // Act
    var error = failureError(service.handle(createTechnicianCommand()));

    // Assert
    assertThat(error.code()).isEqualTo("TECHNICIAN_CONFLICT");
    assertThat(error.details()).isEqualTo("User '9' already has a profile");
    verifyNoInteractions(technicianProfileRepository);
  }

  @Test
  @DisplayName("handle(CreateTechnicianCommand) e-mail already registered -> TECHNICIAN_CONFLICT (AAA)")
  void createRejectsExistingEmail() {
    // Arrange
    when(profileRepository.existsByUserId(new UserId(USER_ID))).thenReturn(false);
    when(profileRepository.existsByEmailAddress(email())).thenReturn(true);

    // Act
    var error = failureError(service.handle(createTechnicianCommand()));

    // Assert
    assertThat(error.code()).isEqualTo("TECHNICIAN_CONFLICT");
    assertThat(error.details()).contains("ana.torres@example.com");
    verifyNoInteractions(technicianProfileRepository);
  }

  @Test
  @DisplayName("handle(CreateTechnicianCommand) invalid certification -> VALIDATION_ERROR, nothing saved (AAA)")
  void createMapsValidation() {
    // Arrange
    var command = new pe.edu.upc.ice.track.platform.profiles.domain.model.commands.CreateTechnicianCommand(
        new UserId(USER_ID), pe.edu.upc.ice.track.platform.profiles.domain.model.factories.ProfileCreationData
        .forTechnician(personName(), email(), phone(), address(), speciality(), "x"));
    when(profileRepository.existsByUserId(new UserId(USER_ID))).thenReturn(false);
    when(profileRepository.existsByEmailAddress(email())).thenReturn(false);

    // Act
    var error = failureError(service.handle(command));

    // Assert
    assertThat(error.code()).isEqualTo("VALIDATION_ERROR");
    assertThat(error.details()).startsWith("Certification number must be 3 to 50");
    verifyNoInteractions(technicianProfileRepository);
  }

  @Test
  @DisplayName("handle(CreateTechnicianCommand) unexpected failure -> UNEXPECTED_ERROR (AAA)")
  void createMapsUnexpected() {
    // Arrange
    when(profileRepository.existsByUserId(new UserId(USER_ID))).thenThrow(new IllegalStateException("db down"));

    // Act
    var error = failureError(service.handle(createTechnicianCommand()));

    // Assert
    assertThat(error.code()).isEqualTo("UNEXPECTED_ERROR");
    assertThat(error.message()).isEqualTo("Unexpected error in Technician creation");
  }

  @Test
  @DisplayName("handle(UpdateTechnicianCommand) unknown technician -> TECHNICIAN_NOT_FOUND (AAA)")
  void updateNotFound() {
    // Arrange
    when(technicianProfileRepository.findById(TECHNICIAN_ID)).thenReturn(Optional.empty());

    // Act
    var error = failureError(service.handle(updateTechnicianCommand("HV-77")));

    // Assert
    assertThat(error.code()).isEqualTo("TECHNICIAN_NOT_FOUND");
    verify(technicianProfileRepository, never()).save(any());
  }

  @Test
  @DisplayName("handle(UpdateTechnicianCommand) -> applies info and certification and saves (AAA)")
  void updateSucceeds() {
    // Arrange
    var technician = persistedTechnician();
    when(technicianProfileRepository.findById(TECHNICIAN_ID)).thenReturn(Optional.of(technician));
    when(technicianProfileRepository.save(technician)).thenReturn(technician);

    // Act
    var saved = successValue(service.handle(updateTechnicianCommand("HV-77")));

    // Assert
    assertThat(saved.getFullName().getFullName()).isEqualTo("Luis Perez");
    assertThat(saved.getSpeciality().name()).isEqualTo("HVAC");
    assertThat(saved.getCertificationNumber()).isEqualTo("HV-77");
    assertThat(saved.getAddress().city()).isEqualTo("Arequipa");
    verify(technicianProfileRepository, times(1)).save(technician);
  }

  @Test
  @DisplayName("handle(UpdateTechnicianCommand) malformed certification -> VALIDATION_ERROR, nothing saved (AAA)")
  void updateMapsValidation() {
    // Arrange
    var technician = persistedTechnician();
    when(technicianProfileRepository.findById(TECHNICIAN_ID)).thenReturn(Optional.of(technician));

    // Act
    var error = failureError(service.handle(updateTechnicianCommand("x!")));

    // Assert
    assertThat(error.code()).isEqualTo("VALIDATION_ERROR");
    verify(technicianProfileRepository, never()).save(any());
  }

  @Test
  @DisplayName("handle(UpdateTechnicianCommand) unexpected failure -> UNEXPECTED_ERROR (AAA)")
  void updateMapsUnexpected() {
    // Arrange
    var technician = persistedTechnician();
    when(technicianProfileRepository.findById(TECHNICIAN_ID)).thenReturn(Optional.of(technician));
    when(technicianProfileRepository.save(technician)).thenThrow(new IllegalStateException("boom"));

    // Act
    var error = failureError(service.handle(updateTechnicianCommand("HV-77")));

    // Assert
    assertThat(error.code()).isEqualTo("UNEXPECTED_ERROR");
    assertThat(error.message()).isEqualTo("Unexpected error in Technician update");
  }
}
