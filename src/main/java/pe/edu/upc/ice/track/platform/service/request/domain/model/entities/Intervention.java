package pe.edu.upc.ice.track.platform.service.request.domain.model.entities;

import lombok.Getter;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.EquipmentId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.TechnicianProfileId;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The technical report a technician registers when the work on a service request is done.
 *
 * <p>Identified by {@code clientGeneratedId}, a UUID the technician's device generates before it
 * submits the report. A device working offline retries until it is acknowledged, so the same report
 * may arrive several times; the identifier is what lets the {@code ServiceRequest} recognize a
 * replay and answer it with the report it already holds instead of registering a second one.</p>
 *
 * <p>An intervention is immutable once registered: correcting a report is not a feature of this
 * context, and an amended report would make the completion instant of the request a lie.</p>
 *
 * <p>No JPA or persistence annotation is present here - the mapping lives exclusively in
 * {@code InterventionPersistenceEntity}.</p>
 */
@Getter
public class Intervention {

  /** Maximum number of characters the diagnosis and the actions taken may each hold. */
  public static final int MAX_TEXT_LENGTH = 2000;

  /** Maximum number of spare parts a report may list. */
  public static final int MAX_SPARE_PARTS = 50;

  /** Maximum number of characters a spare part entry may hold. */
  public static final int MAX_SPARE_PART_LENGTH = 100;

  /** Maximum number of photos a report may reference. */
  public static final int MAX_PHOTOS = 20;

  /** Maximum number of characters a photo reference (a URL or a storage key) may hold. */
  public static final int MAX_PHOTO_REFERENCE_LENGTH = 2048;

  private final UUID clientGeneratedId;
  private final TechnicianProfileId technicianId;
  private final EquipmentId equipmentId;
  private final String diagnosis;
  private final String actionsTaken;
  private final List<String> spareParts;
  private final List<String> photoEvidence;
  private final Instant startedAt;
  private final Instant completedAt;

  /**
   * Creates or reconstitutes an intervention.
   *
   * @param clientGeneratedId the identifier the technician's device generated; required
   * @param technicianId      the technician who performed the work; required
   * @param equipmentId       the unit the work was performed on; required
   * @param diagnosis         what the technician found; required, not blank
   * @param actionsTaken      what the technician did; required, not blank
   * @param spareParts        the spare parts used, may be {@code null} or empty
   * @param photoEvidence     references to the photos taken, may be {@code null} or empty
   * @param startedAt         when the work started; required
   * @param completedAt       when the work finished; required and not before {@code startedAt}
   * @throws IllegalArgumentException when a value violates an invariant of the report
   */
  public Intervention(UUID clientGeneratedId, TechnicianProfileId technicianId, EquipmentId equipmentId,
                      String diagnosis, String actionsTaken, List<String> spareParts, List<String> photoEvidence,
                      Instant startedAt, Instant completedAt) {
    this.clientGeneratedId = requireValue(clientGeneratedId, "clientGeneratedId");
    this.technicianId = requireValue(technicianId, "technicianId");
    this.equipmentId = requireValue(equipmentId, "equipmentId");
    this.diagnosis = requiredText(diagnosis, "diagnosis");
    this.actionsTaken = requiredText(actionsTaken, "actionsTaken");
    this.spareParts = validatedEntries(spareParts, "spareParts", MAX_SPARE_PARTS, MAX_SPARE_PART_LENGTH);
    this.photoEvidence = validatedEntries(photoEvidence, "photoEvidence", MAX_PHOTOS, MAX_PHOTO_REFERENCE_LENGTH);
    this.startedAt = requireValue(startedAt, "startedAt");
    this.completedAt = requireValue(completedAt, "completedAt");
    if (completedAt.isBefore(startedAt)) {
      throw new IllegalArgumentException("An intervention cannot be completed before it started");
    }
  }

  /**
   * Computes how long the work took.
   *
   * @return the time between the start and the completion, never negative
   */
  public Duration duration() {
    return Duration.between(startedAt, completedAt);
  }

  /**
   * Tells whether this report is the one the given client identifier names.
   *
   * @param candidate the identifier to test
   * @return {@code true} when the identifiers match
   */
  public boolean isIdentifiedBy(UUID candidate) {
    return clientGeneratedId.equals(candidate);
  }

  private static <T> T requireValue(T value, String field) {
    if (value == null) {
      throw new IllegalArgumentException("Intervention %s must not be null".formatted(field));
    }
    return value;
  }

  private static String requiredText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Intervention %s must not be null or blank".formatted(field));
    }
    var trimmed = value.strip();
    if (trimmed.length() > MAX_TEXT_LENGTH) {
      throw new IllegalArgumentException(
          "Intervention %s must not exceed %d characters".formatted(field, MAX_TEXT_LENGTH));
    }
    return trimmed;
  }

  private static List<String> validatedEntries(List<String> entries, String field, int maxEntries, int maxLength) {
    if (entries == null || entries.isEmpty()) {
      return List.of();
    }
    if (entries.size() > maxEntries) {
      throw new IllegalArgumentException(
          "Intervention %s must not hold more than %d entries".formatted(field, maxEntries));
    }
    var validated = new ArrayList<String>(entries.size());
    for (var entry : entries) {
      if (entry == null || entry.isBlank()) {
        throw new IllegalArgumentException("Intervention %s must not hold blank entries".formatted(field));
      }
      var trimmed = entry.strip();
      if (trimmed.length() > maxLength) {
        throw new IllegalArgumentException(
            "Intervention %s entries must not exceed %d characters".formatted(field, maxLength));
      }
      validated.add(trimmed);
    }
    return List.copyOf(validated);
  }
}
