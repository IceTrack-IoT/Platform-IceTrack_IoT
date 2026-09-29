package pe.edu.upc.ice.track.platform.profiles.application.internal.queryservices;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.profiles.application.queryservices.OwnerQueryService;
import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.OwnerProfile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetAllOwnersQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetOwnerByIdQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetOwnerByUserIdQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.repositories.OwnerProfileRepository;

import java.util.List;
import java.util.Optional;

/**
 * Application service that resolves owner read queries.
 */
@Service
public class OwnerQueryServiceImpl implements OwnerQueryService {
  private final OwnerProfileRepository ownerProfileRepository;

  /**
   * Creates the query service with the owner profile repository dependency.
   *
   * @param ownerProfileRepository owner profile repository port
   */
  public OwnerQueryServiceImpl(OwnerProfileRepository ownerProfileRepository) {
    this.ownerProfileRepository = ownerProfileRepository;
  }

  // inherited javadoc
  @Override
  public Optional<OwnerProfile> handle(GetOwnerByIdQuery query) {
    return ownerProfileRepository.findById(query.ownerId());
  }

  // inherited javadoc
  @Override
  public Optional<OwnerProfile> handle(GetOwnerByUserIdQuery query) {
    return ownerProfileRepository.findByUserId(query.userId());
  }

  // inherited javadoc
  @Override
  public List<OwnerProfile> handle(GetAllOwnersQuery query) {
    return ownerProfileRepository.findAll();
  }
}
