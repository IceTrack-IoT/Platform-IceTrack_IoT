package pe.edu.upc.ice.track.platform.profiles.domain.model.commands;

import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.PersonName;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.Phone;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.Speciality;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.StreetAddress;

import java.util.Objects;

/**
 * Command to replace the editable details of a technician.
 *
 * <p>The identifiers and the email address are not part of the command: the account binding
 * never changes, and the email is owned by the account in the IAM context.</p>
 *
 * @param technicianId        identifier of the technician to update; required
 * @param fullName            the new name; required
 * @param phone               the new phone number; required
 * @param address             the new street address; required
 * @param speciality          the certified speciality; required
 * @param certificationNumber the number of the certification backing the speciality; required
 */
public record UpdateTechnicianCommand(
    Long technicianId,
    PersonName fullName,
    Phone phone,
    StreetAddress address,
    Speciality speciality,
    String certificationNumber) {

  /**
   * Validates that every component was supplied.
   */
  public UpdateTechnicianCommand {
    Objects.requireNonNull(technicianId, "technicianId must not be null");
    Objects.requireNonNull(fullName, "fullName must not be null");
    Objects.requireNonNull(phone, "phone must not be null");
    Objects.requireNonNull(address, "address must not be null");
    Objects.requireNonNull(speciality, "speciality must not be null");
    if (certificationNumber == null || certificationNumber.isBlank()) {
      throw new IllegalArgumentException("Certification number must not be null or blank");
    }
  }
}
