package pe.edu.upc.ice.track.platform.assets.application.internal.commandservices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.assets.application.commandservices.EquipmentCommandService;
import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Equipment;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.ChangeStatusCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.ChangeThresholdCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.RegisterEquipmentCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.UpdateEquipmentCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.TemperatureThreshold;
import pe.edu.upc.ice.track.platform.assets.domain.repositories.EquipmentRepository;
import pe.edu.upc.ice.track.platform.assets.domain.repositories.SiteRepository;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

import java.util.Optional;

/**
 * Equipment Command Service Implementation.
 *
 * <p>Ownership is resolved through the site, never through a column on the unit: a unit belongs to
 * whoever owns the site it is installed at. Every command therefore starts by loading the site and
 * confirming it belongs to the requesting owner, which is what makes the "one unit, one owner" rule
 * impossible to bypass.</p>
 *
 * <p>A caller that asks for a unit it does not own is told the unit does not exist rather than
 * that it belongs to somebody else: the alternative would turn this endpoint into an oracle for
 * discovering which identifiers are real.</p>
 */
@Service
@Slf4j
public class EquipmentCommandServiceImpl implements EquipmentCommandService {

  private static final String EQUIPMENT_RESOURCE = "Equipment";
  private static final String SITE_RESOURCE = "Site";

  private final EquipmentRepository equipmentRepository;
  private final SiteRepository siteRepository;

