package pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.EmailAddress;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.converters.EmailAddressPersistenceConverter;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.embeddables.PersonNamePersistenceEmbeddable;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.embeddables.PhonePersistenceEmbeddable;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.embeddables.StreetAddressPersistenceEmbeddable;
import pe.edu.upc.ice.track.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;

/**
 * JPA persistence entity for the attributes shared by every profile, mapped to the
 * {@code profiles} table.
 *
 * <p>Root of a {@link InheritanceType#JOINED joined-table} hierarchy: each role keeps its own
 * attributes in its own table ({@code owner_profiles}, {@code technician_profiles}) whose primary
 * key is also a foreign key to {@code profiles.user_profile_id}. No table holds a column that only
 * makes sense for another role, so every role column is {@code NOT NULL}. Only the street number
 * is optional.</p>
 *
 * <p>The inherited identifier is renamed to {@code user_profile_id}, which is the column the
 * subclasses join on.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "profiles")
@Inheritance(strategy = InheritanceType.JOINED)
@AttributeOverride(name = "id", column = @Column(name = "user_profile_id"))
public abstract class ProfilePersistenceEntity extends AuditableAbstractPersistenceEntity {

  /**
   * Identifier of the IAM account this profile belongs to. Stored as a plain column, never as a
   * foreign key association: the {@code profiles} context must not reference an IAM entity. It is
   * unique across every role, so an account can never hold two profiles.
   */
  @Column(name = "user_id", nullable = false, unique = true)
  private Long userId;

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "firstName", column = @Column(name = "first_name", nullable = false)),
      @AttributeOverride(name = "lastName", column = @Column(name = "last_name", nullable = false))})
  private PersonNamePersistenceEmbeddable name;

  @Convert(converter = EmailAddressPersistenceConverter.class)
  @Column(name = "email_address", nullable = false, unique = true)
  private EmailAddress emailAddress;

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "countryCode", column = @Column(name = "country_code", nullable = false)),
      @AttributeOverride(name = "number", column = @Column(name = "phone_number", nullable = false))})
  private PhonePersistenceEmbeddable phone;

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "street", column = @Column(name = "street_address_street", nullable = false)),
      @AttributeOverride(name = "number", column = @Column(name = "street_address_number")),
      @AttributeOverride(name = "city", column = @Column(name = "street_address_city", nullable = false)),
      @AttributeOverride(name = "postalCode", column = @Column(name = "street_address_postal_code", nullable = false)),
      @AttributeOverride(name = "country", column = @Column(name = "street_address_country", nullable = false))})
  private StreetAddressPersistenceEmbeddable streetAddress;

  protected ProfilePersistenceEntity() {
  }
}
