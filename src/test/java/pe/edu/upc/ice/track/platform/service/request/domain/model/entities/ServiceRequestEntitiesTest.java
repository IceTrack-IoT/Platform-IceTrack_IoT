package pe.edu.upc.ice.track.platform.service.request.domain.model.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pe.edu.upc.ice.track.platform.service.request.domain.model.entities.Intervention;
import pe.edu.upc.ice.track.platform.service.request.domain.model.entities.MaintenanceWindowLink;
import pe.edu.upc.ice.track.platform.service.request.domain.model.entities.StatusTransition;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.MaintenanceOrigin;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceStatus;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.EquipmentId;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pe.edu.upc.ice.track.platform.service.request.test.fixtures.ServiceRequestDomainFixtures.*;

class ServiceRequestEntitiesTest {

  private static final Instant START = Instant.parse("2026-03-01T09:00:00Z");
  private static final Instant END = Instant.parse("2026-03-01T11:00:00Z");

  private static Intervention intervention(String diagnosis, String actions, List<String> parts, List<String> photos,
                                           Instant start, Instant end) {
    return new Intervention(CLIENT_ID, technician(), new EquipmentId(EQUIPMENT_ID), diagnosis, actions, parts, photos, start, end);
  }

  @Nested
  class InterventionTests {

