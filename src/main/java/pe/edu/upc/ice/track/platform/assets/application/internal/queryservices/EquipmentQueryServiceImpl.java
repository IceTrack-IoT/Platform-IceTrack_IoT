package pe.edu.upc.ice.track.platform.assets.application.internal.queryservices;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
 * <p>Two owner rules live here. A query that names an owner only ever sees that owner's units, and
 * a query that names none deliberately sees any unit, which is the shape the ACL facade uses to
 * answer the other bounded contexts.</p>
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

  /**
   * Creates the query service with its repository dependencies.
   *
   * @param equipmentRepository equipment repository port
   * @param siteRepository     site repository port, used to resolve the owner of a unit's site
   */
  public EquipmentQueryServiceImpl(EquipmentRepository equipmentRepository, SiteRepository siteRepository) {
    this.equipmentRepository = equipmentRepository;
    this.siteRepository = siteRepository;
  }

  // inherited javadoc
  @Override
  @Transactional(readOnly = true)
  public Optional<Equipment> handle(GetEquipmentByIdQuery query) {
    return equipmentRepository.findById(query.equipmentId())
        .filter(equipment -> query.ownerId() == null || isOwnedBy(equipment, query.ownerId()));
  }

  // inherited javadoc
  @Override
  @Transactional(readOnly = true)
  public List<Equipment> handle(GetEquipmentBySiteQuery query) {
    if (!isSiteOwnedBy(query.siteId(), query.ownerId())) {
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
   * @param query the query naming the owner, the filters and the requested slice
   * @return the requested page of matching units
   */
  @Override
  @Transactional(readOnly = true)
  public EquipmentPage handle(GetEquipmentByOwnerQuery query) {
    var ownerId = query.ownerId();
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