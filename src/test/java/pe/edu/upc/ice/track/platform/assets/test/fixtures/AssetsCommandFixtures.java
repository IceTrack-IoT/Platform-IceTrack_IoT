package pe.edu.upc.ice.track.platform.assets.test.fixtures;

import pe.edu.upc.ice.track.platform.assets.domain.model.commands.*;
import pe.edu.upc.ice.track.platform.assets.domain.model.queries.GetEquipmentByIdQuery;
import pe.edu.upc.ice.track.platform.assets.domain.model.queries.GetEquipmentBySiteQuery;
import pe.edu.upc.ice.track.platform.assets.domain.model.queries.GetSiteByIdQuery;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.EquipmentType;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.StatusEquipment;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.CallerRole;

import java.time.Instant;

import static pe.edu.upc.ice.track.platform.assets.test.fixtures.AssetsDomainFixtures.*;

/**
 * Assets-specific command and query factories. Every static method returns a fresh, valid command
 * built from canonical constants. Tests MUST NOT instantiate assets commands inline; a test that
 * needs an invalid value overrides it through the matching parameter.
 */
public final class AssetsCommandFixtures {

  public static final String UPDATED_EQUIPMENT_NAME = "Refrigerador Principal";
  public static final Integer UPDATED_REMINDER_INTERVAL_DAYS = 120;
  public static final String UPDATED_SITE_NAME = "Sede Norte";
  public static final String UPDATED_CONTACT_NAME = "Carlos Diaz";
  public static final String UPDATED_PHONE = "+51 912345678";

  private AssetsCommandFixtures() {
    throw new AssertionError("AssetsCommandFixtures is a utility class and must not be instantiated.");
  }

  public static RegisterEquipmentCommand registerEquipmentCommand() {
    return registerEquipmentCommand(VALID_UID, VALID_MIN_CELSIUS, VALID_MAX_CELSIUS);
  }

  public static RegisterEquipmentCommand registerEquipmentCommand(String uid, Double minCelsius, Double maxCelsius) {
    return new RegisterEquipmentCommand(
        USERNAME, SITE_ID, uid, VALID_EQUIPMENT_NAME, EquipmentType.REFRIGERATOR, minCelsius, maxCelsius,
        VALID_REMINDER_INTERVAL_DAYS);
  }

  public static UpdateEquipmentCommand updateEquipmentCommand(Long expectedVersion) {
    return updateEquipmentCommand(UPDATED_EQUIPMENT_NAME, expectedVersion);
  }

  public static UpdateEquipmentCommand updateEquipmentCommand(String name, Long expectedVersion) {
    return new UpdateEquipmentCommand(
        EQUIPMENT_ID, USERNAME, name, EquipmentType.DISPLAY_CASE, UPDATED_REMINDER_INTERVAL_DAYS, expectedVersion);
  }

  public static ChangeThresholdCommand changeThresholdCommand(Double minCelsius, Double maxCelsius, Long expectedVersion) {
    return new ChangeThresholdCommand(EQUIPMENT_ID, USERNAME, minCelsius, maxCelsius, expectedVersion);
  }

  public static ChangeStatusCommand changeStatusCommand(StatusEquipment newStatus, Long expectedVersion) {
    return new ChangeStatusCommand(EQUIPMENT_ID, USERNAME, newStatus, expectedVersion);
  }

  public static BumpConfigVersionCommand bumpConfigVersionCommand() {
    return new BumpConfigVersionCommand(EQUIPMENT_ID, "alert policy updated");
  }

  public static EnterMaintenanceCommand enterMaintenanceCommand() {
    return new EnterMaintenanceCommand(EQUIPMENT_ID, 77L);
  }

  public static LeaveMaintenanceCommand leaveMaintenanceCommand() {
    return new LeaveMaintenanceCommand(EQUIPMENT_ID, 77L);
  }

  public static RecordEquipmentReadingCommand recordEquipmentReadingCommand(
      Double temperatureCelsius, Instant recordedAt, Instant receivedAt) {
    return new RecordEquipmentReadingCommand(EQUIPMENT_ID, temperatureCelsius, recordedAt, receivedAt);
  }

  public static MarkEquipmentOfflineCommand markEquipmentOfflineCommand() {
    return new MarkEquipmentOfflineCommand(EQUIPMENT_ID);
  }

  public static RegisterSiteCommand registerSiteCommand() {
    return registerSiteCommand(null, null);
  }

  public static RegisterSiteCommand registerSiteCommand(Double pinLatitude, Double pinLongitude) {
    return registerSiteCommand(VALID_SITE_NAME, VALID_PHONE, pinLatitude, pinLongitude);
  }

  public static RegisterSiteCommand registerSiteCommand(
      String name, String phone, Double pinLatitude, Double pinLongitude) {
    return new RegisterSiteCommand(
        USERNAME, name, VALID_CONTACT_NAME, phone, VALIDATION_TOKEN, pinLatitude, pinLongitude);
  }

  public static UpdateSiteInfoCommand updateSiteInfoCommand(Long expectedVersion) {
    return updateSiteInfoCommand(UPDATED_SITE_NAME, UPDATED_PHONE, expectedVersion);
  }

  public static UpdateSiteInfoCommand updateSiteInfoCommand(String name, String phone, Long expectedVersion) {
    return new UpdateSiteInfoCommand(SITE_ID, USERNAME, name, UPDATED_CONTACT_NAME, phone, expectedVersion);
  }

  public static UpdateSiteAddressCommand updateSiteAddressCommand(
      Double pinLatitude, Double pinLongitude, Long expectedVersion) {
    return new UpdateSiteAddressCommand(SITE_ID, USERNAME, VALIDATION_TOKEN, pinLatitude, pinLongitude, expectedVersion);
  }

  public static RetrySiteGeocodingCommand retrySiteGeocodingCommand() {
    return new RetrySiteGeocodingCommand(SITE_ID, USERNAME);
  }

  public static ResumeSiteGeocodingCommand resumeSiteGeocodingCommand(boolean finalAttempt) {
    return new ResumeSiteGeocodingCommand(SITE_ID, finalAttempt);
  }

  public static ValidateAddressCommand validateAddressCommand() {
    return validateAddressCommand(VALID_ADDRESS, null);
  }

  public static ValidateAddressCommand validateAddressCommand(String address, String placeId) {
    return new ValidateAddressCommand(USERNAME, address, placeId);
  }

  public static ValidateAddressCommand validateAddressCommandWithPlaceId() {
    return validateAddressCommand(VALID_ADDRESS, VALID_PLACE_ID);
  }

  public static GetEquipmentByIdQuery equipmentByIdQuery(CallerRole callerRole) {
    return new GetEquipmentByIdQuery(EQUIPMENT_ID, usernameFor(callerRole), callerRole);
  }

  public static GetEquipmentBySiteQuery equipmentBySiteQuery(CallerRole callerRole) {
    return new GetEquipmentBySiteQuery(SITE_ID, usernameFor(callerRole), callerRole);
  }

  public static GetSiteByIdQuery siteByIdQuery(CallerRole callerRole) {
    return new GetSiteByIdQuery(SITE_ID, usernameFor(callerRole), callerRole);
  }

  /** The canonical username of the given caller role. */
  public static String usernameFor(CallerRole callerRole) {
    return callerRole == CallerRole.TECHNICIAN
        ? AssetsDomainFixtures.TECHNICIAN_USERNAME
        : AssetsDomainFixtures.USERNAME;
  }
}
