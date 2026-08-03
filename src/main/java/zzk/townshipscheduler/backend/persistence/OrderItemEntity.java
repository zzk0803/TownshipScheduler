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
        name = "order-item.g.full",
        includeAllAttributes = true,
        attributeNodes = {
                @NamedAttributeNode(
                        value = "productEntity",
                        subgraph = "productEntity.suggraph"
                ),
                @NamedAttributeNode(value = "orderEntity")
        },
        subgraphs = {
                @NamedSubgraph(
                        name = "productEntity.suggraph",
                        attributeNodes = {
                                @NamedAttributeNode(
                                        value = "crawledAsImage",
                                        subgraph = "crawledAsImage.subgraph"
                                ),
                                @NamedAttributeNode(
                                        value = "manufactureInfoEntities"
                                )
                        }
                ),
                @NamedSubgraph(
                        name = "crawledAsImage.subgraph",
                        attributeNodes = {
                                @NamedAttributeNode(value = "imageBytes")
                        }
                )
        }
)
public class OrderItemEntity
        implements Comparable<OrderItemEntity> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private OrderEntity orderEntity;

    @ManyToOne
    @JoinColumn(foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private ProductEntity productEntity;

    private Integer amount;

    @Override
    public int compareTo(OrderItemEntity that) {
        return Comparator.comparing(OrderItemEntity::getOrderEntity, Comparator.nullsFirst(Comparator.comparingLong(OrderEntity::getId)))
                .thenComparing(Comparator.nullsFirst(Comparator.comparingLong(OrderItemEntity::getId)))
                .compare(this, that);
    }

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
        OrderItemEntity that = (OrderItemEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

}
