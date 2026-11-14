package pe.edu.upc.ice.track.platform.profiles.test.fixtures;

import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.CardLayoutCommandItem;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.CreateOwnerCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.CreateTechnicianCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.InitializeDashboardConfigCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.ResetDashboardConfigToDefaultCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.ToggleCardVisibilityCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.UpdateDashboardDefaultsCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.UpdateDashboardLayoutCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.UpdateOwnerCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.UpdateTechnicianCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.PersonName;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.Phone;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.Ruc;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.Speciality;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.StreetAddress;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.util.List;

import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.CERTIFICATION;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.OWNER_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.SITE_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.TECHNICIAN_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.USER_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.ownerData;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.technicianData;

public final class ProfilesCommandFixtures {

  private ProfilesCommandFixtures() {
  }

  public static CreateOwnerCommand createOwnerCommand() {
    return new CreateOwnerCommand(new UserId(USER_ID), ownerData());
  }

  public static CreateTechnicianCommand createTechnicianCommand() {
    return new CreateTechnicianCommand(new UserId(USER_ID), technicianData());
  }

  public static UpdateOwnerCommand updateOwnerCommand(Long expectedVersion) {
    return new UpdateOwnerCommand(OWNER_ID, new PersonName("Maria", "Lopez"), new Phone("+51", "911222333"),
        new StreetAddress("Calle Sol", "5", "Cusco", "08000", "Peru"), new Ruc(10987654321L), expectedVersion);
  }

  public static UpdateTechnicianCommand updateTechnicianCommand(String certificationNumber) {
    return new UpdateTechnicianCommand(TECHNICIAN_ID, new PersonName("Luis", "Perez"), new Phone("+51", "955666777"),
        new StreetAddress("Jr. Luna", "7", "Arequipa", "04000", "Peru"), new Speciality("HVAC"),
        certificationNumber);
  }

  public static InitializeDashboardConfigCommand initializeDashboardCommand() {
    return new InitializeDashboardConfigCommand(USER_ID, SITE_ID, 2, 8, "C", "Cold chain");
  }

  public static UpdateDashboardDefaultsCommand updateDefaultsCommand(Long expectedVersion) {
    return new UpdateDashboardDefaultsCommand(USER_ID, 4L, -20, -15, "c", "Freezer", expectedVersion);
  }

  /** Reverses the four persisted cards (101..104) and hides the first one. */
  public static UpdateDashboardLayoutCommand reverseLayoutCommand(Long expectedVersion) {
    return new UpdateDashboardLayoutCommand(USER_ID, List.of(
        new CardLayoutCommandItem(101L, 4, false),
        new CardLayoutCommandItem(102L, 3, true),
        new CardLayoutCommandItem(103L, 2, true),
        new CardLayoutCommandItem(104L, 1, true)), expectedVersion);
  }

  public static ToggleCardVisibilityCommand toggleCommand(Long cardId, Long expectedVersion) {
    return new ToggleCardVisibilityCommand(USER_ID, cardId, expectedVersion);
  }

  public static ResetDashboardConfigToDefaultCommand resetCommand() {
    return new ResetDashboardConfigToDefaultCommand(USER_ID);
  }

  public static String certification() {
    return CERTIFICATION;
  }
}
