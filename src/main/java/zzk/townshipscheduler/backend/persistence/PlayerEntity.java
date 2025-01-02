package zzk.townshipscheduler.backend.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.proxy.HibernateProxy;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Getter
@Setter
@ToString
@DynamicUpdate
@NamedEntityGraph(
        name = "player.full",
        attributeNodes = {
                @NamedAttributeNode(value = "warehouseEntity", subgraph = "player.warehouse"),
                @NamedAttributeNode(value = "fieldFactoryEntities"),
                @NamedAttributeNode(value = "account")
        },
        subgraphs = {
                @NamedSubgraph(
                        name = "player.warehouse",
                        attributeNodes = {
                                @NamedAttributeNode(value = "productAmountMap")
                        }
                )
        }
)
public class PlayerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer level = 1;

    @OneToOne
    @JoinColumn(name = "account_id")
    @ToString.Exclude
    private AccountEntity account;

    private int fieldAmount;

    @OneToMany(
            targetEntity = FieldFactoryEntity.class,
            cascade = CascadeType.ALL,
            mappedBy = "playerEntity"
    )
    @ToString.Exclude
    private Set<FieldFactoryEntity> fieldFactoryEntities = new HashSet<>();

    @OneToMany(
            targetEntity = OrderEntity.class,
            mappedBy = "playerEntity"
    )
    @ToString.Exclude
    private Set<OrderEntity> orderEntities = new HashSet<>();

    @OneToOne(
            targetEntity = WarehouseEntity.class,
            mappedBy = "playerEntity",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @ToString.Exclude
    private WarehouseEntity warehouseEntity;

    public void addAllOrderEntity(Collection<? extends OrderEntity> orderEntities) {
        orderEntities.forEach(this::attacheOrderEntity);
    }

    public void attacheOrderEntity(OrderEntity orderEntity) {
        orderEntity.setPlayerEntity(this);
        this.addOrderEntity(orderEntity);
    }

    public boolean addOrderEntity(OrderEntity orderEntity) {
        orderEntity.setPlayerEntity(this);
        return orderEntities.add(orderEntity);
    }

    public void removeAllOrderEntity(Collection<? extends OrderEntity> orderEntities) {
        orderEntities.forEach(this::removeOrderEntity);
    }

    public boolean removeOrderEntity(OrderEntity orderEntity) {
        orderEntity.setPlayerEntity(null);
        return orderEntities.remove(orderEntity);
    }

    public boolean addAllFieldFactory(Collection<? extends FieldFactoryEntity> fieldFactoryEntities) {
        return fieldFactoryEntities.stream().map(this::addFieldFactory).anyMatch(boolResult -> !boolResult);
    }

    public boolean addFieldFactory(FieldFactoryEntity fieldFactoryEntity) {
        fieldFactoryEntity.setPlayerEntity(this);
        return fieldFactoryEntities.add(fieldFactoryEntity);
    }

    public boolean removeAllFieldFactory(Collection<? extends FieldFactoryEntity> fieldFactoryEntities) {
        return fieldFactoryEntities.stream().map(this::removeFieldFactory).anyMatch(boolResult -> !boolResult);
    }

    public boolean removeFieldFactory(FieldFactoryEntity fieldFactoryEntity) {
        fieldFactoryEntity.setPlayerEntity(null);
        return fieldFactoryEntities.remove(fieldFactoryEntity);
    }

    public void attacheWarehouseEntity(WarehouseEntity warehouseEntity) {
        warehouseEntity.setPlayerEntity(this);
        this.setWarehouseEntity(warehouseEntity);
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer()
                .getPersistentClass()
                .hashCode() : getClass().hashCode();
    }

    @Override
    public final boolean equals(Object object) {
        if (this == object) return true;
        if (object == null) return false;
        Class<?> oEffectiveClass = object instanceof HibernateProxy
                ? ((HibernateProxy) object).getHibernateLazyInitializer().getPersistentClass()
                : object.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        PlayerEntity that = (PlayerEntity) object;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

}
