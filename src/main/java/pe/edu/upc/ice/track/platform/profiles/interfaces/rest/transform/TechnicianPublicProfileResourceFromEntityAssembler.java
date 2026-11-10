package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.TechnicianProfile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.readmodels.TechnicianDirectoryPage;
import pe.edu.upc.ice.track.platform.profiles.domain.model.readmodels.TechnicianMetrics;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.PagedTechnicianPublicProfileResource;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.TechnicianPublicProfileResource;

import java.util.Optional;

/**
 * Assembler building a {@link TechnicianPublicProfileResource} from a technician profile and its
 * projected metrics.
 *
 * <p>Only professional data is copied from the profile: email, phone and address are never read
 * here, so they cannot leak into the public view.</p>
 */
public final class TechnicianPublicProfileResourceFromEntityAssembler {

  private TechnicianPublicProfileResourceFromEntityAssembler() {
  }

  /**
   * @param profile the technician profile
   * @param metrics the technician's projected metrics, empty when nothing was projected yet
   * @return the public resource; a technician without metrics has no completed service and no rating
   */
  public static TechnicianPublicProfileResource toResourceFromEntity(TechnicianProfile profile,
                                                                     Optional<TechnicianMetrics> metrics) {
    var averageRating = metrics.map(TechnicianMetrics::getAverageRating).orElse(null);
    return new TechnicianPublicProfileResource(
        profile.getUserProfileId(),
        profile.getFullName().getFullName(),
        profile.getSpeciality().name(),
        profile.getCertificationNumber(),
        metrics.map(TechnicianMetrics::getCompletedServicesCount).orElse(0),
        averageRating,
        averageRating != null);
  }

  /**
   * Converts a {@link TechnicianDirectoryPage} into its outbound paged resource.
   *
   * @param page the page to convert
   * @return the {@link PagedTechnicianPublicProfileResource} resource
   */
  public static PagedTechnicianPublicProfileResource toPagedResourceFromPage(TechnicianDirectoryPage page) {
    return new PagedTechnicianPublicProfileResource(
        page.content().stream()
            .map(entry -> toResourceFromEntity(entry.profile(), entry.findMetrics()))
            .toList(),
        page.page(),
        page.size(),
        page.totalElements(),
        page.totalPages());
  }
}
