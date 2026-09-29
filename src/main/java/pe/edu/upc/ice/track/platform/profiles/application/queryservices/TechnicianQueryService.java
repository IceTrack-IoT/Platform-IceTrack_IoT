package pe.edu.upc.ice.track.platform.profiles.application.queryservices;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.TechnicianProfile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetAllTechniciansQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetTechnicianByIdQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetTechnicianByUserIdQuery;

import java.util.List;
import java.util.Optional;

/**
 * Technician Query Service
 */
public interface TechnicianQueryService {

  /**
   * Handle Get Technician By Id Query.
   *
   * @param query the query
   * @return the technician profile, or empty when not found
   */
  Optional<TechnicianProfile> handle(GetTechnicianByIdQuery query);

  /**
   * Handle Get Technician By User Id Query.
   *
   * @param query the query
   * @return the technician profile, or empty when not found
   */
  Optional<TechnicianProfile> handle(GetTechnicianByUserIdQuery query);

  /**
   * Handle Get All Technicians Query.
   *
   * @param query the query
   * @return every technician profile
   */
  List<TechnicianProfile> handle(GetAllTechniciansQuery query);
}
