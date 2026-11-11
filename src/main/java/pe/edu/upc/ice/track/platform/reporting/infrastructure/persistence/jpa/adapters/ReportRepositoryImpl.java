package pe.edu.upc.ice.track.platform.reporting.infrastructure.persistence.jpa.adapters;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.reporting.domain.model.aggregates.Report;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportListFilter;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportPage;
import pe.edu.upc.ice.track.platform.reporting.domain.repositories.ReportRepository;
import pe.edu.upc.ice.track.platform.reporting.infrastructure.persistence.jpa.assemblers.ReportPersistenceAssembler;
import pe.edu.upc.ice.track.platform.reporting.infrastructure.persistence.jpa.entities.ReportPersistenceEntity;
import pe.edu.upc.ice.track.platform.reporting.infrastructure.persistence.jpa.repositories.ReportPersistenceRepository;
import pe.edu.upc.ice.track.platform.reporting.infrastructure.persistence.jpa.specifications.ReportPersistenceSpecifications;

import java.util.List;
import java.util.Optional;

/**
 * Adapter implementing {@link ReportRepository} on top of {@link ReportPersistenceRepository}.
 */
@Repository
public class ReportRepositoryImpl implements ReportRepository {

  /**
   * Most recently requested first, then by identifier, because a listing paginated without a total
   * order silently drops and repeats rows from one page to the next.
   */
  private static final Sort LISTING_ORDER = Sort.by(Sort.Order.desc("requestedAt"), Sort.Order.desc("id"));

  private final ReportPersistenceRepository reportPersistenceRepository;

  public ReportRepositoryImpl(ReportPersistenceRepository reportPersistenceRepository) {
    this.reportPersistenceRepository = reportPersistenceRepository;
  }

  @Override
  public Report save(Report report) {
    ReportPersistenceEntity entity = ReportPersistenceAssembler.toEntity(report);
    ReportPersistenceEntity savedEntity = reportPersistenceRepository.save(entity);
    return ReportPersistenceAssembler.toAggregate(savedEntity);
  }

  @Override
  public Optional<Report> findById(Long reportId) {
    return reportPersistenceRepository.findById(reportId).map(ReportPersistenceAssembler::toAggregate);
  }

  @Override
  public List<Report> findByUserId(Long userId, ReportListFilter filter) {
    if (userId == null) {
      return List.of();
    }
    return reportPersistenceRepository.findAll(ReportPersistenceSpecifications.requestedBy(userId, filter), LISTING_ORDER)
        .stream()
        .map(ReportPersistenceAssembler::toAggregate)
        .toList();
  }

  @Override
  public ReportPage findPageByUserId(Long userId, ReportListFilter filter, int page, int size) {
    if (userId == null) {
      return ReportPage.empty(page, size);
    }
    var specification = ReportPersistenceSpecifications.requestedBy(userId, filter);
    // An offset beyond Integer.MAX_VALUE cannot be expressed; such a page is necessarily empty.
    if ((long) page * size > Integer.MAX_VALUE) {
      return new ReportPage(List.of(), page, size, reportPersistenceRepository.count(specification));
    }
    var slice = reportPersistenceRepository.findAll(specification, PageRequest.of(page, size, LISTING_ORDER));
    var content = slice.getContent().stream()
        .map(ReportPersistenceAssembler::toAggregate)
        .toList();
    return new ReportPage(content, page, size, slice.getTotalElements());
  }

  @Override
  public void deleteById(Long reportId) {
    if (reportId == null) {
      return;
    }
    reportPersistenceRepository.deleteById(reportId);
  }
}
