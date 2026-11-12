package pe.edu.upc.ice.track.platform.iam.test.fixtures;

import pe.edu.upc.ice.track.platform.iam.domain.model.commands.*;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.ClientChannel;

import static pe.edu.upc.ice.track.platform.iam.test.fixtures.IamDomainFixtures.*;

/** iam command factories. Each returns a fresh, valid command built from the canonical constants. */
public final class IamCommandFixtures {

  private IamCommandFixtures() {
    throw new AssertionError("IamCommandFixtures is a utility class and must not be instantiated.");
  }

  public static SignInByLocalCommand signInByLocalCommand(ClientChannel channel) {
    return new SignInByLocalCommand(USERNAME, PASSWORD, channel);
  }

  public static SignInByGoogleCommand signInByGoogleCommand(ClientChannel channel) {
    return new SignInByGoogleCommand(GOOGLE_ID_TOKEN, channel);
  }

  public static LinkGoogleAccountCommand linkGoogleAccountCommand(ClientChannel channel) {
    return new LinkGoogleAccountCommand(GOOGLE_ID_TOKEN, PASSWORD, channel);
  }

  public static RefreshTokenCommand refreshTokenCommand(ClientChannel channel) {
    return new RefreshTokenCommand(RAW_REFRESH_TOKEN, channel);
  }

  public static SignOutCommand signOutCommand() {
    return new SignOutCommand(RAW_REFRESH_TOKEN);
  }

  public static SignUpOwnerCommand signUpOwnerCommand() {
    return new SignUpOwnerCommand(USERNAME, PASSWORD, EMAIL, FULL_NAME, contactDetails(), RUC);
  }

  public static SignUpTechnicianCommand signUpTechnicianCommand() {
    return new SignUpTechnicianCommand(USERNAME, PASSWORD, EMAIL, FULL_NAME, contactDetails(), SPECIALITY,
        CERTIFICATION_NUMBER);
  }

  public static CompleteGoogleOwnerRegistrationCommand completeGoogleOwnerRegistrationCommand() {
    return new CompleteGoogleOwnerRegistrationCommand(GOOGLE_ID_TOKEN, USERNAME, contactDetails(), RUC);
  }

  public static CompleteGoogleTechnicianRegistrationCommand completeGoogleTechnicianRegistrationCommand() {
    return new CompleteGoogleTechnicianRegistrationCommand(GOOGLE_ID_TOKEN, USERNAME, contactDetails(),
        SPECIALITY, CERTIFICATION_NUMBER);
  }
}
