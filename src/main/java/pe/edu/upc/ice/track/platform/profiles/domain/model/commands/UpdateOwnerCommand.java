package pe.edu.upc.ice.track.platform.profiles.domain.model.commands;

import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.PersonName;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.Phone;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.Ruc;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.StreetAddress;

import java.util.Objects;

/**
 * Command to replace the editable details of an owner.
 *
 * <p>The identifiers and the email address are not part of the command: the account binding
 * never changes, and the email is owned by the account in the IAM context.</p>
 *
 * @param ownerId  identifier of the owner to update; required
 * @param fullName the new name; required
 * @param phone    the new phone number; required
 * @param address  the new street address; required
 * @param ruc      the new taxpayer registration number; required
 */
public record UpdateOwnerCommand(
    Long ownerId,
    PersonName fullName,
    Phone phone,
    StreetAddress address,
    Ruc ruc) {

  /**
   * Validates that every component was supplied.
   */
  public UpdateOwnerCommand {
    Objects.requireNonNull(ownerId, "ownerId must not be null");
    Objects.requireNonNull(fullName, "fullName must not be null");
    Objects.requireNonNull(phone, "phone must not be null");
    Objects.requireNonNull(address, "address must not be null");
    Objects.requireNonNull(ruc, "ruc must not be null");
  }
}
