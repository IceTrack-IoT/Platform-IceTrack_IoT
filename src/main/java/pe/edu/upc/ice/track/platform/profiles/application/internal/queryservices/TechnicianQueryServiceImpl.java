package pe.edu.upc.ice.track.platform.profiles.application.internal.queryservices;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.profiles.application.queryservices.TechnicianQueryService;
import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.TechnicianProfile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetAllTechniciansQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetTechnicianByIdQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetTechnicianByUserIdQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.repositories.TechnicianProfileRepository;

import java.util.List;
import java.util.Optional;

/**
 * Application service that resolves technician read queries.
 */
@Service
public class TechnicianQueryServiceImpl implements TechnicianQueryService {
  private final TechnicianProfileRepository technicianProfileRepository;

  /**
   * Creates the query service with the technician profile repository dependency.
   *
   * @param technicianProfileRepository technician profile repository port
   */
  public TechnicianQueryServiceImpl(TechnicianProfileRepository technicianProfileRepository) {
    this.technicianProfileRepository = technicianProfileRepository;
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
