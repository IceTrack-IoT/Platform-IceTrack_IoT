package pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates;

import lombok.Getter;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.ProfileCreationData;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.ProfileRole;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.TechnicianQualification;

import java.util.Objects;

/**
 * Profile of an ice track maintenance technician.
 *
 * <p>A technician is only eligible for maintenance work with a speciality and a certification,
 * so a technician profile cannot exist without a {@link TechnicianQualification}.</p>
 */
@Getter
public final class TechnicianProfile extends Profile {

  private final TechnicianQualification qualification;

  /**
   * Creates a new, not yet persisted, technician profile.
   *
   * @param data          the validated creation data; required
   * @param qualification the technician's speciality and certification; required
   */
  public TechnicianProfile(ProfileCreationData data, TechnicianQualification qualification) {
    this(null, data, qualification);
  }

  /**
   * Reconstitutes a technician profile.
   *
   * @param id            the persistence identity, or {@code null} for a profile not yet persisted
   * @param data          the validated creation data; required
   * @param qualification the technician's speciality and certification; required
   */
  public TechnicianProfile(Long id, ProfileCreationData data, TechnicianQualification qualification) {
    super(id, data);
    this.qualification = Objects.requireNonNull(qualification, "qualification must not be null");
  }

  // inherited javadoc
  @Override
  public ProfileRole getRole() {
    return ProfileRole.TECHNICIAN;
  }
}
