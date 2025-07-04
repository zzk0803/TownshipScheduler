package zzk.townshipscheduler.backend.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.proxy.HibernateProxy;
import zzk.townshipscheduler.backend.OrderEntityScheduleState;
import zzk.townshipscheduler.backend.OrderType;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@NamedEntityGraph(
        name = "order.project-amount-map",
        attributeNodes = {
                @NamedAttributeNode(
                        value = "productAmountMap",
                        keySubgraph = "order.project-amount-map.key"
                )
        },
        subgraphs = {
                @NamedSubgraph(
                        name = "order.project-amount-map.key",
                        attributeNodes = {
                                @NamedAttributeNode(
                                        value = "crawledAsImage",
                                        subgraph = "order.project-amount-map.key.image"
                                )
                        }
                ),
                @NamedSubgraph(
                        name = "order.project-amount-map.key.image",
                        attributeNodes = {
                                @NamedAttributeNode(
                                        value = "imageBytes"
                                )
                        }
                )
        }
)
public class OrderEntity {

    public static final OrderType DEFAULT_ORDER_TYPE = OrderType.HELICOPTER;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private OrderType orderType = OrderType.HELICOPTER;

    private LocalDateTime createdDateTime;

    private boolean bearDeadline;

    private LocalDateTime deadLine;

    @Enumerated(EnumType.ORDINAL)
    private OrderEntityScheduleState billScheduleState = OrderEntityScheduleState.NONE;

    @ManyToOne
    @JoinColumn(name = "player_id",foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private PlayerEntity playerEntity;

    @ElementCollection
    @Column(name = "amount")
    @MapKeyJoinColumn(
            name = "product_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT)
    )
    @MapKeyClass(ProductEntity.class)
    private Map<ProductEntity, Integer> productAmountMap = new HashMap<>();

    private boolean boolFinished;

    private LocalDateTime finishedDateTime;

    public void addItem(ProductEntity productEntity, Integer amount) {
        this.productAmountMap.put(productEntity, amount);
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
                : getClass().hashCode();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy
                ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
                : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        OrderEntity orderEntity = (OrderEntity) o;
        return getId() != null && Objects.equals(getId(), orderEntity.getId());
    }

    public Integer remove(Object key) {
        return productAmountMap.remove(key);
    }

    public int size() {
        return productAmountMap.size();
    }

}
