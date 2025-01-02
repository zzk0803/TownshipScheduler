package zzk.townshipscheduler.backend.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Getter
@Setter
@ToString
@Entity
public class FieldFactoryEntity implements Comparable<FieldFactoryEntity> {

    public static final Comparator<FieldFactoryEntity> COMPARATOR =
            Comparator.comparing(fieldFactoryEntity -> fieldFactoryEntity.getFieldFactoryInfoEntity().getLevel());

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(
            name = "factory_info_id",
            foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT)
    )
    private FieldFactoryInfoEntity fieldFactoryInfoEntity;

    @ManyToOne(optional = false)
    @JoinColumn(
            name = "player_id",
            foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT)
    )
    private PlayerEntity playerEntity;

    @Column
    private int producingLength;

    @Column
    private int reapWindowSize;

    public FieldFactoryEntity() {
        //JPA use
    }


//    @OneToMany(
//            targetEntity = FieldFactoryRecordEntity.class,
//            cascade = CascadeType.ALL,
//            mappedBy = "fieldFactoryEntity"
//    )
//    @ToString.Exclude
//    private Set<FieldFactoryRecordEntity> factoryRecords = new TreeSet<>();

    public FieldFactoryEntity(FieldFactoryInfoEntity fieldFactoryInfoEntity, PlayerEntity playerEntity) {
        this.fieldFactoryInfoEntity = fieldFactoryInfoEntity;
        this.playerEntity = playerEntity;
    }


    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer()
                .getPersistentClass()
                .hashCode() : getClass().hashCode();
    }

//    public void attachFactoryRecord(Collection<FieldFactoryRecordEntity> fieldFactoryRecordEntities) {
//        fieldFactoryRecordEntities.forEach(this::attachFactoryRecord);
//    }
//
//    public void attachFactoryRecord(FieldFactoryRecordEntity fieldFactoryRecordEntity) {
//        fieldFactoryRecordEntity.setFieldFactoryEntity(this);
//        this.factoryRecords.add(fieldFactoryRecordEntity);
//    }

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
        FieldFactoryEntity that = (FieldFactoryEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public int compareTo(FieldFactoryEntity that) {
        return COMPARATOR.compare(this, that);
    }

}
