package pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects;

/**
 * TechnicianQualification Value Object.
 *
 * <p>What makes a technician eligible to maintain ice tracks: the speciality they practise and the
 * number of the certification that backs it. Both components are mandatory.</p>
 *
 * @param speciality          the technician's speciality; required
 * @param certificationNumber the number of the technician's certification; required
 */
public record TechnicianQualification(String speciality, String certificationNumber) {

  /**
   * Validates and normalises both components.
   */
  public TechnicianQualification {
    if (speciality == null || speciality.isBlank()) {
      throw new IllegalArgumentException("Speciality must not be null or blank");
    }
    if (certificationNumber == null || certificationNumber.isBlank()) {
      throw new IllegalArgumentException("Certification number must not be null or blank");
    }
    speciality = speciality.trim();
    certificationNumber = certificationNumber.trim();
  }
}
