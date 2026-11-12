package pe.edu.upc.ice.track.platform.iam.domain.model.queries;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static pe.edu.upc.ice.track.platform.iam.test.fixtures.IamDomainFixtures.USERNAME;

class IamQueriesTest {

  @Test
  @DisplayName("GetCurrentUserQuery(String) -> keeps the username (AAA)")
  void currentUserKeepsUsername() {
    assertThat(new GetCurrentUserQuery(USERNAME).username()).isEqualTo(USERNAME);
  }

  @ParameterizedTest(name = "[{index}] username=''{0}''")
  @NullAndEmptySource
  @ValueSource(strings = {" "})
  @DisplayName("GetCurrentUserQuery(String) -> rejects a blank username (AAA)")
  void currentUserRejectsBlank(String username) {
    var ex = assertThrows(IllegalArgumentException.class, () -> new GetCurrentUserQuery(username));
    assertThat(ex).hasMessage("username must not be null or blank");
  }
}
