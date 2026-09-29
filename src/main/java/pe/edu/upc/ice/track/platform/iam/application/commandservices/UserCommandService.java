package pe.edu.upc.ice.track.platform.iam.application.commandservices;

import org.apache.commons.lang3.tuple.ImmutablePair;
import pe.edu.upc.ice.track.platform.iam.domain.model.aggregates.User;
import pe.edu.upc.ice.track.platform.iam.domain.model.commands.CompleteGoogleOwnerRegistrationCommand;
import pe.edu.upc.ice.track.platform.iam.domain.model.commands.CompleteGoogleTechnicianRegistrationCommand;
import pe.edu.upc.ice.track.platform.iam.domain.model.commands.PurgeExpiredRefreshTokensCommand;
import pe.edu.upc.ice.track.platform.iam.domain.model.commands.RefreshTokenCommand;
import pe.edu.upc.ice.track.platform.iam.domain.model.commands.SignInByGoogleCommand;
import pe.edu.upc.ice.track.platform.iam.domain.model.commands.SignInByLocalCommand;
import pe.edu.upc.ice.track.platform.iam.domain.model.commands.SignOutCommand;
import pe.edu.upc.ice.track.platform.iam.domain.model.commands.SignUpOwnerCommand;
import pe.edu.upc.ice.track.platform.iam.domain.model.commands.SignUpTechnicianCommand;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.SessionTokens;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

/**
 * Application service contract for IAM user commands.
 *
 * <p>Every registration command is role explicit: the role is implied by the command type, never
 * supplied as data. Each one creates the account and, in the same transaction, its concrete
 * profile through the
 * {@link pe.edu.upc.ice.track.platform.iam.application.internal.outboundservices.acl.ExternalProfileService}
 * outbound service; when the profile is rejected, the account is rolled back.</p>
 *
 * <p>Every command that signs an account in opens a session: it returns {@link SessionTokens}
 * made of a short-lived access token and a single-use refresh token.</p>
 */
public interface UserCommandService {

  /**
   * Handles the sign-in command by local authentication.
   *
   * @param command the sign-in command containing the user's credentials
   * @return a Result containing an ImmutablePair of the authenticated User and its session tokens if successful, or an ApplicationError if failed
   */
  Result<ImmutablePair<User, SessionTokens>, ApplicationError> handle(SignInByLocalCommand command);

  /**
   * Registers an ice track owner with local credentials and its {@code Owner} profile.
   *
   * @param command the owner sign-up command
   * @return a Result containing the newly created User if successful, or an ApplicationError if failed
   */
  Result<User, ApplicationError> handle(SignUpOwnerCommand command);

  /**
   * Registers a maintenance technician with local credentials and its {@code Technician} profile.
   *
   * @param command the technician sign-up command
   * @return a Result containing the newly created User if successful, or an ApplicationError if failed
   */
  Result<User, ApplicationError> handle(SignUpTechnicianCommand command);

  /**
   * Handles the Google sign-in command - the first step of the deferred registration flow.
   *
   * <p>Validates the Google OIDC id_token and, when a platform account matches it, opens a
   * session. When no account matches, nothing is written and the result is a
   * {@code GOOGLE_ACCOUNT_NOT_FOUND} failure, telling the caller to complete the onboarding.</p>
   *
   * @param command the command carrying the Google id_token
   * @return a Result containing an ImmutablePair of the authenticated User and its session tokens,
   *         or an ApplicationError when the token is rejected or the account is not registered
   */
  Result<ImmutablePair<User, SessionTokens>, ApplicationError> handle(SignInByGoogleCommand command);

  /**
   * Completes the deferred registration of a Google account as an ice track owner.
   *
   * <p>Validates the Google OIDC id_token again, then creates the account with
   * {@code OWNER_ROLE} and its {@code Owner} profile in the same transaction. When the Google
   * account is already registered, it is simply signed in with its existing role.</p>
   *
   * @param command the command carrying the Google id_token and the owner onboarding form
   * @return a Result containing an ImmutablePair of the authenticated User and its session tokens,
   *         or an ApplicationError when the token, the form or the profile is rejected
   */
  Result<ImmutablePair<User, SessionTokens>, ApplicationError> handle(CompleteGoogleOwnerRegistrationCommand command);

  /**
   * Completes the deferred registration of a Google account as a maintenance technician.
   *
   * <p>Validates the Google OIDC id_token again, then creates the account with
   * {@code TECHNICIAN_ROLE} and its {@code Technician} profile in the same transaction. When the
   * Google account is already registered, it is simply signed in with its existing role.</p>
   *
   * @param command the command carrying the Google id_token and the technician onboarding form
   * @return a Result containing an ImmutablePair of the authenticated User and its session tokens,
   *         or an ApplicationError when the token, the form or the profile is rejected
   */
  Result<ImmutablePair<User, SessionTokens>, ApplicationError> handle(CompleteGoogleTechnicianRegistrationCommand command);

  /**
   * Exchanges a refresh token for a new session, rotating the refresh token.
   *
   * <p>The presented token is revoked and replaced: the result carries a new access token and a
   * new refresh token. A token that is unknown, expired or already revoked is rejected with an
   * {@code UNAUTHORIZED} failure. A revoked token being presented again is a replay: every
   * session of its account is revoked as well, unless it was rotated within the reuse grace
   * window, which marks a concurrent refresh by the legitimate client.</p>
   *
   * @param command the command carrying the refresh token
   * @return a Result containing an ImmutablePair of the authenticated User and its new session
   *         tokens, or an ApplicationError when the refresh token is not accepted
   */
  Result<ImmutablePair<User, SessionTokens>, ApplicationError> handle(RefreshTokenCommand command);

  /**
   * Ends the session bound to a refresh token.
   *
   * <p>Idempotent: an unknown or already discarded token is silently ignored, so the outcome
   * never reveals whether a token exists.</p>
   *
   * @param command the command carrying the refresh token of the session to end
   */
  void handle(SignOutCommand command);

  /**
   * Deletes every refresh token past its expiry date.
   *
   * <p>Revoked tokens that have not expired yet are kept, so that a replay can still be detected.</p>
   *
   * @param command the purge command
   * @return the number of deleted tokens
   */
  int handle(PurgeExpiredRefreshTokensCommand command);
}
