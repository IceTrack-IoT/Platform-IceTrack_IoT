package pe.edu.upc.ice.track.platform.profiles.interfaces.acl;

/**
 * ACL facade that exposes Profiles bounded context capabilities to other contexts.
 *
 * <p>This interface <em>is</em> the published contract of the Anti-Corruption Layer of the
 * {@code profiles} context. Every parameter it accepts and every value it returns is a primitive,
 * a boxed primitive or a {@link String}: no {@code Profile} aggregate, value object, factory,
 * command, repository or JPA entity appears in its signature. Callers depend on this interface
 * alone and must never reach into {@code profiles.domain.*}, {@code profiles.application.*} or
 * {@code profiles.infrastructure.*}.</p>
 *
 * <p>Creation is role specific: there is one method per role, each taking exactly the attributes
 * that role needs, so no caller ever passes a value that belongs to another role.</p>
 *
 * <p>Failures are reported with JDK exceptions only, so the calling context never needs a
 * profiles type to understand them:</p>
 * <ul>
 *   <li>{@link IllegalArgumentException} - a supplied value violates a profiles invariant
 *       (malformed email, 10 digit RUC, blank speciality, incomplete address...);</li>
 *   <li>{@link IllegalStateException} - the account or the email already has a profile;</li>
 *   <li>any other {@link RuntimeException} - an unexpected failure.</li>
 * </ul>
 * <p>Each create method joins the caller's transaction: a registration and its profile commit or
 * roll back together.</p>
 */
public interface ProfilesContextFacade {

  /**
   * Creates the owner profile of a platform account.
   *
   * @param userId     identifier of the account the owner belongs to; required
   * @param fullName   display name of the account holder; required. It is split into a given and
   *                   a family name by the profiles context
   * @param email      email address of the account holder; required and well formed
   * @param phone      phone number of the account holder; required. A leading {@code +NN }
   *                   prefix separated by a space is read as the country code
   * @param street     street of the account holder's address; required
   * @param number     street number or apartment; may be {@code null}
   * @param city       city of the address; required
   * @param postalCode postal code of the address; required
   * @param country    country of the address; required
   * @param ruc        the owner's 11 digit taxpayer registration number; required
   * @return the identifier of the created profile, never {@code null}
   * @throws IllegalArgumentException when a value violates a profiles invariant
   * @throws IllegalStateException    when the account or the email already has a profile
   */
  Long createOwnerProfile(Long userId, String fullName, String email, String phone,
                          String street, String number, String city, String postalCode, String country,
                          Long ruc);

  /**
   * Creates the technician profile of a platform account.
   *
   * @param userId              identifier of the account the technician belongs to; required
   * @param fullName            display name of the account holder; required
   * @param email               email address of the account holder; required and well formed
   * @param phone               phone number of the account holder; required
   * @param street              street of the account holder's address; required
   * @param number              street number or apartment; may be {@code null}
   * @param city                city of the address; required
   * @param postalCode          postal code of the address; required
   * @param country             country of the address; required
   * @param speciality          the technician's speciality; required
   * @param certificationNumber the number of the technician's certification; required
   * @return the identifier of the created profile, never {@code null}
   * @throws IllegalArgumentException when a value violates a profiles invariant
   * @throws IllegalStateException    when the account or the email already has a profile
   */
  Long createTechnicianProfile(Long userId, String fullName, String email, String phone,
                               String street, String number, String city, String postalCode, String country,
                               String speciality, String certificationNumber);

  /**
   * Checks whether a profile of any role is bound to a platform account.
   *
   * @param userId identifier of the account
   * @return {@code true} when the account has a profile
   */
  boolean existsProfileByUserId(Long userId);

  /**
   * Resolves the owner profile a platform account acts as.
   *
   * <p>This is the read only half of the ACL, the counterpart of the create methods: a calling
   * context that owns resources - sites, equipment, subscriptions - needs to know which owner
   * profile an authenticated account stands for, and it may only learn that through this
   * facade.</p>
   *
   * @param userId identifier of the account; may be {@code null}
   * @return the identifier of the owner profile bound to the account, or {@code 0L} when the
   *         account holds no owner profile - a technician account, say
   */
  Long fetchOwnerIdByUserId(Long userId);
}
