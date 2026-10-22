package pe.edu.upc.ice.track.platform.assets.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.Address;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.GeocodingStatus;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.Phone;
import pe.edu.upc.ice.track.platform.assets.infrastructure.persistence.jpa.converters.AddressPersistenceConverter;
import pe.edu.upc.ice.track.platform.assets.infrastructure.persistence.jpa.converters.PhonePersistenceConverter;
import pe.edu.upc.ice.track.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;

/**
 * JPA persistence entity for a {@code Site}, mapped to the {@code sites} table.
 *
 * <p>Kept separate from the {@code Site} aggregate on purpose: the aggregate stays free of JPA so
 * it can be reasoned about, created and tested without a persistence context, while this class
 * absorbs the mapping, the column widths and the indexes.</p>
 *
 * <p>{@code ownerProfilesId} is a plain column and never a foreign key. The owner lives in the
 * {@code profiles} bounded context, so this table points at an identifier and
 * nothing else - which is what lets this context be upstream and run without Profiles.</p>
 *
 * <p>The index on {@code owner_profiles_id} is not decorative: every listing an owner opens is
 * scoped by it, so without it each page of the dashboard would be a sequential scan.</p>
 *
 * <p>The location is flattened into three nullable columns rather than embedded, so the
 * {@code GeoLocation} value object stays free of JPA too. The columns added for geocoding, the
 * equipment counter and the version all carry a database default, because the schema is evolved
 * by {@code ddl-auto=update} and a {@code NOT NULL} column without a default cannot be added to a
 * table that already holds rows. Existing sites therefore start {@code PENDING}, which the startup
 * recovery in {@code SiteGeocodingRecoveryEventHandler} picks up.</p>
 */
@Getter
@Setter
@Entity
@Table(
    name = "sites",
    indexes = @Index(
        name = "idx_sites_owner_profiles_id",
        columnList = "owner_profiles_id"))
public class SitePersistenceEntity extends AuditableAbstractPersistenceEntity {

  /**
   * Identifier of the owner profile this site belongs to. Stored as a plain column, never as a
   * foreign key association: the assets context must not reference a Profiles entity.
   */
  @Column(name = "owner_profiles_id", nullable = false)
  private Long ownerProfilesId;

  @Column(name = "name", nullable = false, length = 30)
  private String name;

  @Convert(converter = AddressPersistenceConverter.class)
  @Column(name = "address", nullable = false, length = 50)
  private Address address;

  @Column(name = "contact_name", nullable = false, length = 30)
  private String contactName;

  @Convert(converter = PhonePersistenceConverter.class)
  @Column(name = "phone", nullable = false, length = 20)
  private Phone phone;

  @ColumnDefault("0")
  @Column(name = "equipment_count", nullable = false)
  private Integer equipmentCount;

  /** Stored as {@code STRING}, never as an ordinal, for the same reason as the equipment enums. */
  @Enumerated(EnumType.STRING)
  @ColumnDefault("'PENDING'")
  @Column(name = "geocoding_status", nullable = false, length = 20)
  private GeocodingStatus geocodingStatus;

  /** Latitude of the resolved location; {@code null} unless the status is {@code RESOLVED}. */
  @Column(name = "latitude")
  private Double latitude;

  /** Longitude of the resolved location; {@code null} unless the status is {@code RESOLVED}. */
  @Column(name = "longitude")
  private Double longitude;

  /** The geocoding provider's place identifier, when it supplied one. */
  @Column(name = "place_id")
  private String placeId;

  /**
   * Optimistic lock. A site has two independent writers - the owner's edits and the asynchronous
   * geocoding write-back - and without it the slower one would silently overwrite the other.
   */
  @Version
  @ColumnDefault("0")
  @Column(name = "version", nullable = false)
  private Long version;

  public SitePersistenceEntity() {
  }
}
