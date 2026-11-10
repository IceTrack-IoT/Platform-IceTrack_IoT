package pe.edu.upc.ice.track.platform.profiles.domain.model.readmodels;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.TechnicianProfile;

import java.util.Objects;
import java.util.Optional;

/**
 * One technician of the public directory: its profile next to its projected statistics.
 *
 * <p>The statistics are a separate read model, created by the first completed service or review of
 * the technician, so a technician may have none yet. That reads as no completed service and no
 * rating, never as a rating of zero.</p>
 *
 * @param profile the technician profile; required
 * @param metrics the technician's projected statistics, or {@code null} when nothing was projected
 *                for it yet
 */
public record TechnicianDirectoryEntry(TechnicianProfile profile, TechnicianMetrics metrics) {

  /**
   * Validates that the profile is present.
   */
  public TechnicianDirectoryEntry {
    Objects.requireNonNull(profile, "profile must not be null");
  }

  /**
   * @return the technician's full name
   */
  public String fullName() {
    return profile.getFullName().getFullName();
  }

  /**
   * @return the name of the technician's speciality
   */
  public String specialityName() {
    return profile.getSpeciality().name();
  }

  /**
   * @return the services the technician completed, {@code 0} when nothing was projected yet
   */
  public int completedServices() {
    return metrics == null ? 0 : metrics.getCompletedServicesCount();
  }

  /**
   * @return the technician's average rating, or {@code null} until its first review
   */
  public Double averageRating() {
    return metrics == null ? null : metrics.getAverageRating();
  }

  /**
   * @return the projected statistics, empty when nothing was projected yet
   */
  public Optional<TechnicianMetrics> findMetrics() {
    return Optional.ofNullable(metrics);
  }
}