    @Test
    @DisplayName("new Intervention(...) -> trims texts and entries, copies lists, exposes duration (AAA)")
    void valid() {
      // Arrange
      var parts = new ArrayList<>(List.of(" bearing ", "seal"));

      // Act
      var intervention = intervention(" Worn ", " Replaced ", parts, List.of("a.jpg"), START, END);
      parts.add("extra");

      // Assert
      assertThat(intervention.getDiagnosis()).isEqualTo("Worn");
      assertThat(intervention.getActionsTaken()).isEqualTo("Replaced");
      assertThat(intervention.getSpareParts()).containsExactly("bearing", "seal");
      assertThat(intervention.getPhotoEvidence()).containsExactly("a.jpg");
      assertThat(intervention.duration()).isEqualTo(Duration.ofHours(2));
      assertThat(intervention.isIdentifiedBy(CLIENT_ID)).isTrue();
      assertThat(intervention.isIdentifiedBy(java.util.UUID.randomUUID())).isFalse();
      assertThatThrownBy(() -> intervention.getSpareParts().add("x")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("new Intervention(null / empty lists) -> empty immutable lists; equal start and end allowed (AAA)")
    void emptyLists() {
      // Act
      var intervention = intervention("d", "a", null, Collections.emptyList(), START, START);

      // Assert
      assertThat(intervention.getSpareParts()).isEmpty();
      assertThat(intervention.getPhotoEvidence()).isEmpty();
      assertThat(intervention.duration()).isEqualTo(Duration.ZERO);
    }

    @Test
    @DisplayName("new Intervention(null parts) -> IllegalArgumentException naming the field (AAA)")
    void nullParts() {
      // Act & Assert
      assertThatThrownBy(() -> new Intervention(null, technician(), new EquipmentId(1L), "d", "a", null, null, START, END))
          .hasMessage("Intervention clientGeneratedId must not be null");
      assertThatThrownBy(() -> new Intervention(CLIENT_ID, null, new EquipmentId(1L), "d", "a", null, null, START, END))
          .hasMessage("Intervention technicianId must not be null");
      assertThatThrownBy(() -> new Intervention(CLIENT_ID, technician(), null, "d", "a", null, null, START, END))
          .hasMessage("Intervention equipmentId must not be null");
      assertThatThrownBy(() -> intervention("d", "a", null, null, null, END)).hasMessage("Intervention startedAt must not be null");
      assertThatThrownBy(() -> intervention("d", "a", null, null, START, null)).hasMessage("Intervention completedAt must not be null");
    }

    @Test
    @DisplayName("new Intervention(blank or oversized text) -> IllegalArgumentException (AAA)")
    void textRules() {
      // Act & Assert
      assertThatThrownBy(() -> intervention(" ", "a", null, null, START, END)).hasMessage("Intervention diagnosis must not be null or blank");
      assertThatThrownBy(() -> intervention("d", null, null, null, START, END)).hasMessage("Intervention actionsTaken must not be null or blank");
      assertThatThrownBy(() -> intervention("x".repeat(2001), "a", null, null, START, END))
          .hasMessage("Intervention diagnosis must not exceed 2000 characters");
      assertThat(intervention("x".repeat(2000), "a", null, null, START, END).getDiagnosis()).hasSize(2000);
    }

    @Test
    @DisplayName("new Intervention(entries over limits or blank) -> IllegalArgumentException (AAA)")
    void entryRules() {
      // Arrange
      var tooManyParts = Collections.nCopies(51, "p");
      var tooManyPhotos = Collections.nCopies(21, "p");

      // Act & Assert
      assertThatThrownBy(() -> intervention("d", "a", tooManyParts, null, START, END))
          .hasMessage("Intervention spareParts must not hold more than 50 entries");
      assertThat(intervention("d", "a", Collections.nCopies(50, "p"), null, START, END).getSpareParts()).hasSize(50);
      assertThatThrownBy(() -> intervention("d", "a", null, tooManyPhotos, START, END))
          .hasMessage("Intervention photoEvidence must not hold more than 20 entries");
      assertThatThrownBy(() -> intervention("d", "a", List.of("ok", " "), null, START, END))
          .hasMessage("Intervention spareParts must not hold blank entries");
      assertThatThrownBy(() -> intervention("d", "a", java.util.Arrays.asList("ok", null), null, START, END))
          .hasMessage("Intervention spareParts must not hold blank entries");
      assertThatThrownBy(() -> intervention("d", "a", List.of("x".repeat(101)), null, START, END))
          .hasMessage("Intervention spareParts entries must not exceed 100 characters");
      assertThatThrownBy(() -> intervention("d", "a", null, List.of("x".repeat(2049)), START, END))
          .hasMessage("Intervention photoEvidence entries must not exceed 2048 characters");
      assertThat(intervention("d", "a", null, List.of("x".repeat(2048)), START, END).getPhotoEvidence()).hasSize(1);
    }

    @Test
    @DisplayName("new Intervention(completed before started) -> IllegalArgumentException (AAA)")
    void chronology() {
      // Act & Assert
      assertThatThrownBy(() -> intervention("d", "a", null, null, END, START))
          .hasMessage("An intervention cannot be completed before it started");
    }
  }

  @Nested
  class MaintenanceWindowLinkTests {

    @Test
    @DisplayName("MaintenanceWindowLink.open(...) -> open link without end or reason (AAA)")
    void open() {
      // Act
      var link = MaintenanceWindowLink.open(WINDOW_ID, MaintenanceOrigin.BUTTON, TECHNICIAN_ID, START);

      // Assert
      assertThat(link.getId()).isNull();
      assertThat(link.getEndedAt()).isNull();
      assertThat(link.getClosureReason()).isNull();
      assertThat(link.isFor(WINDOW_ID)).isTrue();
      assertThat(link.isFor(1L)).isFalse();
      assertThat(link.getTechnicianProfileId()).isEqualTo(TECHNICIAN_ID);
    }

    @Test
    @DisplayName("new MaintenanceWindowLink(null window / origin / start) -> IllegalArgumentException (AAA)")
    void requiredParts() {
      // Act & Assert
      assertThatThrownBy(() -> MaintenanceWindowLink.open(null, MaintenanceOrigin.APP, null, START))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("A maintenance window link needs the window, its origin and its start");
      assertThatThrownBy(() -> MaintenanceWindowLink.open(1L, null, null, START)).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> MaintenanceWindowLink.open(1L, MaintenanceOrigin.APP, null, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("new MaintenanceWindowLink(..., reason) -> reason trimmed, blank becomes null, over 30 chars rejected (AAA)")
    void reasonNormalisation() {
      // Act & Assert
      assertThat(new MaintenanceWindowLink(1L, 2L, MaintenanceOrigin.APP, null, START, END, "  DONE ").getClosureReason()).isEqualTo("DONE");
      assertThat(new MaintenanceWindowLink(1L, 2L, MaintenanceOrigin.APP, null, START, END, "   ").getClosureReason()).isNull();
      assertThat(new MaintenanceWindowLink(1L, 2L, MaintenanceOrigin.APP, null, START, END, "x".repeat(30)).getClosureReason()).hasSize(30);
      assertThatThrownBy(() -> new MaintenanceWindowLink(1L, 2L, MaintenanceOrigin.APP, null, START, END, "x".repeat(31)))
          .hasMessage("A closure reason must not exceed 30 characters");
    }

    @Test
    @DisplayName("close(endedAt, reason) -> true once, stores trimmed reason; later closes return false and change nothing (AAA)")
    void close() {
      // Arrange
      var link = MaintenanceWindowLink.open(WINDOW_ID, MaintenanceOrigin.APP, null, START);

      // Act
      var first = link.close(END, " AUTO ");
      var second = link.close(END.plusSeconds(500), "OTHER");

      // Assert
      assertThat(first).isTrue();
      assertThat(second).isFalse();
      assertThat(link.getEndedAt()).isEqualTo(END);
      assertThat(link.getClosureReason()).isEqualTo("AUTO");
    }

    @Test
    @DisplayName("close(endedAt before start) -> end clamped to the start (AAA)")
    void closeClamped() {
      // Arrange
      var link = MaintenanceWindowLink.open(WINDOW_ID, MaintenanceOrigin.APP, null, START);

      // Act
      link.close(START.minusSeconds(30), "AUTO");

      // Assert
      assertThat(link.getEndedAt()).isEqualTo(START);
    }

    @Test
    @DisplayName("close(null end / blank reason) -> NullPointerException / IllegalArgumentException; already closed ignores them (AAA)")
    void closeGuards() {
      // Arrange
      var open = MaintenanceWindowLink.open(WINDOW_ID, MaintenanceOrigin.APP, null, START);
      var closed = new MaintenanceWindowLink(1L, WINDOW_ID, MaintenanceOrigin.APP, null, START, END, "AUTO");

      // Act & Assert
      assertThatThrownBy(() -> open.close(null, "AUTO")).isInstanceOf(NullPointerException.class).hasMessage("endedAt must not be null");
      assertThatThrownBy(() -> open.close(END, " ")).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("A closure reason is required to complete a maintenance window link");
      assertThat(open.getEndedAt()).isNull();
      assertThat(closed.close(END, null)).isFalse();
    }
  }

  @Nested
  class StatusTransitionTests {

    @Test
    @DisplayName("StatusTransition.of(...) -> keeps fields, trims the reason, creation detected by missing origin (AAA)")
    void of() {
      // Act
      var creation = StatusTransition.of(null, ServiceStatus.PENDING, OWNER_ID, START, "  because ", technician());
      var move = StatusTransition.of(ServiceStatus.PENDING, ServiceStatus.ACCEPTED, TECHNICIAN_ID, END, null, null);

      // Assert
      assertThat(creation.isCreation()).isTrue();
      assertThat(creation.getReason()).isEqualTo("because");
      assertThat(creation.getTechnicianId()).isEqualTo(technician());
      assertThat(creation.getId()).isNull();
      assertThat(move.isCreation()).isFalse();
      assertThat(move.getFromStatus()).isEqualTo(ServiceStatus.PENDING);
      assertThat(move.getActorId()).isEqualTo(TECHNICIAN_ID);
      assertThat(move.getOccurredAt()).isEqualTo(END);
      assertThat(move.getReason()).isNull();
    }

    @Test
    @DisplayName("new StatusTransition(blank reason) -> reason null; 500 chars accepted, 501 rejected (AAA)")
    void reason() {
      // Act & Assert
      assertThat(StatusTransition.of(null, ServiceStatus.PENDING, 1L, START, "   ", null).getReason()).isNull();
      assertThat(StatusTransition.of(null, ServiceStatus.PENDING, 1L, START, "x".repeat(500), null).getReason()).hasSize(500);
      assertThatThrownBy(() -> StatusTransition.of(null, ServiceStatus.PENDING, 1L, START, "x".repeat(501), null))
          .hasMessage("Status transition reason must not exceed 500 characters");
    }

    @Test
    @DisplayName("new StatusTransition(null target / actor / instant) -> IllegalArgumentException (AAA)")
    void requiredParts() {
      // Act & Assert
      assertThatThrownBy(() -> StatusTransition.of(null, null, 1L, START, null, null)).hasMessage("Status transition target must not be null");
      assertThatThrownBy(() -> StatusTransition.of(null, ServiceStatus.PENDING, null, START, null, null)).hasMessage("Status transition actor must not be null");
      assertThatThrownBy(() -> StatusTransition.of(null, ServiceStatus.PENDING, 1L, null, null, null)).hasMessage("Status transition instant must not be null");
    }
  }
}
