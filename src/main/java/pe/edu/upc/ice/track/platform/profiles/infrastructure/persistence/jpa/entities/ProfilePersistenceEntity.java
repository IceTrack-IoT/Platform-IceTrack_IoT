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
 *
 * <p>Both concrete profiles share this single table. The {@code role} column is the discriminator
 * the assembler uses to reconstitute an {@code OwnerProfile} or a {@code TechnicianProfile}; the
 * role specific columns are only populated for their own role, and the domain model refuses to
 * build a profile whose role specific data is missing.</p>
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
  @Column(name = "user_id", nullable = false, unique = true)
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
  @Column(name = "role", nullable = false, length = 20)
  private ProfileRole role;

  /**
   * Taxpayer registration number. Populated for {@link ProfileRole#OWNER} profiles only.
   */
  @Column(name = "ruc")
  private Long ruc;

  /**
   * Speciality of the technician. Populated for {@link ProfileRole#TECHNICIAN} profiles only.
   */
  @Column(name = "speciality", length = 100)
  private String speciality;

  /**
   * Certification number of the technician. Populated for {@link ProfileRole#TECHNICIAN}
   * profiles only.
   */
  @Column(name = "certification_number", length = 50)
  private String certificationNumber;

  /**
   * Optional opaque annotation supplied by the bounded context that requested the profile.
   * Stored verbatim and never interpreted by the persistence layer.
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
