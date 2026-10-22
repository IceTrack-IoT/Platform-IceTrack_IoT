package pe.edu.upc.ice.track.platform.assets.application.internal.commandservices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.assets.application.commandservices.EquipmentCommandService;
import pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.acl.ExternalOwnerService;
import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Equipment;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.ChangeStatusCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.ChangeThresholdCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.RegisterEquipmentCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.UpdateEquipmentCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.CallerRole;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.TemperatureThreshold;
import pe.edu.upc.ice.track.platform.assets.domain.repositories.EquipmentRepository;
import pe.edu.upc.ice.track.platform.assets.domain.repositories.SiteRepository;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

/**
 * Equipment Command Service Implementation.
 *
 * <p>Ownership is resolved through the site, never through a column on the unit: a unit belongs to
 * whoever owns the site it is installed at. Every command therefore starts by resolving the
 * caller's username to an owner through the {@link ExternalOwnerService} ACL, then loading the site
 * and confirming it belongs to that owner, which is what makes the "one unit, one owner" rule
 * impossible to bypass. An account that holds no owner profile is answered with
 * {@code ACCESS_DENIED}.</p>
 *
 * <p>The single exception is a status change by a caller acting as a technician, who services
 * units of any owner and is therefore not scoped by ownership. Registering, updating and changing
 * the threshold of a unit carry no role at all and are always owner-scoped.</p>
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
  private final ExternalOwnerService externalOwnerService;

  /**
   * Constructor
   *
   * @param equipmentRepository  The {@link EquipmentRepository} instance
   * @param siteRepository       The {@link SiteRepository} instance
   * @param externalOwnerService The {@link ExternalOwnerService} instance
   */
  public EquipmentCommandServiceImpl(
      EquipmentRepository equipmentRepository,
      SiteRepository siteRepository,
      ExternalOwnerService externalOwnerService) {
    this.equipmentRepository = equipmentRepository;
    this.siteRepository = siteRepository;
    this.externalOwnerService = externalOwnerService;
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<Equipment, ApplicationError> handle(RegisterEquipmentCommand command) {
    return resolveOwnerId(command.username()).flatMap(ownerId -> {
      if (!isSiteOwnedBy(command.siteId(), ownerId)) {
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
        // unique index; the database is the final authority on uid uniqueness. Rethrown so
        // GlobalExceptionHandler answers 409 DUPLICATE_RESOURCE_CONFLICT: the failed insert has
        // already marked the transaction rollback-only, so returning a Result would only surface
        // as an UnexpectedRollbackException.
        throw e;
      } catch (OptimisticLockingFailureException e) {
        // Registering a unit also increments its site's equipment counter, a version-checked
        // write. Rethrown so GlobalExceptionHandler answers 409: the transaction is already
        // rollback-only, and returning a Result would only surface as an
        // UnexpectedRollbackException.
        throw e;
      } catch (Exception e) {
        return Result.failure(ApplicationError.unexpected("Equipment registration", e.getMessage()));
      }
    });
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<Equipment, ApplicationError> handle(UpdateEquipmentCommand command) {
    return reachableEquipment(command.equipmentId(), command.username()).flatMap(equipment -> {
      try {
        equipment.updateInfo(command.name(), command.equipmentType(), command.reminderIntervalDays());
        var savedEquipment = equipmentRepository.save(equipment);
        log.info("Updated equipment {}", savedEquipment.getEquipmentId());
        return Result.success(savedEquipment);
      } catch (IllegalArgumentException e) {
        return Result.failure(ApplicationError.validationError(EQUIPMENT_RESOURCE, e.getMessage()));
      } catch (Exception e) {
        return Result.failure(ApplicationError.unexpected("Equipment update", e.getMessage()));
      }
    });
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<Equipment, ApplicationError> handle(ChangeThresholdCommand command) {
    return reachableEquipment(command.equipmentId(), command.username()).flatMap(equipment -> {
      try {
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
    });
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<Equipment, ApplicationError> handle(ChangeStatusCommand command) {
    // A technician changes the status of the units it services in the field, whatever their
    // owner, so this is the one write not scoped by ownership.
    var reachable = command.callerRole() == CallerRole.TECHNICIAN
        ? existingEquipment(command.equipmentId())
        : reachableEquipment(command.equipmentId(), command.username());
    return reachable.flatMap(equipment -> {
      try {
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
    });
  }

  /**
   * Resolves the caller's username to the owner it acts as.
   *
   * @param username username of the requesting caller
   * @return the owner identifier, or a 403 failure when the caller holds no owner profile
   */
  private Result<Long, ApplicationError> resolveOwnerId(String username) {
    var ownerId = externalOwnerService.fetchOwnerIdByUsername(username);
    return ownerId.<Result<Long, ApplicationError>>map(Result::success).orElseGet(() -> Result.failure(noOwnerProfile()));
  }

  /**
   * Loads a unit the caller may act on as an owner: it must exist, and the site it is installed at
   * must belong to the owner the caller's username resolves to.
   *
   * @param equipmentId the unit identifier
   * @param username    username of the requesting caller
   * @return the unit; a 403 failure when the caller holds no owner profile; or a 404 failure when
   *         the unit does not exist or is owned by somebody else
   */
  private Result<Equipment, ApplicationError> reachableEquipment(Long equipmentId, String username) {
    return resolveOwnerId(username).flatMap(ownerId -> {
      var equipment = equipmentRepository.findById(equipmentId)
          .filter(candidate -> isSiteOwnedBy(candidate.getSiteId(), ownerId));
      return equipment.<Result<Equipment, ApplicationError>>map(Result::success).orElseGet(() -> Result.failure(notReachableEquipment(equipmentId)));
    });
  }

  /**
   * Loads a unit whatever its owner, for the one write a technician may perform.
   *
   * @param equipmentId the unit identifier
   * @return the unit, or a 404 failure when it does not exist
   */
  private Result<Equipment, ApplicationError> existingEquipment(Long equipmentId) {
    var equipment = equipmentRepository.findById(equipmentId);
    return equipment.<Result<Equipment, ApplicationError>>map(Result::success).orElseGet(() -> Result.failure(notReachableEquipment(equipmentId)));
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

  /**
   * Builds the failure reported when the caller's account holds no owner profile.
   *
   * <p>Reported as access denied rather than as a missing unit: the caller is known and the
   * payload may be perfectly fine, there is simply no owner behind it.</p>
   *
   * @return a 403 failure
   */
  private static ApplicationError noOwnerProfile() {
    return ApplicationError.accessDenied(
        "The authenticated account has no owner profile, so it owns no site or equipment");
  }
}
