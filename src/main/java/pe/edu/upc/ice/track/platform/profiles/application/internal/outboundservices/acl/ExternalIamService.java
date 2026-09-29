package pe.edu.upc.ice.track.platform.profiles.application.internal.outboundservices.acl;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.iam.interfaces.acl.IamContextFacade;

import java.util.Optional;

/**
 * Outbound service through which the {@code profiles} bounded context queries the IAM context.
 *
 * <p>This is the <em>outbound half</em> of the Anti-Corruption Layer between {@code profiles} and
 * {@code iam}, and the only class in {@code profiles} that knows an IAM context exists. It talks
 * to it exclusively through {@link IamContextFacade}, whose signature is primitives and
 * {@link String}s: no IAM aggregate, entity, repository or security type is reachable from
 * here.</p>
 */
@Service
public class ExternalIamService {

  /**
   * Sentinel the IAM facade returns when no account matches.
   */
  private static final long NO_USER = 0L;

  private final IamContextFacade iamContextFacade;

  /**
   * Creates the outbound service.
   *
   * @param iamContextFacade the inbound ACL facade of the IAM bounded context
   */
  public ExternalIamService(IamContextFacade iamContextFacade) {
    this.iamContextFacade = iamContextFacade;
  }

  /**
   * Resolves the identifier of the platform account holding a username.
   *
   * @param username the username of the account
   * @return the account identifier, or empty when no account holds the username
   */
  public Optional<Long> fetchUserIdByUsername(String username) {
    if (username == null || username.isBlank()) {
      return Optional.empty();
    }
    var userId = iamContextFacade.fetchUserIdByUsername(username);
    return userId == null || userId == NO_USER ? Optional.empty() : Optional.of(userId);
  }
}
