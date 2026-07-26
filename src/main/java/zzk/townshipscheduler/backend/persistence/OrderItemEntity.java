package zzk.townshipscheduler.backend.persistence;

import jakarta.persistence.*;
import lombok.*;
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
                @NamedAttributeNode(value = "productEntity"),
                @NamedAttributeNode(value = "orderEntity")
        }
)
public class OrderItemEntity implements Comparable<OrderItemEntity>{

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
        return Comparator.comparing(OrderItemEntity::getOrderEntity,Comparator.comparingLong(OrderEntity::getId))
                .thenComparingLong(OrderItemEntity::getId).compare(this,that);
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

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
                : getClass().hashCode();
    }

}
