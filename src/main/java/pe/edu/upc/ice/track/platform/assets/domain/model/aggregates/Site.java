package pe.edu.upc.ice.track.platform.assets.domain.model.aggregates;

import lombok.Getter;
import pe.edu.upc.ice.track.platform.assets.domain.model.events.SiteCreatedEvent;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.Address;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.GeoLocation;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.GeocodingStatus;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.SiteContactPhone;
import pe.edu.upc.ice.track.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;

import java.util.Objects;

/**
 * Site aggregate root of the {@code assets} bounded context.
 *
 * <p>A physical premises where an ice track owner has installed one or more refrigeration units.
 * Ownership is held by identifier only ({@code ownerId}), never by composition.</p>
 *
 * <p><strong>Verified location.</strong> A site is only created or re-addressed with an address
 * that has already been verified against the geocoding provider (see {@code VerifiedAddress}). Such
 * a site is {@link GeocodingStatus#RESOLVED} and always carries its location; the provider is not
 * consulted again when it is saved. Sites created before verification existed may hold another
 * status and no location; they are kept as they are and reported without coordinates.</p>
 *
 * <p>No JPA or persistence annotation is present here - the mapping lives exclusively in
 * {@code SitePersistenceEntity}.</p>
 */
@Getter
public class Site extends AbstractDomainAggregateRoot<Site> {

  /** Maximum number of characters the site name may hold, matching the {@code name} column width. */
  public static final int MAX_NAME_LENGTH = 30;

  /** Maximum number of characters the contact name may hold, matching the column width. */
  public static final int MAX_CONTACT_NAME_LENGTH = 30;

  private final Long siteId;
  private final Long ownerId;

  /**
   * Optimistic concurrency token assigned by persistence, {@code null} until the site is first saved.
   */
  private final Long version;

  private String name;
  private Address address;
  private String contactName;
  private SiteContactPhone phone;
  private Integer equipmentCount;
  private GeocodingStatus geocodingStatus;
  private GeoLocation location;

  /**
   * Registers a new site at a verified address.
   *
   * @param ownerId     identifier of the owner the site belongs to; required
   * @param name        the site name; required, at most 30 characters
   * @param address     the normalized address; required
   * @param contactName the person reachable on site; required, at most 30 characters
   * @param phone       the contact phone number; required
   * @param location    the verified (or pin-adjusted) coordinates; required
   * @return the new, unpersisted site, already {@code RESOLVED}
   * @throws IllegalArgumentException when a value violates a domain invariant
   */
  public static Site registerVerified(Long ownerId, String name, Address address, String contactName,
                                      SiteContactPhone phone, GeoLocation location) {
    return new Site(null, ownerId, null, name, address, contactName, phone, 0,
        GeocodingStatus.RESOLVED, Objects.requireNonNull(location, "location must not be null"));
  }

  /**
   * Reconstitutes a site from persistence.
   *
   * @param siteId          the persistence identity
   * @param ownerId         identifier of the owner the site belongs to
   * @param version         the optimistic concurrency token
   * @param name            the site name
   * @param address         the physical address of the site
   * @param contactName     the name of the person reachable on site
   * @param phone           the contact phone number
   * @param equipmentCount  the number of units installed at the site
   * @param geocodingStatus the stored geocoding status; {@code RESOLVED} for verified sites
   * @param location        the coordinates; ignored unless the status is {@code RESOLVED}
   * @throws IllegalArgumentException when a value violates a domain invariant
   */
  public Site(Long siteId, Long ownerId, Long version, String name, Address address, String contactName,
              SiteContactPhone phone, Integer equipmentCount, GeocodingStatus geocodingStatus, GeoLocation location) {
    this.siteId = siteId;
    this.ownerId = Objects.requireNonNull(ownerId, "ownerId must not be null");
    this.version = version;
    this.name = validatedName(name);
    this.address = Objects.requireNonNull(address, "address must not be null");
    this.contactName = validatedContactName(contactName);
    this.phone = Objects.requireNonNull(phone, "phone must not be null");
    this.equipmentCount = validatedEquipmentCount(equipmentCount);
    this.geocodingStatus = Objects.requireNonNull(geocodingStatus, "geocodingStatus must not be null");
    // Coordinates stored next to any other status belong to an address the site no longer vouches for.
    this.location = geocodingStatus == GeocodingStatus.RESOLVED ? location : null;
  }

  /**
   * Replaces the editable details of this site, except its address.
   *
   * <p>The address changes only through {@link #changeVerifiedAddress}, so an unverified address can
   * never be saved. The owner never changes either.</p>
   *
   * @param name        the new site name; required
   * @param contactName the new contact name; required
   * @param phone       the new contact phone number; required
   * @throws IllegalArgumentException when a value violates a domain invariant
   */
  public void updateInfo(String name, String contactName, SiteContactPhone phone) {
    var validatedName = validatedName(name);
    var validatedContactName = validatedContactName(contactName);
    var validatedPhone = Objects.requireNonNull(phone, "phone must not be null");
    this.name = validatedName;
    this.contactName = validatedContactName;
    this.phone = validatedPhone;
  }

  /**
   * Moves the site to a verified address and its coordinates.
   *
   * @param verifiedAddress the normalized address; required
   * @param location        the verified or pin-adjusted coordinates; required
   */
  public void changeVerifiedAddress(Address verifiedAddress, GeoLocation location) {
    this.address = Objects.requireNonNull(verifiedAddress, "address must not be null");
    this.location = Objects.requireNonNull(location, "location must not be null");
    this.geocodingStatus = GeocodingStatus.RESOLVED;
  }

  /**
   * Tells whether this site has coordinates that may be shown.
   *
   * @return {@code true} only when the address is resolved and a location is present
   */
  public boolean isLocated() {
    return geocodingStatus == GeocodingStatus.RESOLVED && location != null;
  }

  /** Records that one more unit was installed at this site. */
  public void incrementEquipmentCount() {
    this.equipmentCount = equipmentCount + 1;
  }

  /**
   * Records that one unit was removed from this site.
   *
   * @throws IllegalStateException when the site has no unit left to remove
   */
  public void decrementEquipmentCount() {
    if (equipmentCount == 0) {
      throw new IllegalStateException("Site %s has no equipment left to remove".formatted(siteId));
    }
    this.equipmentCount = equipmentCount - 1;
  }

  /**
   * Tells whether this site belongs to the given owner.
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
   * <p>Called by the repository adapter once the persistence identity is available.</p>
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
      throw new IllegalArgumentException("Site name must not exceed %d characters".formatted(MAX_NAME_LENGTH));
    }
    return trimmed;
  }

  private static String validatedContactName(String contactName) {
    if (contactName == null || contactName.isBlank()) {
      throw new IllegalArgumentException("Site contact name must not be null or blank");
    }
    var trimmed = contactName.trim();
    if (trimmed.length() > MAX_CONTACT_NAME_LENGTH) {
      throw new IllegalArgumentException("Site contact name must not exceed %d characters".formatted(MAX_CONTACT_NAME_LENGTH));
    }
    return trimmed;
  }

  private static Integer validatedEquipmentCount(Integer equipmentCount) {
    if (equipmentCount == null || equipmentCount < 0) {
      throw new IllegalArgumentException("Site equipment count must be zero or positive");
    }
    return equipmentCount;
  }
}
