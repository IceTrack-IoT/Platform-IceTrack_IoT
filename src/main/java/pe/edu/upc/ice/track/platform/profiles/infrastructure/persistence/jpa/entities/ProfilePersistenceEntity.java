package pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.EmailAddress;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.ProfileRole;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.converters.EmailAddressPersistenceConverter;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.embeddables.PersonNamePersistenceEmbeddable;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.embeddables.PhonePersistenceEmbeddable;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.embeddables.StreetAddressPersistenceEmbeddable;
import pe.edu.upc.ice.track.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;

/**
 * JPA persistence entity for profiles.
 */
@Getter
@Setter
@Entity
@Table(name = "profiles")
public class ProfilePersistenceEntity extends AuditableAbstractPersistenceEntity {

  /**
   * Identifier of the IAM account this profile belongs to. Stored as a plain column, never as a
   * foreign key association: the {@code profiles} context must not reference an IAM entity.
   */
  @Column(name = "user_id", unique = true)
  private Long userId;

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "firstName", column = @Column(name = "first_name")),
      @AttributeOverride(name = "lastName", column = @Column(name = "last_name"))})
  private PersonNamePersistenceEmbeddable name;

  @Convert(converter = EmailAddressPersistenceConverter.class)
  @Column(name = "email_address", nullable = false, unique = true)
  private EmailAddress emailAddress;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", length = 20)
  private ProfileRole role = ProfileRole.USER;

  /**
   * Optional opaque annotation supplied by the bounded context that requested the profile, such
   * as the avatar URL released by an identity provider. Stored verbatim and never interpreted
   * by the persistence layer.
   */
  @Column(name = "auxiliary_data", length = 512)
  private String auxiliaryData;

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "street", column = @Column(name = "street_address_street")),
      @AttributeOverride(name = "number", column = @Column(name = "street_address_number")),
      @AttributeOverride(name = "city", column = @Column(name = "street_address_city")),
      @AttributeOverride(name = "postalCode", column = @Column(name = "street_address_postal_code")),
      @AttributeOverride(name = "country", column = @Column(name = "street_address_country"))})
  private StreetAddressPersistenceEmbeddable streetAddress;

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "number", column = @Column(name = "phone_number"))})
  private PhonePersistenceEmbeddable phone;

  public ProfilePersistenceEntity() {
  }
}
