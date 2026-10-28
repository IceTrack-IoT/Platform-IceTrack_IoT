package pe.edu.upc.ice.track.platform.assets.application.internal.queryservices;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.acl.AssetsExternalOwnerService;
import pe.edu.upc.ice.track.platform.assets.application.queryservices.EquipmentQueryService;
import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Equipment;
import pe.edu.upc.ice.track.platform.assets.domain.model.queries.GetEquipmentByIdQuery;
import pe.edu.upc.ice.track.platform.assets.domain.model.queries.GetEquipmentByOwnerQuery;
import pe.edu.upc.ice.track.platform.assets.domain.model.queries.GetEquipmentBySiteQuery;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.CallerRole;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.EquipmentPage;
import pe.edu.upc.ice.track.platform.assets.domain.repositories.EquipmentRepository;
import pe.edu.upc.ice.track.platform.assets.domain.repositories.SiteRepository;

import java.util.List;
import java.util.Optional;

/**
 * Application service that resolves equipment read queries.
 *
 * <p>Every query names its caller by username, which is resolved to an owner through the
 * {@link AssetsExternalOwnerService} ACL before anything is read, so a query only ever sees that owner's
 * units. A caller that holds no owner profile sees no unit at all.</p>
 *
 * <p>Fetching a single unit and listing the units of a site are also open to a technician, who
 * services units of any owner: for a caller acting as one, neither is scoped by ownership. The
 * owner's listing is never opened to a technician.</p>
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
  private final AssetsExternalOwnerService assetsExternalOwnerService;

  /**
   * Creates the query service with its dependencies.
   *
   * @param equipmentRepository  equipment repository port
   * @param siteRepository       site repository port, used to resolve the owner of a unit's site
   * @param assetsExternalOwnerService outbound ACL service, used to resolve the caller to its owner
   */
  public EquipmentQueryServiceImpl(
      EquipmentRepository equipmentRepository,
      SiteRepository siteRepository,
      AssetsExternalOwnerService assetsExternalOwnerService) {
    this.equipmentRepository = equipmentRepository;
    this.siteRepository = siteRepository;
    this.assetsExternalOwnerService = assetsExternalOwnerService;
  }

  // inherited javadoc
  @Override
  @Transactional(readOnly = true)
  public Optional<Equipment> handle(GetEquipmentByIdQuery query) {
    if (query.callerRole() == CallerRole.TECHNICIAN) {
      return equipmentRepository.findById(query.equipmentId());
    }
    return assetsExternalOwnerService.fetchOwnerIdByUsername(query.username())
        .flatMap(ownerId -> equipmentRepository.findById(query.equipmentId())
            .filter(equipment -> isOwnedBy(equipment, ownerId)));
  }

  // inherited javadoc
  @Override
  @Transactional(readOnly = true)
  public List<Equipment> handle(GetEquipmentBySiteQuery query) {
    if (query.callerRole() == CallerRole.TECHNICIAN) {
      return equipmentRepository.findBySiteId(query.siteId());
    }
    var ownerId = assetsExternalOwnerService.fetchOwnerIdByUsername(query.username()).orElse(null);
    if (ownerId == null || !isSiteOwnedBy(query.siteId(), ownerId)) {
      return List.of();
    }
    return equipmentRepository.findBySiteId(query.siteId());
  }

  /**
   * Lists the caller's units, applying the optional filters and the optional paging of the query.
   *
   * <p>Paging is delegated to the repository as a page index and a size, so a dashboard listing
   * never materializes the whole collection to slice it in memory. A query without {@code page} or
   * without {@code size} is answered as a single page holding everything, which keeps the
   * response shape of the basic listing identical for clients that do not paginate.</p>
   *
   * <p>The owner is resolved from the caller before any filter is looked at, and is handed to the
   * repository as a mandatory restriction; the filters of the query can only narrow it.</p>
   *
   * @param query the query naming the caller, the filters and the requested slice
   * @return the requested page of matching units, empty when the caller holds no owner profile
   */
  @Override
  @Transactional(readOnly = true)
  public EquipmentPage handle(GetEquipmentByOwnerQuery query) {
    var ownerId = assetsExternalOwnerService.fetchOwnerIdByUsername(query.username()).orElse(null);
    if (ownerId == null) {
      return query.isPaged()
          ? new EquipmentPage(List.of(), query.page(), query.size(), 0)
          : EquipmentPage.unpaged(List.of());
    }
    if (!query.isPaged()) {
      var firstRows = equipmentRepository.findPageByOwnerId(
          ownerId, query.siteId(), query.status(), query.equipmentType(), query.search(), 0, MAX_UNPAGED_ROWS);
      return EquipmentPage.unpaged(firstRows.content(), firstRows.totalElements());
    }
    return equipmentRepository.findPageByOwnerId(
        ownerId, query.siteId(), query.status(), query.equipmentType(), query.search(),
        query.page(), query.size());
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