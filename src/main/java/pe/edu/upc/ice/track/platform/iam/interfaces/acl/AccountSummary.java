package pe.edu.upc.ice.track.platform.iam.interfaces.acl;

/**
 * Published view of a platform account, returned by {@link IamContextFacade#fetchAccountByUsername}.
 *
 * <p>Part of the published language of the {@code iam} context: plain values only. The role and the
 * identity provider travel as the names of their constants.</p>
 *
 * @param userId   identifier of the account
 * @param username the username it signs in with
 * @param role     its role, such as {@code OWNER_ROLE} or {@code TECHNICIAN_ROLE}
 * @param provider its identity provider, {@code LOCAL} or {@code GOOGLE}
 */
public record AccountSummary(Long userId, String username, String role, String provider) {
}
