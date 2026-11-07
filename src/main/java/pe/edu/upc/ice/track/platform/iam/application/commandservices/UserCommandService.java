package pe.edu.upc.ice.track.platform.iam.application.commandservices;

import org.apache.commons.lang3.tuple.ImmutablePair;
import pe.edu.upc.ice.track.platform.iam.domain.model.aggregates.User;
import pe.edu.upc.ice.track.platform.iam.domain.model.commands.CompleteGoogleOwnerRegistrationCommand;
import pe.edu.upc.ice.track.platform.iam.domain.model.commands.CompleteGoogleTechnicianRegistrationCommand;
import pe.edu.upc.ice.track.platform.iam.domain.model.commands.LinkGoogleAccountCommand;
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
   * <p>An unknown account, an account without a local password and a wrong password all fail with
   * the same {@code BAD_CREDENTIALS}, after the same amount of hashing work. Valid credentials on a
   * channel that does not admit the account's role fail with {@code ROLE_CHANNEL_MISMATCH}.</p>
   *
   * @param command the sign-in command containing the user's credentials and client channel
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
   * <p>Validates the Google OIDC id_token and, when a platform account is bound to it, opens a
   * session. A rejected token fails with {@code INVALID_TOKEN}. When no account matches, nothing
   * is written and the result is a {@code GOOGLE_ACCOUNT_NOT_FOUND} failure, telling the caller to
   * complete the onboarding. When the verified email belongs to an account registered with a
   * password, the result is {@code ACCOUNT_LINK_REQUIRED}: the account is never linked here.</p>
   *
   * @param command the command carrying the Google id_token and the client channel
   * @return a Result containing an ImmutablePair of the authenticated User and its session tokens,
   *         or an ApplicationError when the token is rejected or the account is not registered
   */
  Result<ImmutablePair<User, SessionTokens>, ApplicationError> handle(SignInByGoogleCommand command);

  /**
   * Links a Google identity to the account registered with a password whose profile uses the
   * verified Google email, then opens a session.
   *
   * <p>The link is authorized by the account's current password; a missing account or a wrong
   * password fails with {@code BAD_CREDENTIALS}. A Google identity already bound to an account
   * simply signs that account in.</p>
   *
   * @param command the command carrying the Google id_token, the password and the client channel
   * @return a Result containing an ImmutablePair of the authenticated User and its session tokens,
   *         or an ApplicationError when the token, the password or the channel is rejected
   */
  Result<ImmutablePair<User, SessionTokens>, ApplicationError> handle(LinkGoogleAccountCommand command);

  /**
   * Completes the deferred registration of a Google account as an ice track owner.
   *
   * <p>Validates the Google OIDC id_token again, then creates the account with
   * {@code OWNER_ROLE} and its {@code Owner} profile in the same transaction. When the Google
   * account is already registered, it is signed in only if it holds {@code OWNER_ROLE}; otherwise
   * the result is {@code ROLE_CHANNEL_MISMATCH}.</p>
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
   * Google account is already registered, it is signed in only if it holds
   * {@code TECHNICIAN_ROLE}; otherwise the result is {@code ROLE_CHANNEL_MISMATCH}.</p>
   *
   * @param command the command carrying the Google id_token and the technician onboarding form
   * @return a Result containing an ImmutablePair of the authenticated User and its session tokens,
   *         or an ApplicationError when the token, the form or the profile is rejected
   */
  Result<ImmutablePair<User, SessionTokens>, ApplicationError> handle(CompleteGoogleTechnicianRegistrationCommand command);

  /**
   * Exchanges a refresh token for a new session, rotating the refresh token.
   *
   * <p>The presented token is rotated: the result carries a new access token and a new refresh
   * token. A rotated token presented again is a replay: every session of its account is revoked,
   * unless it was rotated within the reuse grace window, which marks a concurrent refresh by the
   * legitimate client.</p>
   *
   * <p>The new session keeps the channel the presented one was opened from. When the command
   * states another channel, or the account's role is no longer admitted by it, the result is
   * {@code ROLE_CHANNEL_MISMATCH} and the presented token is left untouched.</p>
   *
   * @param command the command carrying the refresh token and, optionally, the client channel
   * @return a Result containing an ImmutablePair of the authenticated User and its new session
   *         tokens, or an ApplicationError when the command is missing or the channel is rejected
   * @throws pe.edu.upc.ice.track.platform.iam.domain.exceptions.RefreshTokenException when the
   *         refresh token is not accepted, carrying the
   *         {@link pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.AuthErrorCode} explaining why
   */
  Result<ImmutablePair<User, SessionTokens>, ApplicationError> handle(RefreshTokenCommand command);

  /**
   * Ends the session bound to a refresh token.
   *
   * <p>The token is revoked without a replacement, so presenting it later is rejected as
   * {@code REFRESH_TOKEN_REVOKED}. Idempotent: an unknown or already revoked token is silently
   * ignored, so the outcome never reveals whether a token exists.</p>
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
