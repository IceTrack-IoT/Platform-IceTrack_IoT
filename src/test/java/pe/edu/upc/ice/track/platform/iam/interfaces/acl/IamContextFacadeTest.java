package pe.edu.upc.ice.track.platform.iam.interfaces.acl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.iam.application.queryservices.UserQueryService;
import pe.edu.upc.ice.track.platform.iam.domain.model.queries.GetUserByIdQuery;
import pe.edu.upc.ice.track.platform.iam.domain.model.queries.GetUserByUsernameQuery;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.iam.test.fixtures.IamDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class IamContextFacadeTest {

  @Mock
  private UserQueryService userQueryService;

  @InjectMocks
  private IamContextFacade facade;

  @Test
  @DisplayName("fetchUserIdByUsername(String) -> returns the id of the matching user (AAA)")
  void fetchUserIdFound() {
    // Arrange
    when(userQueryService.handle(new GetUserByUsernameQuery(USERNAME))).thenReturn(Optional.of(localOwner()));

    // Act & Assert
    assertThat(facade.fetchUserIdByUsername(USERNAME)).isEqualTo(USER_ID);
  }

  @Test
  @DisplayName("fetchUserIdByUsername(String) -> returns 0 when the user does not exist (AAA)")
  void fetchUserIdMissing() {
    // Arrange
    when(userQueryService.handle(new GetUserByUsernameQuery(USERNAME))).thenReturn(Optional.empty());

    // Act & Assert
    assertThat(facade.fetchUserIdByUsername(USERNAME)).isZero();
  }

  @Test
  @DisplayName("fetchUsernameByUserId(Long) -> returns the username of the matching user (AAA)")
  void fetchUsernameFound() {
    // Arrange
    when(userQueryService.handle(new GetUserByIdQuery(USER_ID))).thenReturn(Optional.of(localOwner()));

    // Act & Assert
    assertThat(facade.fetchUsernameByUserId(USER_ID)).isEqualTo(USERNAME);
  }

  @Test
  @DisplayName("fetchUsernameByUserId(Long) -> returns an empty string when the user does not exist (AAA)")
  void fetchUsernameMissing() {
    // Arrange
    when(userQueryService.handle(new GetUserByIdQuery(USER_ID))).thenReturn(Optional.empty());

    // Act & Assert
    assertThat(facade.fetchUsernameByUserId(USER_ID)).isEmpty();
  }

  @Test
  @DisplayName("existsUserById(Long) -> true when the user exists, false otherwise (AAA)")
  void existsUserById() {
    // Arrange
    when(userQueryService.handle(new GetUserByIdQuery(USER_ID))).thenReturn(Optional.of(localOwner()));
    doReturn(Optional.empty()).when(userQueryService).handle(new GetUserByIdQuery(2L));

    // Act & Assert
    assertThat(facade.existsUserById(USER_ID)).isTrue();
    assertThat(facade.existsUserById(2L)).isFalse();
  }

  @Test
  @DisplayName("existsUserById(Long) -> a null id is false without querying (AAA)")
  void existsUserByNullId() {
    // Act & Assert
    assertThat(facade.existsUserById(null)).isFalse();
    verifyNoInteractions(userQueryService);
  }

  @Test
  @DisplayName("fetchAccountByUsername(String) -> maps the user to an AccountSummary (AAA)")
  void fetchAccountSummary() {
    // Arrange
    when(userQueryService.handle(new GetUserByUsernameQuery(USERNAME))).thenReturn(Optional.of(googleOwner()));

    // Act
    var summary = facade.fetchAccountByUsername(USERNAME);

    // Assert
    assertThat(summary).contains(new AccountSummary(USER_ID, USERNAME, "OWNER_ROLE", "GOOGLE"));
    verify(userQueryService, times(1)).handle(new GetUserByUsernameQuery(USERNAME));
  }

  @Test
  @DisplayName("fetchAccountByUsername(String) -> empty when the user does not exist (AAA)")
  void fetchAccountMissing() {
    // Arrange
    when(userQueryService.handle(new GetUserByUsernameQuery(USERNAME))).thenReturn(Optional.empty());

    // Act & Assert
    assertThat(facade.fetchAccountByUsername(USERNAME)).isEmpty();
  }

  @Test
  @DisplayName("fetchAccountByUsername(String) -> a null or blank username is empty without querying (AAA)")
  void fetchAccountBlankUsername() {
    // Act & Assert
    assertThat(facade.fetchAccountByUsername(null)).isEmpty();
    assertThat(facade.fetchAccountByUsername("  ")).isEmpty();
    verifyNoInteractions(userQueryService);
  }
}
