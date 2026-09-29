package pe.edu.upc.ice.track.platform.iam.domain.model.commands;

/**
 * Purge expired refresh tokens command
 * This class represents a command to delete every refresh token past its expiry date. It is issued
 * periodically so that rotated and abandoned sessions do not accumulate.
 */
public record PurgeExpiredRefreshTokensCommand() {
}
