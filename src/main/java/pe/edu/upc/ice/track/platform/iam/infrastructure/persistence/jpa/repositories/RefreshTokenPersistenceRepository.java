package pe.edu.upc.ice.track.platform.iam.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.iam.infrastructure.persistence.jpa.entities.RefreshTokenPersistenceEntity;

import java.util.Optional;

/**
 * Spring Data repository for IAM refresh token persistence entities.
 *
 * <p>State changes are Criteria API bulk statements, run through
 * {@link JpaSpecificationExecutor#update} and {@link JpaSpecificationExecutor#delete} with the
 * specifications built by {@code RefreshTokenPersistenceSpecifications}. {@code RefreshTokenRepositoryImpl}
 * flushes pending writes before each of them and clears the persistence context afterward, so no
 * stale managed instance can survive them.</p>
 */
@Repository
public interface RefreshTokenPersistenceRepository
    extends JpaRepository<RefreshTokenPersistenceEntity, Long>,
    JpaSpecificationExecutor<RefreshTokenPersistenceEntity> {

  /**
   * This method is responsible for finding the refresh token by its digest.
   * @param token The token digest.
   * @return The refresh token object.
   */
  Optional<RefreshTokenPersistenceEntity> findByToken(String token);
}
