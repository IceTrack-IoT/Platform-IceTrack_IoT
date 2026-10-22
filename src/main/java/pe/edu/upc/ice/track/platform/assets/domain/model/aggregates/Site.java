package pe.edu.upc.ice.track.platform.assets.domain.model.aggregates;

import lombok.Getter;
import pe.edu.upc.ice.track.platform.assets.domain.model.events.SiteAddressChangedEvent;
import pe.edu.upc.ice.track.platform.assets.domain.model.events.SiteCreatedEvent;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.Address;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.GeoLocation;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.GeocodingStatus;
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
 * knows that a site belongs to some owner, and nothing about how that owner is modeled by the
 * Profiles context. That is what keeps this context upstream - it needs no other context in order
 * to register a site.</p>
 *
 * <p>A site belongs to exactly one owner and that never changes: there is no reassignment method,
 * because moving a site to another owner would silently transfer every equipment installed in it,
 * along with their thresholds and maintenance history.</p>
 *
 * <p><strong>Geocoding (US-20, TS-17).</strong> The address is what the owner types; the
 * coordinates are derived from it, later and elsewhere. A site is therefore always saved first,
 * with {@link GeocodingStatus#PENDING} and no location, and an external lookup settles it
 * afterward through {@link #markLocationResolved}, {@link #markLocationNotFound} or
 * {@link #markLocationFailed}. The invariant those methods keep is that a location exists
 * <em>only</em> while the status is {@code RESOLVED}: whenever the address changes or a lookup
 * does not succeed, the previous coordinates are cleared rather than left pointing at an address
 * the site no longer has.</p>
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

  /**
   * Optimistic concurrency token assigned by persistence, {@code null} until the site is first
   * saved. Opaque to the domain: it only travels so that a write based on a stale copy of the site
   * - typically the asynchronous geocoding write racing an owner's edit - is rejected instead of
   * silently overwriting the other one.
   */
  private final Long version;

  private String name;
  private Address address;
  private String contactName;
  private Phone phone;
  private Integer equipmentCount;
  private GeocodingStatus geocodingStatus;
  private GeoLocation location;

  /**
   * Registers a new site for an owner.
   *
   * <p>The site starts with no equipment and {@link GeocodingStatus#PENDING}: its address has not
   * been located yet, and claiming otherwise would put a pin on a map that nobody computed.</p>
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
    this(siteId, ownerId, null, name, address, contactName, phone, 0, GeocodingStatus.PENDING, null);
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
   * @param geocodingStatus where the site stands in locating its address
   * @param location        the resolved coordinates; ignored unless the status is
   *                        {@code RESOLVED}
   * @throws IllegalArgumentException when a value violates a domain invariant
   */
  public Site(Long siteId, Long ownerId, Long version, String name, Address address, String contactName,
              Phone phone, Integer equipmentCount, GeocodingStatus geocodingStatus, GeoLocation location) {
    this.siteId = siteId;
    this.ownerId = Objects.requireNonNull(ownerId, "ownerId must not be null");
    this.version = version;
    this.name = validatedName(name);
    this.address = Objects.requireNonNull(address, "address must not be null");
    this.contactName = validatedContactName(contactName);
    this.phone = Objects.requireNonNull(phone, "phone must not be null");
    this.equipmentCount = validatedEquipmentCount(equipmentCount);
    this.geocodingStatus = Objects.requireNonNull(geocodingStatus, "geocodingStatus must not be null");
    // Coordinates stored next to any other status belong to an address the site no longer
    // vouches for, so they are dropped on the way in rather than exposed.
    this.location = geocodingStatus == GeocodingStatus.RESOLVED ? location : null;
  }

  /**
   * Replaces the editable details of this site.
   *
   * <p>The owner is deliberately not part of the update: ownership of a site is fixed when it is
   * registered. Geocoding restarts only when the address actually differs, so renaming a site or
   * changing its contact never throws away coordinates that are still correct.</p>
   *
   * @param name        the new site name; required
   * @param address     the new address; required
   * @param contactName the new contact name; required
   * @param phone       the new contact phone number; required
   * @throws IllegalArgumentException when a value violates a domain invariant
   */
  public void updateInfo(String name, Address address, String contactName, Phone phone) {
    var validatedName = validatedName(name);
    var validatedAddress = Objects.requireNonNull(address, "address must not be null");
    var validatedContactName = validatedContactName(contactName);
    var validatedPhone = Objects.requireNonNull(phone, "phone must not be null");
    this.name = validatedName;
    this.contactName = validatedContactName;
    this.phone = validatedPhone;
    if (!validatedAddress.equals(this.address)) {
      changeAddress(validatedAddress);
    }
  }

  /**
   * Gives this site a new address and queues a lookup for it.
   *
   * <p>The previous coordinates are cleared and the status goes back to
   * {@link GeocodingStatus#PENDING}, so a site never shows the location of the address it used to
   * have. Re-submitting the current address restarts the lookup as well, which is how a client
   * retries a {@code FAILED} or {@code NOT_FOUND} site; only a site already {@code RESOLVED} for
   * that same address is left untouched.</p>
   *
   * @param newAddress the new address; required
   */
  public void changeAddress(Address newAddress) {
    var validated = Objects.requireNonNull(newAddress, "address must not be null");
    if (validated.equals(this.address) && geocodingStatus == GeocodingStatus.RESOLVED) {
      return;
    }
    this.address = validated;
    queueGeocoding();
  }

  /**
   * Queues another lookup of the current address.
   *
   * <p>Used to resume a site whose lookup was lost, or to retry one that did not resolve, for
   * example once a missing API key has been configured. A site already {@code RESOLVED} is left
   * untouched, since there is nothing to retry.</p>
   */
  public void retryGeocoding() {
    if (geocodingStatus == GeocodingStatus.RESOLVED) {
      return;
    }
    queueGeocoding();
  }

  /**
   * Tells whether a lookup for the given address may still settle this site.
   *
   * <p>A lookup takes seconds and may be retried, and meanwhile the owner may have changed the
   * address again or another lookup may have finished first. Its result is only applicable while
   * the site is still pending <em>and</em> still holds the address that was looked up.</p>
   *
   * @param lookedUpAddress the address the lookup was run for
   * @return {@code true} when the result of that lookup may be recorded
   */
  public boolean isAwaitingGeocodingOf(String lookedUpAddress) {
    return geocodingStatus == GeocodingStatus.PENDING && address.value().equals(lookedUpAddress);
  }

  /**
   * Records the location the address resolved to.
   *
   * @param location the resolved coordinates; required
   * @throws IllegalStateException when the site is not awaiting a lookup
   */
  public void markLocationResolved(GeoLocation location) {
    ensureAwaitingGeocoding();
    this.location = Objects.requireNonNull(location, "location must not be null");
    this.geocodingStatus = GeocodingStatus.RESOLVED;
  }

  /**
   * Records that the address matched no location, or more than one.
   *
   * @throws IllegalStateException when the site is not awaiting a lookup
   */
  public void markLocationNotFound() {
    ensureAwaitingGeocoding();
    clearLocation();
    this.geocodingStatus = GeocodingStatus.NOT_FOUND;
  }

  /**
   * Records that the lookup could not be completed.
   *
   * @throws IllegalStateException when the site is not awaiting a lookup
   */
  public void markLocationFailed() {
    ensureAwaitingGeocoding();
    clearLocation();
    this.geocodingStatus = GeocodingStatus.FAILED;
  }

  /**
   * Tells whether this site has coordinates that may be shown.
   *
   * @return {@code true} only when the address resolved and a location is present
   */
  public boolean isLocated() {
    return geocodingStatus == GeocodingStatus.RESOLVED && location != null;
  }

  /**
   * Records that one more unit was installed at this site.
   */
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
   * <p>Called by the repository adapter once the persistence identity is available, since both
   * events carry it. The address of a new site has never been located, so its creation also
   * requests the first lookup.</p>
   */
  public void onCreated() {
    registerDomainEvent(SiteCreatedEvent.from(this));
    registerDomainEvent(SiteAddressChangedEvent.from(this));
  }

  private void queueGeocoding() {
    clearLocation();
    this.geocodingStatus = GeocodingStatus.PENDING;
    // A site not yet persisted has no identity to announce; its first lookup is requested by
    // onCreated() once the insert has assigned one.
    if (siteId != null) {
      registerDomainEvent(SiteAddressChangedEvent.from(this));
    }
  }

  private void clearLocation() {
    this.location = null;
  }

  private void ensureAwaitingGeocoding() {
    if (geocodingStatus != GeocodingStatus.PENDING) {
      throw new IllegalStateException(
          "Site %s is not awaiting geocoding; its status is %s".formatted(siteId, geocodingStatus));
    }
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

  private static Integer validatedEquipmentCount(Integer equipmentCount) {
    if (equipmentCount == null || equipmentCount < 0) {
      throw new IllegalArgumentException("Site equipment count must be zero or positive");
    }
    return equipmentCount;
  }
}
