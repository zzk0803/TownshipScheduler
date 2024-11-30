package zzk.townshipscheduler.backend.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.proxy.HibernateProxy;

import java.util.*;

@Entity
@Getter
@Setter
@ToString
@DynamicUpdate
public class PlayerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer level = 1;

    @OneToOne
    @JoinColumn(name = "account_id")
    @ToString.Exclude
    private AccountEntity account;

    @OneToMany(
            targetEntity = FieldFactoryEntity.class,
            cascade = CascadeType.ALL,
            mappedBy = "playerEntity"
    )
    @ToString.Exclude
    private Set<FieldFactoryEntity> fieldFactoryEntities = new HashSet<>();

    @OneToOne(
            targetEntity = WarehouseEntity.class,
            mappedBy = "playerEntity",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @ToString.Exclude
    private WarehouseEntity warehouseEntity;

    public void attacheWarehouseEntity(WarehouseEntity warehouseEntity) {
        warehouseEntity.setPlayerEntity(this);
        this.setWarehouseEntity(warehouseEntity);
    }

    public boolean addFieldFactory(FieldFactoryEntity fieldFactoryEntity) {
        fieldFactoryEntity.setPlayerEntity(this);
        return fieldFactoryEntities.add(fieldFactoryEntity);
    }

    public boolean addAllFieldFactory(Collection<? extends FieldFactoryEntity> fieldFactoryEntities) {
        return fieldFactoryEntities.stream().map(this::addFieldFactory).anyMatch(boolResult -> !boolResult);
    }

    public boolean removeFieldFactory(FieldFactoryEntity fieldFactoryEntity) {
        fieldFactoryEntity.setPlayerEntity(null);
        return fieldFactoryEntities.remove(fieldFactoryEntity);
    }

    public boolean removeAllFieldFactory(Collection<? extends FieldFactoryEntity> fieldFactoryEntities) {
        return fieldFactoryEntities.stream().map(this::removeFieldFactory).anyMatch(boolResult -> !boolResult);
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
