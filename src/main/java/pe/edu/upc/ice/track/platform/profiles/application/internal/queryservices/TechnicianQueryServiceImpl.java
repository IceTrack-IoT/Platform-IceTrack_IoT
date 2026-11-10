package pe.edu.upc.ice.track.platform.profiles.application.internal.queryservices;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.profiles.application.queryservices.TechnicianQueryService;
import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.TechnicianProfile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetAllTechniciansQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetPublicTechnicianDirectoryQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetTechnicianByIdQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetTechnicianByUserIdQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetTechnicianMetricsQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.readmodels.TechnicianDirectoryEntry;
import pe.edu.upc.ice.track.platform.profiles.domain.model.readmodels.TechnicianDirectoryPage;
import pe.edu.upc.ice.track.platform.profiles.domain.model.readmodels.TechnicianMetrics;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.TechnicianDirectorySort;
import pe.edu.upc.ice.track.platform.profiles.domain.repositories.TechnicianMetricsRepository;
import pe.edu.upc.ice.track.platform.profiles.domain.repositories.TechnicianProfileRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

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

  /**
   * Answers one page of the public technician directory.
   *
   * <p>Two reads whatever the number of technicians: every profile, then the statistics projected
   * for them. Profiles and statistics are two read models with no association between them, and the
   * filters and the order span both - a rating bound, an order by completed services - so they are
   * joined, filtered, ordered and sliced here rather than in a query. The directory is the
   * platform's catalog of technicians, which the unpaged directory already reads whole.</p>
   *
   * @param query the query naming the filters, the order and the requested page
   * @return the requested page, with the count of every matching technician
   */
  @Override
  public TechnicianDirectoryPage handle(GetPublicTechnicianDirectoryQuery query) {
    var profiles = technicianProfileRepository.findAll();
    var metricsByTechnician = technicianMetricsRepository
        .findByTechnicianProfileIds(profiles.stream().map(TechnicianProfile::getUserProfileId).toList())
        .stream()
        .collect(Collectors.toMap(TechnicianMetrics::getTechnicianProfileId, Function.identity()));
    var matching = profiles.stream()
        .map(profile -> new TechnicianDirectoryEntry(profile, metricsByTechnician.get(profile.getUserProfileId())))
        .filter(entry -> matches(entry, query))
        .sorted(orderOf(query.sort()))
        .toList();
    var offset = (long) query.page() * query.size();
    var content = offset >= matching.size()
        ? List.<TechnicianDirectoryEntry>of()
        : matching.subList((int) offset, (int) Math.min(offset + query.size(), matching.size()));
    return new TechnicianDirectoryPage(content, query.page(), query.size(), matching.size());
  }

  /**
   * Tells whether a technician passes every filter of the query that was supplied.
   *
   * @param entry the technician
   * @param query the query
   * @return {@code true} when no supplied filter excludes the technician
   */
  private static boolean matches(TechnicianDirectoryEntry entry, GetPublicTechnicianDirectoryQuery query) {
    if (query.search() != null) {
      var needle = query.search().toLowerCase(Locale.ROOT);
      if (!entry.fullName().toLowerCase(Locale.ROOT).contains(needle)
          && !entry.specialityName().toLowerCase(Locale.ROOT).contains(needle)) {
        return false;
      }
    }
    if (query.speciality() != null && !entry.specialityName().equalsIgnoreCase(query.speciality())) {
      return false;
    }
    // A technician not rated yet has no rating to compare, so a rating bound leaves it out.
    return query.minRating() == null
        || (entry.averageRating() != null && entry.averageRating() >= query.minRating());
  }

  /**
   * Builds the total order of the directory for a sort.
   *
   * @param sort the requested order
   * @return the comparator; ties fall back to the name and then to the identifier
   */
  private static Comparator<TechnicianDirectoryEntry> orderOf(TechnicianDirectorySort sort) {
    Comparator<TechnicianDirectoryEntry> byName = Comparator
        .comparing((TechnicianDirectoryEntry entry) -> entry.fullName().toLowerCase(Locale.ROOT))
        .thenComparing(entry -> entry.profile().getUserProfileId());
    Comparator<Double> bestRatingFirst = Comparator.nullsLast(Comparator.reverseOrder());
    Comparator<TechnicianDirectoryEntry> byRating =
        Comparator.comparing(TechnicianDirectoryEntry::averageRating, bestRatingFirst);
    Comparator<TechnicianDirectoryEntry> byCompletedServices =
        Comparator.comparingInt(TechnicianDirectoryEntry::completedServices);
    return switch (sort) {
      case NAME -> byName;
      case RATING -> byRating.thenComparing(byName);
      case COMPLETED_SERVICES -> byCompletedServices.reversed().thenComparing(byName);
    };
  }
}
