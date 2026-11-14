package pe.edu.upc.ice.track.platform.profiles.test.fixtures;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.DashboardConfig;
import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.OwnerProfile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.TechnicianProfile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.entities.DashboardCard;
import pe.edu.upc.ice.track.platform.profiles.domain.model.factories.ProfileCreationData;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.CardType;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.EmailAddress;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.PersonName;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.Phone;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.Ruc;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.Speciality;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.StreetAddress;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.TemperatureRange;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.SiteId;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.util.List;

public final class ProfilesDomainFixtures {

  public static final Long USER_ID = 9L;
  public static final Long OTHER_USER_ID = 10L;
  public static final Long OWNER_ID = 5L;
  public static final Long TECHNICIAN_ID = 6L;
  public static final Long SITE_ID = 3L;
  public static final Long DASHBOARD_ID = 20L;
  public static final Long DASHBOARD_VERSION = 3L;
  public static final Long RUC = 20123456789L;
  public static final String EMAIL = "ana.torres@example.com";
  public static final String OTHER_EMAIL = "luis.perez@example.com";
  public static final String CERTIFICATION = "CERT-001";
  public static final String USERNAME = "atorres";

  private ProfilesDomainFixtures() {
  }

  public static PersonName personName() {
    return new PersonName("Ana", "Torres");
  }

  public static EmailAddress email() {
    return new EmailAddress(EMAIL);
  }

  public static Phone phone() {
    return new Phone("+51", "987654321");
  }

  public static StreetAddress address() {
    return new StreetAddress("Av. Primavera", "120", "Lima", "15023", "Peru");
  }

  public static Ruc ruc() {
    return new Ruc(RUC);
  }

  public static Speciality speciality() {
    return new Speciality("Refrigeration");
  }

  public static TemperatureRange temperatureRange() {
    return new TemperatureRange(2, 8, "C", "Cold chain");
  }

  public static ProfileCreationData ownerData() {
    return ProfileCreationData.forOwner(personName(), email(), phone(), address(), ruc());
  }

  public static ProfileCreationData technicianData() {
    return ProfileCreationData.forTechnician(personName(), email(), phone(), address(), speciality(), CERTIFICATION);
  }

  public static OwnerProfile owner() {
    return new OwnerProfile(new UserId(USER_ID), personName(), email(), phone(), address(), ruc());
  }

  public static OwnerProfile persistedOwner() {
    var owner = new OwnerProfile(OWNER_ID, new UserId(USER_ID), personName(), email(), phone(), address(), ruc());
    owner.restoreVersion(2L);
    return owner;
  }

  public static TechnicianProfile technician() {
    return new TechnicianProfile(new UserId(USER_ID), personName(), email(), phone(), address(), speciality(),
        CERTIFICATION);
  }

  public static TechnicianProfile persistedTechnician() {
    return new TechnicianProfile(TECHNICIAN_ID, new UserId(USER_ID), personName(), email(), phone(), address(),
        speciality(), CERTIFICATION);
  }

  public static TechnicianProfile technicianNamed(Long id, Long userId, String firstName, String lastName,
                                                  String specialityName) {
    return new TechnicianProfile(id, new UserId(userId), new PersonName(firstName, lastName),
        new EmailAddress(firstName.toLowerCase() + id + "@example.com"), phone(), address(),
        new Speciality(specialityName), CERTIFICATION);
  }

  public static DashboardConfig dashboard() {
    return new DashboardConfig(new UserId(USER_ID), new SiteId(SITE_ID), temperatureRange());
  }

  public static DashboardConfig persistedDashboard() {
    return new DashboardConfig(DASHBOARD_ID, DASHBOARD_VERSION, new UserId(USER_ID), new SiteId(SITE_ID),
        temperatureRange(), persistedCards());
  }

  public static List<DashboardCard> persistedCards() {
    return List.of(
        new DashboardCard(101L, CardType.MONITORED_EQUIPMENT, 1, true),
        new DashboardCard(102L, CardType.OPEN_ALERTS, 2, true),
        new DashboardCard(103L, CardType.ACTIVE_ORDERS, 3, true),
        new DashboardCard(104L, CardType.EQUIPMENT_STATUS, 4, true));
  }
}
