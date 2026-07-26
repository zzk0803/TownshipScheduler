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
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@NamedEntityGraph(
        name = "order.items",
        attributeNodes = {
                @NamedAttributeNode(
                        value = "orderItemEntities",
                        subgraph = "order.items.product"
                )
        },
        subgraphs = {
                @NamedSubgraph(
                        name = "order.items.product",
                        attributeNodes = {
                                @NamedAttributeNode(
                                        value = "productEntity",
                                        subgraph = "products.g.full"
                                )
                        }
                ),
                @NamedSubgraph(
                        name = "products.g.full",
                        attributeNodes = {
                                @NamedAttributeNode(
                                        value = "crawledAsImage",
                                        subgraph = "products.g.full.image"
                                ),
                                @NamedAttributeNode("fieldFactoryInfo"),
                                @NamedAttributeNode(
                                        value = "manufactureInfoEntities",
                                        subgraph = "products.g.full.manufacture"
                                )
                        }
                ),
                @NamedSubgraph(
                        name = "products.g.full.image",
                        attributeNodes = @NamedAttributeNode("imageBytes")
                ),
                @NamedSubgraph(
                        name = "products.g.full.manufacture",
                        attributeNodes = @NamedAttributeNode("productMaterialsRelations")
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
    @JoinColumn(
            name = "player_id",
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private PlayerEntity playerEntity;

    @OneToMany(
            mappedBy = "orderEntity",
            cascade =CascadeType.ALL,
            orphanRemoval = true
    )
    private SortedSet<OrderItemEntity> orderItemEntities = new TreeSet<>();

    private boolean boolFinished;

    private LocalDateTime finishedDateTime;

    public Map<ProductEntity, Integer> toProductAmountMap() {
        return orderItemEntities.stream().collect(Collectors.toMap(OrderItemEntity::getProductEntity, OrderItemEntity::getAmount));
    }

    public void clearItems() {
        orderItemEntities.forEach(orderItemEntity -> orderItemEntity.setOrderEntity(null));
        orderItemEntities.clear();
    }

    public boolean itemsAddAll(Collection<? extends OrderItemEntity> c) {
        c.forEach(orderItemEntity -> orderItemEntity.setOrderEntity(this));
        return orderItemEntities.addAll(c);
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
        OrderEntity orderEntity = (OrderEntity) o;
        return getId() != null && Objects.equals(getId(), orderEntity.getId());
    }

    public OrderItemEntity itemAdd(ProductEntity productEntity, int amount) {
        AtomicReference<OrderItemEntity> orderItemEntityReference = new AtomicReference<>();
        this.orderItemEntities.stream()
                .filter(orderItemEntity -> orderItemEntity.getProductEntity().equals(productEntity))
                .findFirst()
                .ifPresentOrElse(
                        orderItemEntity -> {
                            orderItemEntityReference.set(orderItemEntity);
                            orderItemEntity.setAmount(amount);
                        },
                        () -> {
                            OrderItemEntity orderItemEntity = new OrderItemEntity();
                            orderItemEntity.setProductEntity(productEntity);
                            orderItemEntity.setAmount(amount);
                            this.itemAdd(orderItemEntity);
                            orderItemEntityReference.set(orderItemEntity);
                        }
                );
        return orderItemEntityReference.get();
    }

    public boolean itemAdd(OrderItemEntity orderItemEntity) {
        orderItemEntity.setOrderEntity(this);
        return orderItemEntities.add(orderItemEntity);
    }

}
