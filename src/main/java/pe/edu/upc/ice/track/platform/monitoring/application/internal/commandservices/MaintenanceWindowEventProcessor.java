package pe.edu.upc.ice.track.platform.monitoring.application.internal.commandservices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl.ExternalServiceRequestServiceForMonitoring;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.MaintenanceWindow;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.RecordEdgeEventCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.ClosureReason;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.EdgeEventResult;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.MaintenanceOrigin;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.MaintenanceWindowRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Applies the maintenance window events of an edge gateway, and answers whether a window covers an
 * instant.
 *
 * <p>The start and end methods run in the transaction of {@code EdgeEventCommandServiceImpl}, so the
 * window and the record of the event that changed it commit together; a duplicate start event is
 * rejected by the unique indexes on {@code event_uid} and {@code start_event_uid} and rolled back as a
 * whole.</p>
 *
 * <p>A unit holds at most one active window:</p>
 * <ul>
 *   <li>a start on a unit without an active window opens one. When the gateway sends no order, the
 *       order that was in progress on the unit when the window started is looked up in Service Request
 *       Management and, when there is one, the window is attributed to it and its technician;</li>
 *   <li>a start from the app on a window the button started promotes it, keeping its identity;</li>
 *   <li>any other start while a window is active keeps the active window unchanged, except that a
 *       window already past its expiration by the new start's time is closed as {@code EXPIRED}
 *       first, since its end event evidently got lost;</li>
 *   <li>an end closes the window named by {@code start_event_uid}, or the unit's active window.</li>
 * </ul>
 */
@Service
@Slf4j
public class MaintenanceWindowEventProcessor {

  /**
   * What a maintenance window event did.
   *
   * @param result              the outcome recorded on the edge event
   * @param maintenanceWindowId the window concerned, or {@code null} when there was none
   */
  public record Outcome(EdgeEventResult result, Long maintenanceWindowId) {
  }

  private final MaintenanceWindowRepository maintenanceWindowRepository;
  private final ExternalServiceRequestServiceForMonitoring externalServiceRequestService;

  public MaintenanceWindowEventProcessor(
      MaintenanceWindowRepository maintenanceWindowRepository,
      ExternalServiceRequestServiceForMonitoring externalServiceRequestService) {
    this.maintenanceWindowRepository = maintenanceWindowRepository;
    this.externalServiceRequestService = externalServiceRequestService;
  }

  /**
   * Applies a {@code MAINTENANCE_WINDOW_STARTED} event, in the caller's transaction.
   *
   * @param command the start event
   * @return what it did
   */
  public Outcome start(RecordEdgeEventCommand command) {
    var equipmentId = command.equipmentId();
    var startedAt = command.occurredAt();
    var active = maintenanceWindowRepository.findActiveByEquipmentId(equipmentId);
    if (active.isPresent() && active.get().getExpiresAt().isBefore(startedAt)) {
      var overdue = active.get();
      overdue.close(overdue.getExpiresAt(), ClosureReason.EXPIRED);
      maintenanceWindowRepository.save(overdue);
      log.info("Closed overdue maintenance window {} of equipment {} as EXPIRED before opening a new one",
          overdue.getId(), equipmentId);
      active = Optional.empty();
    }
    if (active.isPresent()) {
      var window = active.get();
      if (command.origin() == MaintenanceOrigin.APP && window.promoteToApp(
          command.technicianProfileId(), command.serviceRequestId(), MaintenanceOrigin.APP.expiryFrom(startedAt))) {
        maintenanceWindowRepository.save(window);
        log.info("Promoted maintenance window {} of equipment {} to APP (technician {}, service request {})",
            window.getId(), equipmentId, window.getTechnicianProfileId(), window.getServiceRequestId());
        return new Outcome(EdgeEventResult.MAINTENANCE_WINDOW_PROMOTED, window.getId());
      }
      log.info("Equipment {} already has active maintenance window {}; start event {} keeps it",
          equipmentId, window.getId(), command.eventUid());
      return new Outcome(EdgeEventResult.MAINTENANCE_ALREADY_ACTIVE, window.getId());
    }

    var technicianId = command.technicianProfileId();
    var serviceRequestId = command.serviceRequestId();
    if (serviceRequestId == null) {
      var attribution = externalServiceRequestService.findInProgressServiceRequestAt(equipmentId, startedAt);
      if (attribution.isPresent()) {
        serviceRequestId = attribution.get().serviceRequestId();
        technicianId = technicianId == null ? attribution.get().technicianProfileId() : technicianId;
      }
    }
    var window = MaintenanceWindow.open(
        command.eventUid(), equipmentId, command.origin(), startedAt, technicianId, serviceRequestId);
    var saved = maintenanceWindowRepository.save(window);
    saved.onOpened();
    maintenanceWindowRepository.save(saved);
    log.info("Opened {} maintenance window {} on equipment {} until {} (service request {}{})",
        saved.getOrigin(), saved.getId(), equipmentId, saved.getExpiresAt(), saved.getServiceRequestId(),
        saved.isUnlinked() ? ", unlinked" : "");
    return new Outcome(EdgeEventResult.MAINTENANCE_WINDOW_OPENED, saved.getId());
  }

