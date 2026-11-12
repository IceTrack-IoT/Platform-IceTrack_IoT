package pe.edu.upc.ice.track.platform.iam.application.internal.queryservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.iam.application.internal.outboundservices.acl.ExternalProfileService;
import pe.edu.upc.ice.track.platform.iam.domain.model.queries.*;
import pe.edu.upc.ice.track.platform.iam.domain.repositories.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.iam.test.fixtures.IamDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceImplTest {

  @Mock
  private UserRepository userRepository;
  @Mock
  private ExternalProfileService externalProfileService;

  @InjectMocks
  private UserQueryServiceImpl service;

  @Test
  @DisplayName("handle(GetAllUsersQuery) -> returns every stored user (AAA)")
  void returnsAllUsers() {
    // Arrange
    var users = List.of(localOwner(), localTechnician());
    when(userRepository.findAll()).thenReturn(users);

    // Act
    var result = service.handle(new GetAllUsersQuery());

    // Assert
    assertThat(result).isEqualTo(users);
  }

  @Test
  @DisplayName("handle(GetUserByIdQuery) -> returns the user found by id (AAA)")
  void returnsUserById() {
    // Arrange
    var user = localOwner();
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

    // Act
    var result = service.handle(new GetUserByIdQuery(USER_ID));

    // Assert
    assertThat(result).containsSame(user);
  }

  @Test
  @DisplayName("handle(GetUserByIdQuery) -> returns empty when the user does not exist (AAA)")
  void returnsEmptyWhenIdMissing() {
    // Arrange
    when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

    // Act & Assert
    assertThat(service.handle(new GetUserByIdQuery(USER_ID))).isEmpty();
  }

  @Test
  @DisplayName("handle(GetUserByUsernameQuery) -> returns the user found by username (AAA)")
  void returnsUserByUsername() {
    // Arrange
    var user = localOwner();
    when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

    // Act & Assert
    assertThat(service.handle(new GetUserByUsernameQuery(USERNAME))).containsSame(user);
  }

  @Test
  @DisplayName("handle(GetCurrentUserQuery) -> resolves the authenticated user by username (AAA)")
  void returnsCurrentUser() {
    // Arrange
    var user = localOwner();
    when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

    // Act & Assert
    assertThat(service.handle(new GetCurrentUserQuery(USERNAME))).containsSame(user);
  }

  @Test
  @DisplayName("handle(GetCurrentUserQuery) -> a null query returns empty without touching the repository (AAA)")
  void currentUserNullQuery() {
    // Act & Assert
    assertThat(service.handle((GetCurrentUserQuery) null)).isEmpty();
    verifyNoInteractions(userRepository);
  }

  @Test
  @DisplayName("handle(GetUserEmailByUserIdQuery) -> delegates to the profile lookup (AAA)")
  void returnsEmailByUserId() {
    // Arrange
    when(externalProfileService.fetchEmailByUserId(USER_ID)).thenReturn(Optional.of(EMAIL));

    // Act
    var result = service.handle(new GetUserEmailByUserIdQuery(USER_ID));

    // Assert
    assertThat(result).contains(EMAIL);
    verify(externalProfileService, times(1)).fetchEmailByUserId(USER_ID);
  }

  @Test
  @DisplayName("handle(GetUserEmailByUserIdQuery) -> null query or null id returns empty without lookups (AAA)")
  void emailQueryWithoutId() {
    // Act & Assert
    assertThat(service.handle((GetUserEmailByUserIdQuery) null)).isEmpty();
    assertThat(service.handle(new GetUserEmailByUserIdQuery(null))).isEmpty();
    verifyNoInteractions(externalProfileService);
  }
}
