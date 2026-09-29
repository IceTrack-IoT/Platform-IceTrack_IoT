package pe.edu.upc.ice.track.platform.profiles.domain.repositories;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.TechnicianProfile;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.util.List;
import java.util.Optional;

/**
 * Technician profile repository port.
 */
public interface TechnicianProfileRepository {

  /**
   * Find a technician profile by its identifier.
   *
   * @param userProfileId the profile identifier
   * @return the technician profile, or empty when not found or when the profile belongs to another role
   */
  Optional<TechnicianProfile> findById(Long userProfileId);

  /**
   * Find the technician profile bound to a platform account.
   *
   * @param userId the identifier of the account
   * @return the technician profile, or empty when not found
   */
  Optional<TechnicianProfile> findByUserId(UserId userId);

  /**
   * Find all technician profiles.
   *
   * @return every technician profile
   */
  List<TechnicianProfile> findAll();

  /**
   * Save a technician profile.
   *
   * @param technician the technician profile to save
   * @return the saved technician profile
   */
  TechnicianProfile save(TechnicianProfile technician);
}
