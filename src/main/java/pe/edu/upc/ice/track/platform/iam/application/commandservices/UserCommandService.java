package pe.edu.upc.ice.track.platform.iam.application.commandservices;

import org.apache.commons.lang3.tuple.ImmutablePair;
import pe.edu.upc.ice.track.platform.iam.domain.model.aggregates.User;
import pe.edu.upc.ice.track.platform.iam.domain.model.commands.ExchangeGoogleTokenCommand;
import pe.edu.upc.ice.track.platform.iam.domain.model.commands.SignInByLocalCommand;
import pe.edu.upc.ice.track.platform.iam.domain.model.commands.SignUpByLocalCommand;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

/**
 * Application service contract for IAM user commands.
 */
public interface UserCommandService {

  /**
   * Handles the sign-in command by local authentication.
   *
   * @param command the sign-in command containing the user's credentials
   * @return a Result containing an ImmutablePair of the authenticated User and a JWT token if successful, or an ApplicationError if failed
   */
  Result<ImmutablePair<User, String>, ApplicationError> handle(SignInByLocalCommand command);


  /**
   * Handles the sign-up command by local authentication.
   *
   * @param command the sign-up command containing the user's registration details
   * @return a Result containing the newly created User if successful, or an ApplicationError if failed
   */
  Result<User, ApplicationError> handle(SignUpByLocalCommand command);

  /**
   * Handles the OAuth2 Token Exchange command for Google federated authentication.
   *
   * <p>Validates the submitted Google OIDC id_token, resolves the matching platform account -
   * registering it on first contact - and issues the platform's own bearer token. The matching
   * profile is provisioned through the
   * {@link pe.edu.upc.ice.track.platform.iam.application.internal.outboundservices.acl.ExternalProfileService}
   * outbound service within the same transaction; the call is idempotent, so a repeated exchange
   * retrieves the existing profile instead of duplicating it.</p>
   *
   * @param command the token exchange command carrying the Google id_token
   * @return a Result containing an ImmutablePair of the authenticated User and the platform JWT
   *         if successful, or an ApplicationError if the token or the account is rejected
   */
  Result<ImmutablePair<User, String>, ApplicationError> handle(ExchangeGoogleTokenCommand command);
}
