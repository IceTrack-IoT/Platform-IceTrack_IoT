package pe.edu.upc.ice.track.platform.reporting.infrastructure.servicerequests.services;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.servicerequests.ExternalServiceRequestsServiceForReporting;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Adapter for {@link ExternalServiceRequestsServiceForReporting}.
 *
 * <p><strong>Pending dependency, not a facade extension.</strong> Unlike the Assets and
 * Monitoring adapters in this bounded context, there is no real
 * <strong>Service Request and Feedback Management</strong> context in this codebase yet for this
 * adapter to wrap -- so there is no facade to extend and no patch to document. Once that context
 * is built, following the same ACL-facade conventions used by {@code monitoring} and
 * {@code notifications} (a {@code ServiceRequestContextFacade} interface plus impl, exposing
 * primitives/{@code String} only), this adapter should be rewritten to inject and delegate to it,
 * mirroring {@code ExternalAssetsServiceForReportingImpl} and
 * {@code ExternalMonitoringServiceForReportingImpl}. Until then, every method throws
 * {@link UnsupportedOperationException}.</p>
 */
@Service
public class ExternalServiceRequestsServiceForReportingImpl implements ExternalServiceRequestsServiceForReporting {

  @Override
  public List<Long> fetchEquipmentIdsWithCompletedPreventiveMaintenanceInRange(
      Long siteId, LocalDateTime from, LocalDateTime to) {
    throw new UnsupportedOperationException(
        "Service Request and Feedback Management has not been built in this codebase yet -- "
            + "see this class's Javadoc.");
  }

  @Override
  public long countCompletedServiceRequestsByTechnicianInRange(
      Long technicianId, LocalDateTime from, LocalDateTime to) {
    throw new UnsupportedOperationException(
        "Service Request and Feedback Management has not been built in this codebase yet -- "
            + "see this class's Javadoc.");
  }

  @Override
  public double fetchAverageRatingByTechnician(Long technicianId) {
    throw new UnsupportedOperationException(
        "Service Request and Feedback Management has not been built in this codebase yet -- "
            + "see this class's Javadoc.");
  }
}
