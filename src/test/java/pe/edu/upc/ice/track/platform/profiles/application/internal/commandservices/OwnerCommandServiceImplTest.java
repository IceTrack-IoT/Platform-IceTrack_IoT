package pe.edu.upc.ice.track.platform.profiles.application.internal.commandservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.OwnerProfile;
import pe.edu.upc.ice.track.platform.profiles.domain.repositories.OwnerProfileRepository;
import pe.edu.upc.ice.track.platform.profiles.domain.repositories.ProfileRepository;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesCommandFixtures.createOwnerCommand;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesCommandFixtures.updateOwnerCommand;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.OWNER_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.USER_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.email;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.persistedOwner;
import static pe.edu.upc.ice.track.platform.shared.test.util.ResultTestUtils.failureError;
import static pe.edu.upc.ice.track.platform.shared.test.util.ResultTestUtils.successValue;

@ExtendWith(MockitoExtension.class)
class OwnerCommandServiceImplTest {

  @Mock
  private OwnerProfileRepository ownerProfileRepository;
  @Mock
  private ProfileRepository profileRepository;

  @InjectMocks
  private OwnerCommandServiceImpl service;

  @Test
  @DisplayName("handle(CreateOwnerCommand) -> saves a new owner built from the data (AAA)")
  void createSucceeds() {
    // Arrange
    when(profileRepository.existsByUserId(new UserId(USER_ID))).thenReturn(false);
    when(profileRepository.existsByEmailAddress(email())).thenReturn(false);
    when(ownerProfileRepository.save(any(OwnerProfile.class))).thenAnswer(i -> i.getArgument(0));

    // Act
    var result = service.handle(createOwnerCommand());

    // Assert
    var owner = successValue(result);
    assertThat(owner.getUserId().userId()).isEqualTo(USER_ID);
    assertThat(owner.getRuc().value()).isEqualTo(20123456789L);
    verify(ownerProfileRepository, times(1)).save(owner);
  }

  @Test
  @DisplayName("handle(CreateOwnerCommand) user already has a profile -> OWNER_CONFLICT and nothing saved (AAA)")
  void createRejectsExistingUser() {
    // Arrange
    when(profileRepository.existsByUserId(new UserId(USER_ID))).thenReturn(true);

    // Act
    var error = failureError(service.handle(createOwnerCommand()));

    // Assert
    assertThat(error.code()).isEqualTo("OWNER_CONFLICT");
    assertThat(error.details()).isEqualTo("User '9' already has a profile");
    verifyNoInteractions(ownerProfileRepository);
  }

  @Test
  @DisplayName("handle(CreateOwnerCommand) e-mail already registered -> OWNER_CONFLICT and nothing saved (AAA)")
  void createRejectsExistingEmail() {
    // Arrange
    when(profileRepository.existsByUserId(new UserId(USER_ID))).thenReturn(false);
    when(profileRepository.existsByEmailAddress(email())).thenReturn(true);

    // Act
    var error = failureError(service.handle(createOwnerCommand()));

    // Assert
    assertThat(error.code()).isEqualTo("OWNER_CONFLICT");
    assertThat(error.details()).isEqualTo("A profile with email address 'ana.torres@example.com' already exists");
    verifyNoInteractions(ownerProfileRepository);
  }

  @Test
  @DisplayName("handle(CreateOwnerCommand) repository rejects with IllegalArgumentException -> VALIDATION_ERROR (AAA)")
  void createMapsIllegalArgument() {
    // Arrange
    when(profileRepository.existsByUserId(new UserId(USER_ID))).thenReturn(false);
    when(profileRepository.existsByEmailAddress(email())).thenReturn(false);
    when(ownerProfileRepository.save(any(OwnerProfile.class))).thenThrow(new IllegalArgumentException("bad ruc"));

    // Act
    var error = failureError(service.handle(createOwnerCommand()));

    // Assert
    assertThat(error.code()).isEqualTo("VALIDATION_ERROR");
    assertThat(error.details()).isEqualTo("bad ruc");
  }

