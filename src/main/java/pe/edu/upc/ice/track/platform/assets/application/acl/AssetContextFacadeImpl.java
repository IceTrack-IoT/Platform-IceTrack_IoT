package pe.edu.upc.ice.track.platform.assets.application.acl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.assets.application.queryservices.EquipmentQueryService;
import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Equipment;
import pe.edu.upc.ice.track.platform.assets.domain.model.queries.GetEquipmentByIdQuery;
import pe.edu.upc.ice.track.platform.assets.domain.repositories.EquipmentRepository;
import pe.edu.upc.ice.track.platform.assets.interfaces.acl.AssetContextFacade;
import pe.edu.upc.ice.track.platform.assets.interfaces.acl.EquipmentThresholdDto;

import java.util.Optional;

/**
 * Default implementation of {@link AssetContextFacade}.
 *
 * <p>The single translation point of the Anti-Corruption Layer: aggregates and their value objects
 * go in, primitives and {@link EquipmentThresholdDto} come out, and no assets domain type ever
 * crosses this boundary. A {@code null} identifier is answered as "not found" instead of being
 * passed down, so a caller that fails to resolve an id cannot provoke a database round trip for
 * nothing.</p>
 */
@Service
@Slf4j
public class AssetContextFacadeImpl implements AssetContextFacade {

  private final EquipmentQueryService equipmentQueryService;
  private final EquipmentRepository equipmentRepository;

  /**
   * Constructor
   *
   * @param equipmentQueryService The {@link EquipmentQueryService} instance, used for the reads
   *                              that do not need to check ownership
   * @param equipmentRepository  The {@link EquipmentRepository} instance, used for the uid lookup,
   *                              which has no owner to scope by
   */
  public AssetContextFacadeImpl(
      EquipmentQueryService equipmentQueryService, EquipmentRepository equipmentRepository) {
    this.equipmentQueryService = equipmentQueryService;
    this.equipmentRepository = equipmentRepository;
  }

  /**
   * Returns the highest acceptable temperature of a unit.
   *
   * <p>See the note on {@link AssetContextFacade#getEquipmentThreshold(Long)}: this resolves to
   * the maximum bound, which is what a "too warm" alarm rule needs.</p>
   *
   * @param equipmentId identifier of the equipment
   * @return the maximum acceptable temperature, or empty when the unit does not exist
   */
  @Override
  @Transactional(readOnly = true)
  public Optional<Double> getEquipmentThreshold(Long equipmentId) {
    return findAnyEquipment(equipmentId).map(equipment -> equipment.getTemperatureThreshold().maxCelsius());
  }

  // inherited javadoc
  @Override
  @Transactional(readOnly = true)
  public Optional<EquipmentThresholdDto> getEquipmentThresholdRange(Long equipmentId) {
    return findAnyEquipment(equipmentId).map(equipment -> new EquipmentThresholdDto(
        equipment.getTemperatureThreshold().minCelsius(),
        equipment.getTemperatureThreshold().maxCelsius()));
  }

  // inherited javadoc
  @Override
  @Transactional(readOnly = true)
  public boolean existsEquipmentForOwner(Long equipmentId, Long ownerId) {
    if (equipmentId == null || ownerId == null) {
      return false;
    }
    return equipmentQueryService.handle(new GetEquipmentByIdQuery(equipmentId, ownerId)).isPresent();
  }

  // inherited javadoc
  @Override
  @Transactional(readOnly = true)
  public Optional<Long> getSiteIdByEquipment(Long equipmentId) {
    return findAnyEquipment(equipmentId).map(equipment -> equipment.getSiteId());
  }

  // inherited javadoc
  @Override
  @Transactional(readOnly = true)
  public Optional<Long> getEquipmentIdByUid(String uid) {
    if (uid == null || uid.isBlank()) {
      return Optional.empty();
    }
    return equipmentRepository.findByUid(uid).map(equipment -> equipment.getEquipmentId());
  }

  /**
   * Loads a unit whatever its owner, which is what a cross-context caller needs: these callers are
   * asking about equipment they legitimately serve, not about one of their own.
   *
   * @param equipmentId identifier of the equipment
   * @return the unit, or empty when the identifier is absent or unknown
   */
  private Optional<Equipment> findAnyEquipment(Long equipmentId) {
    if (equipmentId == null) {
      return Optional.empty();
    }
    return equipmentQueryService.handle(new GetEquipmentByIdQuery(equipmentId, null));
  }
}