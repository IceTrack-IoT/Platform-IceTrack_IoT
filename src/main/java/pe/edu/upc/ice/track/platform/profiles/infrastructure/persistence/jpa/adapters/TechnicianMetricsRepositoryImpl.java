package pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.adapters;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.profiles.domain.model.readmodels.ProjectedReviewScore;
import pe.edu.upc.ice.track.platform.profiles.domain.model.readmodels.TechnicianMetrics;
import pe.edu.upc.ice.track.platform.profiles.domain.repositories.TechnicianMetricsRepository;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.entities.TechnicianMetricsCompletedServicePersistenceEntity;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.entities.TechnicianMetricsProjectionPersistenceEntity;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.entities.TechnicianMetricsReviewScorePersistenceEntity;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.repositories.TechnicianMetricsCompletedServicePersistenceRepository;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.repositories.TechnicianMetricsProjectionPersistenceRepository;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.repositories.TechnicianMetricsReviewScorePersistenceRepository;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Adapter fulfilling {@link TechnicianMetricsRepository} on top of Spring Data JPA.
 *
 * <p>Metrics already stored are saved by updating the managed entity after checking it is still the
 * version they were read at, exactly like the service request adapter does; a concurrent change
 * surfaces as an {@link OptimisticLockingFailureException}.</p>
 */
@Repository
public class TechnicianMetricsRepositoryImpl implements TechnicianMetricsRepository {

  private final TechnicianMetricsProjectionPersistenceRepository metricsRepository;
  private final TechnicianMetricsCompletedServicePersistenceRepository completedServiceRepository;
  private final TechnicianMetricsReviewScorePersistenceRepository reviewScoreRepository;

  public TechnicianMetricsRepositoryImpl(
      TechnicianMetricsProjectionPersistenceRepository metricsRepository,
      TechnicianMetricsCompletedServicePersistenceRepository completedServiceRepository,
      TechnicianMetricsReviewScorePersistenceRepository reviewScoreRepository) {
    this.metricsRepository = metricsRepository;
    this.completedServiceRepository = completedServiceRepository;
    this.reviewScoreRepository = reviewScoreRepository;
  }

  @Override
  public Optional<TechnicianMetrics> findByTechnicianProfileId(Long technicianProfileId) {
    if (technicianProfileId == null) {
      return Optional.empty();
    }
    return metricsRepository.findById(technicianProfileId).map(TechnicianMetricsRepositoryImpl::toDomain);
  }

  @Override
  public List<TechnicianMetrics> findByTechnicianProfileIds(Collection<Long> technicianProfileIds) {
    if (technicianProfileIds == null || technicianProfileIds.isEmpty()) {
      return List.of();
    }
    return metricsRepository.findAllById(technicianProfileIds).stream()
        .map(TechnicianMetricsRepositoryImpl::toDomain)
        .toList();
  }

  @Override
  public TechnicianMetrics save(TechnicianMetrics metrics) {
    var entity = metrics.getVersion() == null
        ? newEntity(metrics.getTechnicianProfileId())
        : managedEntityOf(metrics);
    entity.setCompletedServicesCount(metrics.getCompletedServicesCount());
    entity.setRatingSum(metrics.getRatingSum());
    entity.setRatingCount(metrics.getRatingCount());
    entity.setAverageRating(metrics.getAverageRating());
    return toDomain(metricsRepository.saveAndFlush(entity));
  }

  @Override
  public boolean registerCompletedService(Long serviceRequestId, Long technicianProfileId) {
    if (completedServiceRepository.existsById(serviceRequestId)) {
      return false;
    }
    var entity = new TechnicianMetricsCompletedServicePersistenceEntity();
    entity.setServiceRequestId(serviceRequestId);
    entity.setTechnicianProfileId(technicianProfileId);
    completedServiceRepository.save(entity);
    return true;
  }

  @Override
  public Optional<ProjectedReviewScore> findReviewScore(Long reviewId) {
    if (reviewId == null) {
      return Optional.empty();
    }
    return reviewScoreRepository.findById(reviewId)
        .map(entity -> new ProjectedReviewScore(
            entity.getReviewId(), entity.getTechnicianProfileId(), entity.getScore(), entity.getScoredAt()));
  }

  @Override
  public void saveReviewScore(ProjectedReviewScore score) {
    var entity = reviewScoreRepository.findById(score.reviewId())
        .orElseGet(TechnicianMetricsReviewScorePersistenceEntity::new);
    entity.setReviewId(score.reviewId());
    entity.setTechnicianProfileId(score.technicianProfileId());
    entity.setScore(score.score());
    entity.setScoredAt(score.scoredAt());
    reviewScoreRepository.save(entity);
  }

  private TechnicianMetricsProjectionPersistenceEntity managedEntityOf(TechnicianMetrics metrics) {
    var entity = metricsRepository.findById(metrics.getTechnicianProfileId())
        .orElseThrow(() -> new OptimisticLockingFailureException(
            "Metrics of technician %s were deleted after they were read".formatted(metrics.getTechnicianProfileId())));
    if (!Objects.equals(entity.getVersion(), metrics.getVersion())) {
      throw new OptimisticLockingFailureException(
          "Metrics of technician %s were modified after they were read".formatted(metrics.getTechnicianProfileId()));
    }
    return entity;
  }

  private static TechnicianMetricsProjectionPersistenceEntity newEntity(Long technicianProfileId) {
    var entity = new TechnicianMetricsProjectionPersistenceEntity();
    entity.setTechnicianProfileId(technicianProfileId);
    return entity;
  }

  private static TechnicianMetrics toDomain(TechnicianMetricsProjectionPersistenceEntity entity) {
    return new TechnicianMetrics(
        entity.getTechnicianProfileId(),
        entity.getCompletedServicesCount(),
        entity.getRatingSum(),
        entity.getRatingCount(),
        entity.getVersion());
  }
}
