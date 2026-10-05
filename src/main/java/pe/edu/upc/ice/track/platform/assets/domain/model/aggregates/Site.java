package pe.edu.upc.ice.track.platform.assets.domain.model.aggregates;

import lombok.Getter;
import pe.edu.upc.ice.track.platform.assets.domain.model.events.SiteCreatedEvent;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.Address;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.Phone;
import pe.edu.upc.ice.track.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;

import java.util.Objects;

/**
 * Site aggregate root of the {@code assets} bounded context.
 *
 * <p>A physical premises where an ice track owner has installed one or more refrigeration units.
 * A site is the physical base of the IoT domain: it groups the equipment that share a location and
 * a single point of contact, and it is the unit an owner registers before any telemetry can exist.</p>
 *
 * <p>Ownership is held by identifier only ({@code ownerId}), never by composition: this context
 * knows that a site belongs to some owner, and nothing about how that owner is modelled by the
 * Profiles context. That is what keeps this context upstream - it needs no other context in order
 * to register a site.</p>
 *
 * <p>A site belongs to exactly one owner and that never changes: there is no reassignment method,
 * because moving a site to another owner would silently transfer every equipment installed in it,
 * along with their thresholds and maintenance history.</p>
 *
 * <p>No JPA or persistence annotation is present here - the mapping lives exclusively in
 * {@code SitePersistenceEntity}.</p>
 */
@Getter
public class Site extends AbstractDomainAggregateRoot<Site> {

  /**
   * Maximum number of characters the site name may hold, matching the {@code name} column width.
   */
  public static final int MAX_NAME_LENGTH = 30;

  /**
   * Maximum number of characters the contact name may hold, matching the column width.
   */
  public static final int MAX_CONTACT_NAME_LENGTH = 30;

  private final Long siteId;
  private final Long ownerId;
  private String name;
  private Address address;
  private String contactName;
  private Phone phone;

  /**
   * Registers a new site for an owner.
   *
   * @param siteId      the persistence identity, or {@code null} for a site not yet persisted
   * @param ownerId     identifier of the owner the site belongs to; required
   * @param name        the site name; required, not blank, at most 30 characters
   * @param address     the physical address of the site; required
   * @param contactName the name of the person reachable on site; required
   * @param phone       the contact phone number; required
   * @throws IllegalArgumentException when a value violates a domain invariant
   */
  public Site(Long siteId, Long ownerId, String name, Address address, String contactName, Phone phone) {
    this.siteId = siteId;
    this.ownerId = Objects.requireNonNull(ownerId, "ownerId must not be null");
    this.name = validatedName(name);
    this.address = Objects.requireNonNull(address, "address must not be null");
    this.contactName = validatedContactName(contactName);
    this.phone = Objects.requireNonNull(phone, "phone must not be null");
  }

  /**
   * Replaces the editable details of this site.
   *
   * <p>The owner is deliberately not part of the update: ownership of a site is fixed when it is
   * registered.</p>
   *
   * @param name        the new site name; required
   * @param address     the new address; required
   * @param contactName the new contact name; required
   * @param phone       the new contact phone number; required
   * @throws IllegalArgumentException when a value violates a domain invariant
   */
  public void updateInfo(String name, Address address, String contactName, Phone phone) {
    this.name = validatedName(name);
    this.address = Objects.requireNonNull(address, "address must not be null");
    this.contactName = validatedContactName(contactName);
    this.phone = Objects.requireNonNull(phone, "phone must not be null");
  }

  /**
   * Tells whether this site belongs to the given owner.
   *
   * <p>This is the single rule behind every ownership check in this context: a site is visible and
   * editable only by the owner it was registered for.</p>
   *
   * @param candidateOwnerId the owner identifier to test
   * @return {@code true} when the identifiers match
   */
  public boolean belongsTo(Long candidateOwnerId) {
    return ownerId.equals(candidateOwnerId);
  }

  /**
   * Signals that this site has just been created and persisted.
   *
   * <p>Called by the repository adapter once the persistence identity is available, since the
   * creation event carries it.</p>
   */
  public void onCreated() {
    registerDomainEvent(SiteCreatedEvent.from(this));
  }

  private static String validatedName(String name) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Site name must not be null or blank");
    }
    var trimmed = name.trim();
    if (trimmed.length() > MAX_NAME_LENGTH) {
      throw new IllegalArgumentException(
          "Site name must not exceed %d characters".formatted(MAX_NAME_LENGTH));
    }
    return trimmed;
  }

  private static String validatedContactName(String contactName) {
    if (contactName == null || contactName.isBlank()) {
      throw new IllegalArgumentException("Site contact name must not be null or blank");
    }
    var trimmed = contactName.trim();
    if (trimmed.length() > MAX_CONTACT_NAME_LENGTH) {
      throw new IllegalArgumentException(
          "Site contact name must not exceed %d characters".formatted(MAX_CONTACT_NAME_LENGTH));
    }
    return trimmed;
  }
}