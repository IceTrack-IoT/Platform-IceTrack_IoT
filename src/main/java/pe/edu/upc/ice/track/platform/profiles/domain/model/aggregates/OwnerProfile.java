package pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates;

import lombok.Getter;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.ProfileCreationData;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.ProfileRole;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.Ruc;

import java.util.Objects;

/**
 * Profile of an ice track owner.
 *
 * <p>An owner is always identified by its taxpayer registration number, so an owner profile
 * cannot exist without a {@link Ruc}.</p>
 */
@Getter
public final class OwnerProfile extends Profile {

  private final Ruc ruc;

  /**
   * Creates a new, not yet persisted, owner profile.
   *
   * @param data the validated creation data; required
   * @param ruc  the owner's taxpayer registration number; required
   */
  public OwnerProfile(ProfileCreationData data, Ruc ruc) {
    this(null, data, ruc);
  }

  /**
   * Reconstitutes an owner profile.
   *
   * @param id   the persistence identity, or {@code null} for a profile not yet persisted
   * @param data the validated creation data; required
   * @param ruc  the owner's taxpayer registration number; required
   */
  public OwnerProfile(Long id, ProfileCreationData data, Ruc ruc) {
    super(id, data);
    this.ruc = Objects.requireNonNull(ruc, "ruc must not be null");
  }

  // inherited javadoc
  @Override
  public ProfileRole getRole() {
    return ProfileRole.OWNER;
  }
}
