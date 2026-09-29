package pe.edu.upc.ice.track.platform.iam.application.internal.outboundservices.acl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.profiles.interfaces.acl.ProfilesContextFacade;

/**
 * Outbound service through which the IAM bounded context provisions the profile of an account it
 * is registering.
 *
 * <p>This is the <em>outbound half</em> of the Anti-Corruption Layer between {@code iam} and
 * {@code profiles}, and the only class in {@code iam} that knows a {@code profiles} context
 * exists. It talks to it exclusively through {@link ProfilesContextFacade}, the inbound facade,
 * whose whole signature is primitives and {@link String}s: no profiles aggregate, value object,
 * factory, repository, command or JPA entity is reachable from here.</p>
 *
 * <p>The call is an ordinary in-process invocation, so it participates in the caller's
 * transaction: a registration and its profile either both commit or both roll back.</p>
 *
 * <p>Failures are the facade's JDK exceptions, propagated unchanged:</p>
 * <ul>
 *   <li>{@link IllegalArgumentException} - a supplied value is rejected by the profiles context;</li>
 *   <li>{@link IllegalStateException} - the account or the email already has a profile;</li>
 *   <li>any other {@link RuntimeException} - an unexpected failure.</li>
 * </ul>
 */
@Service
@Slf4j
public class ExternalProfileService {

  private final ProfilesContextFacade profilesContextFacade;

  /**
   * Creates the outbound service.
   *
   * @param profilesContextFacade the inbound ACL facade of the profiles bounded context
   */
  public ExternalProfileService(ProfilesContextFacade profilesContextFacade) {
    this.profilesContextFacade = profilesContextFacade;
  }

  /**
   * Creates the owner profile of a freshly persisted account.
   *
   * @param userId     identifier of the account; required
   * @param fullName   display name of the account holder; required
   * @param email      email address of the account holder; required
   * @param phone      phone number of the account holder; required
   * @param street     street of the address; required
   * @param number     street number or apartment; may be {@code null}
   * @param city       city of the address; required
   * @param postalCode postal code of the address; required
   * @param country    country of the address; required
   * @param ruc        the owner's taxpayer registration number; required
   * @return the identifier of the created profile, never {@code null}
   */
  public Long createOwnerProfile(Long userId, String fullName, String email, String phone,
                                 String street, String number, String city, String postalCode, String country,
                                 Long ruc) {
    var ownerProfileId = profilesContextFacade.createOwnerProfile(
        userId, fullName, email, phone, street, number, city, postalCode, country, ruc);
    log.info("Provisioned owner profile {} for user {}", ownerProfileId, userId);
    return ownerProfileId;
  }

  /**
   * Creates the technician profile of a freshly persisted account.
   *
   * @param userId              identifier of the account; required
   * @param fullName            display name of the account holder; required
   * @param email               email address of the account holder; required
   * @param phone               phone number of the account holder; required
   * @param street              street of the address; required
   * @param number              street number or apartment; may be {@code null}
   * @param city                city of the address; required
   * @param postalCode          postal code of the address; required
   * @param country             country of the address; required
   * @param speciality          the technician's speciality; required
   * @param certificationNumber the number of the technician's certification; required
   * @return the identifier of the created profile, never {@code null}
   */
  public Long createTechnicianProfile(Long userId, String fullName, String email, String phone,
                                      String street, String number, String city, String postalCode, String country,
                                      String speciality, String certificationNumber) {
    var technicianProfileId = profilesContextFacade.createTechnicianProfile(
        userId, fullName, email, phone, street, number, city, postalCode, country, speciality, certificationNumber);
    log.info("Provisioned technician profile {} for user {}", technicianProfileId, userId);
    return technicianProfileId;
  }
}
