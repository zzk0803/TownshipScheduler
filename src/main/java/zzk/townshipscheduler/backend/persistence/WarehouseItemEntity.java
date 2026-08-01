package zzk.townshipscheduler.backend.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.proxy.HibernateProxy;

import java.util.Comparator;
import java.util.Objects;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@NamedEntityGraph(
        name = "warehouse-item.g.full",
        includeAllAttributes = true,
        attributeNodes = {
                @NamedAttributeNode(
                        value = "product",
                        subgraph = "productEntity.suggraph"
                ),
                @NamedAttributeNode(value = "warehouse")
        },
        subgraphs = {
                @NamedSubgraph(
                        name = "productEntity.suggraph",
                        attributeNodes = {
                                @NamedAttributeNode(
                                        value = "crawledAsImage",
                                        subgraph = "wikiCrawledEntity.subgraph"
                                ),
                                @NamedAttributeNode(
                                        value = "manufactureInfoEntities"
                                )
                        }
                ),
                @NamedSubgraph(
                        name = "wikiCrawledEntity.subgraph",
                        attributeNodes = {
                                @NamedAttributeNode(value = "imageBytes")
                        }
                )
        }
)
public class WarehouseItemEntity
        implements Comparable<WarehouseItemEntity> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private WarehouseEntity warehouse;

    @ManyToOne
    @JoinColumn(foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private ProductEntity product;

    private Integer amount;

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
                : getClass().hashCode();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null)
            return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy
                ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
                : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                : this.getClass();
        if (thisEffectiveClass != oEffectiveClass)
            return false;
        WarehouseItemEntity that = (WarehouseItemEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public int compareTo(WarehouseItemEntity that) {
        return Comparator.comparing(WarehouseItemEntity::getWarehouse, Comparator.nullsFirst(Comparator.comparingLong(WarehouseEntity::getId)))
                .thenComparing(Comparator.nullsFirst(Comparator.comparingLong(WarehouseItemEntity::getId)))
                .compare(this, that);
    }

}
