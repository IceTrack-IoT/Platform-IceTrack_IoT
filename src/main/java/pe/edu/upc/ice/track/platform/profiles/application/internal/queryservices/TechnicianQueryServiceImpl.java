package pe.edu.upc.ice.track.platform.profiles.application.internal.queryservices;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.profiles.application.queryservices.TechnicianQueryService;
import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.TechnicianProfile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetAllTechniciansQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetTechnicianByIdQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetTechnicianByUserIdQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetTechnicianMetricsQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.readmodels.TechnicianMetrics;
import pe.edu.upc.ice.track.platform.profiles.domain.repositories.TechnicianMetricsRepository;
import pe.edu.upc.ice.track.platform.profiles.domain.repositories.TechnicianProfileRepository;

import java.util.List;
import java.util.Optional;

/**
 * Application service that resolves technician read queries.
 */
@Service
public class TechnicianQueryServiceImpl implements TechnicianQueryService {
  private final TechnicianProfileRepository technicianProfileRepository;
  private final TechnicianMetricsRepository technicianMetricsRepository;

  /**
   * Creates the query service with its repository dependencies.
   *
   * @param technicianProfileRepository technician profile repository port
   * @param technicianMetricsRepository technician metrics read model repository port
   */
  public TechnicianQueryServiceImpl(TechnicianProfileRepository technicianProfileRepository,
                                    TechnicianMetricsRepository technicianMetricsRepository) {
    this.technicianProfileRepository = technicianProfileRepository;
    this.technicianMetricsRepository = technicianMetricsRepository;
  }

  // inherited javadoc
  @Override
  public Optional<TechnicianMetrics> handle(GetTechnicianMetricsQuery query) {
    return technicianMetricsRepository.findByTechnicianProfileId(query.technicianId());
  }

  // inherited javadoc
  @Override
  public Optional<TechnicianProfile> handle(GetTechnicianByIdQuery query) {
    return technicianProfileRepository.findById(query.technicianId());
  }

  // inherited javadoc
  @Override
  public Optional<TechnicianProfile> handle(GetTechnicianByUserIdQuery query) {
    return technicianProfileRepository.findByUserId(query.userId());
  }

  // inherited javadoc
  @Override
  public List<TechnicianProfile> handle(GetAllTechniciansQuery query) {
    return technicianProfileRepository.findAll();
  }
}
