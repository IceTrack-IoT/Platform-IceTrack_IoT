package pe.edu.upc.ice.track.platform.reporting.infrastructure.persistence.jpa.adapters;

import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.reporting.domain.model.aggregates.Report;
import pe.edu.upc.ice.track.platform.reporting.domain.repositories.ReportRepository;
import pe.edu.upc.ice.track.platform.reporting.infrastructure.persistence.jpa.assemblers.ReportPersistenceAssembler;
import pe.edu.upc.ice.track.platform.reporting.infrastructure.persistence.jpa.entities.ReportPersistenceEntity;
import pe.edu.upc.ice.track.platform.reporting.infrastructure.persistence.jpa.repositories.ReportPersistenceRepository;

import java.util.List;
import java.util.Optional;

/**
 * Adapter implementing {@link ReportRepository} on top of {@link ReportPersistenceRepository}.
 */
@Repository
public class ReportRepositoryImpl implements ReportRepository {

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
  public List<Report> findByUserId(Long userId) {
    return reportPersistenceRepository.findByUserIdOrderByRequestedAtDesc(userId).stream()
        .map(ReportPersistenceAssembler::toAggregate)
        .toList();
  }
}