  /**
   * Constructor
   *
   * @param equipmentRepository The {@link EquipmentRepository} instance
   * @param siteRepository     The {@link SiteRepository} instance
   */
  public EquipmentCommandServiceImpl(EquipmentRepository equipmentRepository, SiteRepository siteRepository) {
    this.equipmentRepository = equipmentRepository;
    this.siteRepository = siteRepository;
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<Equipment, ApplicationError> handle(RegisterEquipmentCommand command) {
    if (!isSiteOwnedBy(command.siteId(), command.ownerId())) {
      return Result.failure(notReachableSite(command.siteId()));
    }
    if (equipmentRepository.existsByUid(command.uid())) {
      return Result.failure(ApplicationError.conflict(
          EQUIPMENT_RESOURCE,
          "An equipment with uid '%s' already exists".formatted(command.uid())));
    }
    try {
      var equipment = new Equipment(
          null,
          command.siteId(),
          command.uid(),
          command.name(),
          command.equipmentType(),
          new TemperatureThreshold(command.minCelsius(), command.maxCelsius()),
          command.reminderIntervalDays());
      var savedEquipment = equipmentRepository.save(equipment);
      log.info("Registered equipment {} (uid {}) at site {}",
          savedEquipment.getEquipmentId(), savedEquipment.getUid(), command.siteId());
      return Result.success(savedEquipment);
    } catch (IllegalArgumentException e) {
      return Result.failure(ApplicationError.validationError(EQUIPMENT_RESOURCE, e.getMessage()));
    } catch (DataIntegrityViolationException e) {
      // A concurrent request can slip past the existsByUid precheck and still collide on the
      // unique index; the database is the final authority on uid uniqueness, so this is reported
      // the same way as the precheck failure rather than as an unexpected error.
      return Result.failure(ApplicationError.conflict(
          EQUIPMENT_RESOURCE,
          "An equipment with uid '%s' already exists".formatted(command.uid())));
    } catch (Exception e) {
      return Result.failure(ApplicationError.unexpected("Equipment registration", e.getMessage()));
    }
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<Equipment, ApplicationError> handle(UpdateEquipmentCommand command) {
    var reachable = reachableEquipment(command.equipmentId(), command.ownerId());
    if (reachable.isEmpty()) {
      return Result.failure(notReachableEquipment(command.equipmentId()));
    }
    try {
      var equipment = reachable.get();
      equipment.updateInfo(command.name(), command.equipmentType(), command.reminderIntervalDays());
      var savedEquipment = equipmentRepository.save(equipment);
      log.info("Updated equipment {}", savedEquipment.getEquipmentId());
      return Result.success(savedEquipment);
    } catch (IllegalArgumentException e) {
      return Result.failure(ApplicationError.validationError(EQUIPMENT_RESOURCE, e.getMessage()));
    } catch (Exception e) {
      return Result.failure(ApplicationError.unexpected("Equipment update", e.getMessage()));
    }
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<Equipment, ApplicationError> handle(ChangeThresholdCommand command) {
    var reachable = reachableEquipment(command.equipmentId(), command.ownerId());
    if (reachable.isEmpty()) {
      return Result.failure(notReachableEquipment(command.equipmentId()));
    }
    try {
      var equipment = reachable.get();
      // The threshold value object rejects an inverted band by throwing, before the aggregate is
      // modified, so the domain event it registers is never raised for a rejected threshold.
      equipment.changeThreshold(new TemperatureThreshold(command.minCelsius(), command.maxCelsius()));
      var savedEquipment = equipmentRepository.save(equipment);
      log.info("Changed threshold of equipment {} to [{}, {}]C",
          savedEquipment.getEquipmentId(), command.minCelsius(), command.maxCelsius());
      return Result.success(savedEquipment);
    } catch (IllegalArgumentException e) {
      return Result.failure(ApplicationError.validationError(EQUIPMENT_RESOURCE, e.getMessage()));
    } catch (Exception e) {
      return Result.failure(ApplicationError.unexpected("Equipment threshold update", e.getMessage()));
    }
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<Equipment, ApplicationError> handle(ChangeStatusCommand command) {
    var reachable = reachableEquipment(command.equipmentId(), command.ownerId());
    if (reachable.isEmpty()) {
      return Result.failure(notReachableEquipment(command.equipmentId()));
    }
    try {
      var equipment = reachable.get();
      equipment.changeStatus(command.newStatus());
      var savedEquipment = equipmentRepository.save(equipment);
      log.info("Changed status of equipment {} to {}",
          savedEquipment.getEquipmentId(), command.newStatus());
      return Result.success(savedEquipment);
    } catch (IllegalStateException e) {
      return Result.failure(ApplicationError.conflict(EQUIPMENT_RESOURCE, e.getMessage()));
    } catch (IllegalArgumentException e) {
      return Result.failure(ApplicationError.validationError(EQUIPMENT_RESOURCE, e.getMessage()));
    } catch (Exception e) {
      return Result.failure(ApplicationError.unexpected("Equipment status update", e.getMessage()));
    }
  }

  /**
   * Loads a unit only when the site it is installed at belongs to the requesting owner.
   *
   * @param equipmentId the unit identifier
   * @param ownerId     the requesting owner
   * @return the unit, or empty when it does not exist or is owned by somebody else
   */
  private Optional<Equipment> reachableEquipment(Long equipmentId, Long ownerId) {
    return equipmentRepository.findById(equipmentId)
        .filter(equipment -> isSiteOwnedBy(equipment.getSiteId(), ownerId));
  }

  /**
   * Tells whether a site exists and belongs to the given owner.
   *
   * @param siteId  the site identifier
   * @param ownerId the requesting owner
   * @return {@code true} only when both conditions hold
   */
  private boolean isSiteOwnedBy(Long siteId, Long ownerId) {
    return siteRepository.findById(siteId).filter(site -> site.belongsTo(ownerId)).isPresent();
  }

  /**
   * Builds the failure reported when a unit cannot be reached by the requesting owner.
   *
   * @param equipmentId the unit identifier that was requested
   * @return a 404 failure that never reveals whether the unit exists under another owner
   */
  private static ApplicationError notReachableEquipment(Long equipmentId) {
    return ApplicationError.notFound(EQUIPMENT_RESOURCE, String.valueOf(equipmentId));
  }

  /**
   * Builds the failure reported when a unit is registered at a site the caller cannot reach.
   *
   * <p>Reported against the site rather than against the unit, because the site is the reference
   * that failed: it either does not exist, or it belongs to another owner. Both cases answer the
   * same 404 so that registering against somebody else's site teaches nothing.</p>
   *
   * @param siteId the site identifier that was requested
   * @return a 404 failure naming the unreachable site
   */
  private static ApplicationError notReachableSite(Long siteId) {
    return ApplicationError.notFound(SITE_RESOURCE, String.valueOf(siteId));
  }
}