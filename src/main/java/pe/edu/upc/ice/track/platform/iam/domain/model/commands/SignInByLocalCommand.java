package pe.edu.upc.ice.track.platform.iam.domain.model.commands;

/**
 * Sign in command by local authentication
 * This class represents a command to sign in a user using local authentication.
 *
 * @param username the username of the user attempting to sign in
 * @param password the password of the user attempting to sign in
 */
public record SignInByLocalCommand(
    String username,
    String password) {
}
