package pe.edu.upc.ice.track.platform.iam.application.internal.outboundservices.google;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static pe.edu.upc.ice.track.platform.iam.test.fixtures.IamDomainFixtures.EMAIL;
import static pe.edu.upc.ice.track.platform.iam.test.fixtures.IamDomainFixtures.GOOGLE_SUB;

class GoogleUserInfoTest {

  private static GoogleUserInfo info(String name, String given, String family, String email) {
    return new GoogleUserInfo(GOOGLE_SUB, email, true, name, given, family, null);
  }

  @Test
  @DisplayName("displayName() -> prefers the trimmed full name (AAA)")
  void prefersFullName() {
    assertThat(info("  Juan Perez ", "J", "P", EMAIL).displayName()).isEqualTo("Juan Perez");
  }

  @Test
  @DisplayName("displayName() -> composes given and family names when the full name is blank (AAA)")
  void composesGivenAndFamilyName() {
    assertThat(info(" ", "Juan", "Perez", EMAIL).displayName()).isEqualTo("Juan Perez");
    assertThat(info(null, "Juan", null, EMAIL).displayName()).isEqualTo("Juan");
    assertThat(info(null, null, "Perez", EMAIL).displayName()).isEqualTo("Perez");
  }

  @Test
  @DisplayName("displayName() -> falls back to the e-mail local part (AAA)")
  void fallsBackToEmailLocalPart() {
    assertThat(info(null, null, null, "jperez@example.com").displayName()).isEqualTo("jperez");
    assertThat(info("", " ", "", "jperez@example.com").displayName()).isEqualTo("jperez");
  }

  @Test
  @DisplayName("displayName() -> uses the whole e-mail when it has no usable local part (AAA)")
  void usesWholeEmailWithoutLocalPart() {
    assertThat(info(null, null, null, "no-at-sign").displayName()).isEqualTo("no-at-sign");
    assertThat(info(null, null, null, "@example.com").displayName()).isEqualTo("@example.com");
  }
}