  /**
   * Applies a {@code MAINTENANCE_WINDOW_ENDED} event, in the caller's transaction.
   *
   * @param command the end event
   * @return what it did
   */
  public Outcome end(RecordEdgeEventCommand command) {
    var equipmentId = command.equipmentId();
    var target = command.startEventUid() != null
        ? maintenanceWindowRepository.findByStartEventUid(command.startEventUid())
            .filter(window -> equipmentId.equals(window.getEquipmentId()))
        : maintenanceWindowRepository.findActiveByEquipmentId(equipmentId);
    if (target.isEmpty()) {
      log.info("End event {} on equipment {} found no maintenance window to close", command.eventUid(), equipmentId);
      return new Outcome(EdgeEventResult.NO_ACTIVE_MAINTENANCE, null);
    }
    var window = target.get();
    if (!window.close(command.occurredAt(), command.closureReason())) {
      return new Outcome(EdgeEventResult.NO_ACTIVE_MAINTENANCE, window.getId());
    }
    maintenanceWindowRepository.save(window);
    log.info("Closed maintenance window {} of equipment {} ({})", window.getId(), equipmentId, window.getClosureReason());
    return new Outcome(EdgeEventResult.MAINTENANCE_WINDOW_CLOSED, window.getId());
  }

  /**
   * Finds the window of a unit covering a timestamp reported by its gateway.
   *
   * @param equipmentId the unit
   * @param timestamp   the gateway timestamp
   * @return the covering window, or empty
   */
  public Optional<MaintenanceWindow> findCovering(Long equipmentId, Instant timestamp) {
    return maintenanceWindowRepository.findCovering(equipmentId, timestamp);
  }

  /**
   * Closes, as {@code EXPIRED} and dated at their expiration, the windows still active more than a
   * grace period after they expired.
   *
   * <p>The gateway closes an expired window itself and reports it; this only settles windows whose
   * gateway never came back, so the unit does not stay in maintenance forever. The grace period
   * leaves a gateway that was offline time to deliver its own end or promotion events.</p>
   *
   * @param now   the current instant
   * @param grace how long after its expiration a window is still left to its gateway
   * @return how many windows were closed
   */
  @Transactional
  public int closeExpired(Instant now, Duration grace) {
    var closed = 0;
    for (var window : maintenanceWindowRepository.findActiveExpiredBefore(now.minus(grace))) {
      if (window.close(window.getExpiresAt(), ClosureReason.EXPIRED)) {
        maintenanceWindowRepository.save(window);
        closed++;
        log.info("Closed maintenance window {} of equipment {} as EXPIRED: its gateway never reported its end",
            window.getId(), window.getEquipmentId());
      }
    }
    return closed;
  }
}
