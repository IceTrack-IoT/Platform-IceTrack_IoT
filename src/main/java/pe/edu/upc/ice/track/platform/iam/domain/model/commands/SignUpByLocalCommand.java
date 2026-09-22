package pe.edu.upc.ice.track.platform.iam.domain.model.commands;

import pe.edu.upc.ice.track.platform.iam.domain.model.entities.Role;

import java.util.List;

/**
 * Sign up command by local authentication
 * This class represents a command to sign up a user using local authentication.
 *
 * @param username the username of the user attempting to sign up
 * @param password the password of the user attempting to sign up
 * @param email the email of the user attempting to sign up
 * @param role the list of roles to assign to the user upon sign up
 */
public record SignUpByLocalCommand(String username, String password, String email, List<Role> role) {
}