  @Test
  @DisplayName("handle(CreateOwnerCommand) unexpected failure -> UNEXPECTED_ERROR (AAA)")
  void createMapsUnexpected() {
    // Arrange
    when(profileRepository.existsByUserId(new UserId(USER_ID))).thenThrow(new IllegalStateException("db down"));

    // Act
    var error = failureError(service.handle(createOwnerCommand()));

    // Assert
    assertThat(error.code()).isEqualTo("UNEXPECTED_ERROR");
    assertThat(error.message()).isEqualTo("Unexpected error in Owner creation");
    assertThat(error.details()).isEqualTo("db down");
  }

  @Test
  @DisplayName("handle(UpdateOwnerCommand) unknown owner -> OWNER_NOT_FOUND (AAA)")
  void updateNotFound() {
    // Arrange
    when(ownerProfileRepository.findById(OWNER_ID)).thenReturn(Optional.empty());

    // Act
    var error = failureError(service.handle(updateOwnerCommand(null)));

    // Assert
    assertThat(error.code()).isEqualTo("OWNER_NOT_FOUND");
    verify(ownerProfileRepository, never()).save(any());
  }

  @Test
  @DisplayName("handle(UpdateOwnerCommand) stale expected version -> CONCURRENT_MODIFICATION_CONFLICT, nothing saved (AAA)")
  void updateStaleVersion() {
    // Arrange
    var owner = persistedOwner();
    when(ownerProfileRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));

    // Act
    var error = failureError(service.handle(updateOwnerCommand(1L)));

    // Assert
    assertThat(error.code()).isEqualTo("CONCURRENT_MODIFICATION_CONFLICT");
    assertThat(owner.getFullName().getFullName()).isEqualTo("Ana Torres");
    verify(ownerProfileRepository, never()).save(any());
  }

  @Test
  @DisplayName("handle(UpdateOwnerCommand) matching version -> applies info and RUC and saves (AAA)")
  void updateWithMatchingVersion() {
    // Arrange
    var owner = persistedOwner();
    when(ownerProfileRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
    when(ownerProfileRepository.save(owner)).thenReturn(owner);

    // Act
    var result = service.handle(updateOwnerCommand(2L));

    // Assert
    var saved = successValue(result);
    assertThat(saved.getFullName().getFullName()).isEqualTo("Maria Lopez");
    assertThat(saved.getPhone().getFullNumber()).isEqualTo("+51 911222333");
    assertThat(saved.getAddress().city()).isEqualTo("Cusco");
    assertThat(saved.getRuc().value()).isEqualTo(10987654321L);
    verify(ownerProfileRepository, times(1)).save(owner);
  }

  @Test
  @DisplayName("handle(UpdateOwnerCommand) without expected version -> no precondition, saves (AAA)")
  void updateWithoutVersion() {
    // Arrange
    var owner = persistedOwner();
    when(ownerProfileRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
    when(ownerProfileRepository.save(owner)).thenReturn(owner);

    // Act & Assert
    assertThat(service.handle(updateOwnerCommand(null))).isNotNull();
    verify(ownerProfileRepository, times(1)).save(owner);
  }

  @Test
  @DisplayName("handle(UpdateOwnerCommand) optimistic lock failure -> rethrown for the transaction (AAA)")
  void updateRethrowsOptimisticLock() {
    // Arrange
    var owner = persistedOwner();
    when(ownerProfileRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
    when(ownerProfileRepository.save(owner)).thenThrow(new OptimisticLockingFailureException("stale"));

    // Act & Assert
    assertThatThrownBy(() -> service.handle(updateOwnerCommand(null)))
        .isInstanceOf(OptimisticLockingFailureException.class);
  }

  @Test
  @DisplayName("handle(UpdateOwnerCommand) IllegalArgumentException / other failure -> VALIDATION_ERROR / UNEXPECTED_ERROR (AAA)")
  void updateMapsFailures() {
    // Arrange
    var owner = persistedOwner();
    when(ownerProfileRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
    when(ownerProfileRepository.save(owner))
        .thenThrow(new IllegalArgumentException("bad"))
        .thenThrow(new IllegalStateException("boom"));

    // Act
    var validation = failureError(service.handle(updateOwnerCommand(null)));
    var unexpected = failureError(service.handle(updateOwnerCommand(null)));

    // Assert
    assertThat(validation.code()).isEqualTo("VALIDATION_ERROR");
    assertThat(unexpected.code()).isEqualTo("UNEXPECTED_ERROR");
    assertThat(unexpected.message()).isEqualTo("Unexpected error in Owner update");
  }
}
