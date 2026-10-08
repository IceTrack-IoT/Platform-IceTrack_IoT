package pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.CardType;
import pe.edu.upc.ice.track.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;

/**
 * JPA persistence entity for a dashboard card, mapped to the {@code dashboard_cards} table.
 *
 * <p>Owned by its {@link DashboardConfigPersistenceEntity}: every row is inserted, updated and
 * deleted through the configuration's {@code cards} collection, which cascades every operation and
 * removes orphans. There is deliberately no Spring Data repository for this entity.</p>
 *
 * <p>The card type is stored by name ({@link EnumType#STRING}) rather than by ordinal, so adding or
 * reordering a {@link CardType} constant never corrupts existing rows.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "dashboard_cards")
@AttributeOverride(name = "id", column = @Column(name = "card_id"))
public class DashboardCardPersistenceEntity extends AuditableAbstractPersistenceEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "dashboard_config_id", nullable = false)
  private DashboardConfigPersistenceEntity dashboardConfig;

  @Enumerated(EnumType.STRING)
  @Column(name = "card_type", nullable = false, length = 30)
  private CardType cardType;

  /**
   * Position of the card on the dashboard. Not named {@code order}: that is a reserved word in
   * JPQL and in the {@code @OrderBy} fragment of the owning collection.
   */
  @Column(name = "card_order", nullable = false)
  private Integer cardOrder;

  @Column(name = "is_visible", nullable = false)
  private boolean visible;

  public DashboardCardPersistenceEntity() {
  }
}
