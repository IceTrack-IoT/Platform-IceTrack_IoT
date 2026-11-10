package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.Alert;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertPage;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.AlertResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.PagedAlertResource;

/**
 * Maps an {@link Alert}, or a page of them, into its REST representation.
 *
 * <p>{@code Temperature} is unwrapped to a plain {@code Double} here, the same boundary
 * discipline as {@code SensorReadingResourceFromEntityAssembler}.</p>
 */
public final class AlertResourceFromEntityAssembler {

  private AlertResourceFromEntityAssembler() {
  }

  public static AlertResource toResourceFromEntity(Alert alert) {
    var totalDuration = alert.totalDuration();
    return new AlertResource(
        alert.getId(),
        alert.getEquipmentId(),
        alert.getType().name(),
        alert.getSeverity().name(),
        alert.getStatus().name(),
        alert.getTriggeringReadingId(),
        alert.getTriggeringTemperature().celsius(),
        alert.getTriggeringRecordedAt(),
        alert.getPeakTemperature().celsius(),
        alert.getOpenedAt(),
        alert.getAcknowledgedBy(),
        alert.getAcknowledgedAt(),
        alert.getResolvedBy(),
        alert.getResolvedAt(),
        alert.getDismissedBy(),
        alert.getDismissedAt(),
        alert.getDismissReason(),
        alert.getDismissEvidence(),
        totalDuration == null ? null : totalDuration.toSeconds());
  }

  /**
   * Converts an {@link AlertPage} into its outbound paged resource.
   *
   * @param page the page to convert
   * @return the {@link PagedAlertResource} resource
   */
  public static PagedAlertResource toPagedResourceFromPage(AlertPage page) {
    return new PagedAlertResource(
        page.content().stream().map(AlertResourceFromEntityAssembler::toResourceFromEntity).toList(),
        page.page(),
        page.size(),
        page.totalElements(),
        page.totalPages());
  }
}
