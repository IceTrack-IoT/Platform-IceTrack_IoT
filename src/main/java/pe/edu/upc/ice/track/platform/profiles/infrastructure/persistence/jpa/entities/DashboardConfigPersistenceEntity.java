package pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.embeddables.TemperatureRangePersistenceEmbeddable;
import pe.edu.upc.ice.track.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * JPA persistence entity for a dashboard configuration, mapped to the {@code dashboard_configs}
 * table.
 *
 * <p>The {@code cards} collection cascades every operation to its {@link DashboardCardPersistenceEntity
 * cards} and removes orphans, so a card dropped from the collection is deleted, and a card can never
 * outlive - nor be saved apart from - its configuration. The collection itself is never handed out
 * for modification: cards are added and removed through {@link #addCard} and {@link #removeCardsIf},
 * which keep both sides of the association consistent.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "dashboard_configs")
@AttributeOverride(name = "id", column = @Column(name = "dashboard_config_id"))
public class DashboardConfigPersistenceEntity extends AuditableAbstractPersistenceEntity {

  /**
   * Identifier of the IAM account this configuration belongs to. Stored as a plain column, never as
   * a foreign key association: the {@code profiles} context must not reference an IAM entity. It is
   * unique, so an account can never hold two configurations.
   */
  @Column(name = "user_id", nullable = false, unique = true)
  private Long userId;

  /**
   * Identifier of the {@code assets} site the dashboard opens on. A plain column, never a foreign
   * key association: the {@code profiles} context must not reference an {@code assets} entity.
   */
  @Column(name = "default_site_id", nullable = false)
  private Long defaultSiteId;

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "min", column = @Column(name = "temperature_range_min", nullable = false)),
      @AttributeOverride(name = "max", column = @Column(name = "temperature_range_max", nullable = false)),
      @AttributeOverride(name = "unit", column = @Column(name = "temperature_range_unit", nullable = false, length = 1)),
      @AttributeOverride(name = "label", column = @Column(name = "temperature_range_label", nullable = false))})
  private TemperatureRangePersistenceEmbeddable defaultTemperatureRange;

  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  @OneToMany(mappedBy = "dashboardConfig", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @OrderBy("cardOrder ASC")
  private List<DashboardCardPersistenceEntity> cards = new ArrayList<>();

  public DashboardConfigPersistenceEntity() {
  }

  /**
   * Returns the cards of this configuration.
   *
   * @return a read-only view of the cards
   */
  public List<DashboardCardPersistenceEntity> getCards() {
    return Collections.unmodifiableList(cards);
  }

  /**
   * Attaches a card to this configuration; it is inserted when the configuration is saved.
   *
   * @param card the card to attach
   */
  public void addCard(DashboardCardPersistenceEntity card) {
    card.setDashboardConfig(this);
    cards.add(card);
  }

  /**
   * Detaches the matching cards from this configuration; orphan removal deletes them when the
   * configuration is saved.
   *
   * @param filter selects the cards to detach
   */
  public void removeCardsIf(Predicate<DashboardCardPersistenceEntity> filter) {
    cards.removeIf(filter);
  }
}
