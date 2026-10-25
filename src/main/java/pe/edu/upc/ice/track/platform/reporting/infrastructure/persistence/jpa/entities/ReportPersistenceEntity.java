package pe.edu.upc.ice.track.platform.reporting.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportFormat;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportStatus;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportType;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA entity for {@link pe.edu.upc.ice.track.platform.reporting.domain.model.aggregates.Report}.
 *
 * <p>{@code ReportFilters}/{@code DateRange} are not mapped as embeddables; their fields are
 * flattened into plain columns ({@code site_id}, {@code equipment_id}, {@code technician_id},
 * {@code range_from}, {@code range_to}) and reassembled by {@code ReportPersistenceAssembler},
 * mirroring how other aggregates in this codebase keep their persistence entities free of domain
 * value-object types.</p>
 */
@Entity
@Table(name = "reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ReportPersistenceEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 150)
  private String title;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private ReportType type;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private ReportFormat format;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 15)
  private ReportStatus status;

  @Lob
  @Column(name = "result_payload")
  private String resultPayload;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "site_id")
  private Long siteId;

  @Column(name = "equipment_id")
  private Long equipmentId;

  @Column(name = "technician_id")
  private Long technicianId;

  @Column(name = "range_from", nullable = false)
  private LocalDateTime rangeFrom;

  @Column(name = "range_to", nullable = false)
  private LocalDateTime rangeTo;

  @Column(name = "requested_at", nullable = false)
  private LocalDateTime requestedAt;

  @Column(name = "completed_at")
  private LocalDate completedAt;

  @Column(name = "file_key", length = 255)
  private String fileKey;

  @Column(name = "file_content_type", length = 100)
  private String fileContentType;

  @Column(name = "file_size_bytes")
  private Long fileSizeBytes;
}
