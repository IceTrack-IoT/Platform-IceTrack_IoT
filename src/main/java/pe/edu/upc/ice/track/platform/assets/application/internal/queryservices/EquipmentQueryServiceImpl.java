package pe.edu.upc.ice.track.platform.assets.application.internal.queryservices;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.acl.ExternalOwnerService;
import pe.edu.upc.ice.track.platform.assets.application.queryservices.EquipmentQueryService;
import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Equipment;
import pe.edu.upc.ice.track.platform.assets.domain.model.queries.GetEquipmentByIdQuery;
import pe.edu.upc.ice.track.platform.assets.domain.model.queries.GetEquipmentByOwnerQuery;
import pe.edu.upc.ice.track.platform.assets.domain.model.queries.GetEquipmentBySiteQuery;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.EquipmentPage;
import pe.edu.upc.ice.track.platform.assets.domain.repositories.EquipmentRepository;
import pe.edu.upc.ice.track.platform.assets.domain.repositories.SiteRepository;

import java.util.List;
import java.util.Optional;

/**
 * Application service that resolves equipment read queries.
 *
 * <p>Every query names its caller by username, which is resolved to an owner through the
 * {@link ExternalOwnerService} ACL before anything is read, so a query only ever sees that owner's
 * units. A caller that holds no owner profile sees no unit at all.</p>
 */
@Service
public class EquipmentQueryServiceImpl implements EquipmentQueryService {

  /**
   * Maximum number of rows a listing returns when the client asks for no page.
   *
   * <p>An unpaged listing is the simple case, but it is not unbounded: an owner with more units
   * than this is a reporting problem rather than a listing problem, and the count that comes back
   * alongside the rows is what tells the client there is more to ask for.</p>
   */
  private static final int MAX_UNPAGED_ROWS = 500;

  private final EquipmentRepository equipmentRepository;
  private final SiteRepository siteRepository;
  private final ExternalOwnerService externalOwnerService;

  /**
   * Creates the query service with its dependencies.
   *
   * @param equipmentRepository  equipment repository port
   * @param siteRepository       site repository port, used to resolve the owner of a unit's site
   * @param externalOwnerService outbound ACL service, used to resolve the caller to its owner
   */
  public EquipmentQueryServiceImpl(
      EquipmentRepository equipmentRepository,
      SiteRepository siteRepository,
      ExternalOwnerService externalOwnerService) {
    this.equipmentRepository = equipmentRepository;
    this.siteRepository = siteRepository;
    this.externalOwnerService = externalOwnerService;
  }

  // inherited javadoc
  @Override
  @Transactional(readOnly = true)
  public Optional<Equipment> handle(GetEquipmentByIdQuery query) {
    return externalOwnerService.fetchOwnerIdByUsername(query.username())
        .flatMap(ownerId -> equipmentRepository.findById(query.equipmentId())
            .filter(equipment -> isOwnedBy(equipment, ownerId)));
  }

  // inherited javadoc
  @Override
  @Transactional(readOnly = true)
  public List<Equipment> handle(GetEquipmentBySiteQuery query) {
    var ownerId = externalOwnerService.fetchOwnerIdByUsername(query.username()).orElse(null);
    if (ownerId == null || !isSiteOwnedBy(query.siteId(), ownerId)) {
      return List.of();
    }
    return equipmentRepository.findBySiteId(query.siteId());
  }

  /**
   * Lists the caller's units, applying the optional filters and the optional paging of the query.
   *
   * <p>Paging is delegated to the repository as an offset and a limit, so a dashboard listing
   * never materialises the whole collection to slice it in memory. A query without {@code page} or
   * without {@code size} is answered as a single page holding everything, which keeps the
   * response shape of the basic listing identical for clients that do not paginate.</p>
   *
   * @param query the query naming the caller, the filters and the requested slice
   * @return the requested page of matching units, empty when the caller holds no owner profile
   */
  @Override
  @Transactional(readOnly = true)
  public EquipmentPage handle(GetEquipmentByOwnerQuery query) {
    var ownerId = externalOwnerService.fetchOwnerIdByUsername(query.username()).orElse(null);
    if (ownerId == null) {
      return query.isPaged()
          ? new EquipmentPage(List.of(), query.page(), query.size(), 0)
          : EquipmentPage.unpaged(List.of());
    }
    var siteId = query.siteId();
    var status = query.status();
    var equipmentType = query.equipmentType();
    if (!query.isPaged()) {
      var all = equipmentRepository.findByOwnerId(ownerId, siteId, status, equipmentType, 0, MAX_UNPAGED_ROWS);
      var total = equipmentRepository.countByOwnerId(ownerId, siteId, status, equipmentType);
      return EquipmentPage.unpaged(all, total);
    }
    var page = query.page();
    var size = query.size();
    var content = equipmentRepository.findByOwnerId(ownerId, siteId, status, equipmentType, page * size, size);
    var total = equipmentRepository.countByOwnerId(ownerId, siteId, status, equipmentType);
    return new EquipmentPage(content, page, size, total);
  }

  /**
   * Tells whether a unit belongs to the given owner, reached through the site it is installed at.
   *
   * @param equipment the unit to test
   * @param ownerId   the candidate owner
   * @return {@code true} when the unit's site belongs to that owner
   */
  private boolean isOwnedBy(Equipment equipment, Long ownerId) {
    return siteRepository.findById(equipment.getSiteId())
        .filter(site -> site.belongsTo(ownerId))
        .isPresent();
  }

  /**
   * Tells whether a site exists and belongs to the given owner.
   *
   * @param siteId  the site identifier
   * @param ownerId the candidate owner
   * @return {@code true} when both conditions hold
   */
  private boolean isSiteOwnedBy(Long siteId, Long ownerId) {
    return siteRepository.findById(siteId).filter(site -> site.belongsTo(ownerId)).isPresent();
  }
}