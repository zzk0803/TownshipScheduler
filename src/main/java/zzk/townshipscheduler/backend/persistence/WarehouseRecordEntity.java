//package zzk.townshipscheduler.backend.persistence;
//
//import jakarta.persistence.*;
//import jakarta.validation.constraints.PositiveOrZero;
//import lombok.Getter;
//import lombok.Setter;
//import lombok.ToString;
//import org.hibernate.proxy.HibernateProxy;
//
//import java.time.LocalDateTime;
//import java.util.Comparator;
//import java.util.Objects;
//
//@Getter
//@Setter
//@ToString
//@Entity
//public class WarehouseRecordEntity implements Comparable<WarehouseRecordEntity> {
//
//    public static final Comparator<WarehouseRecordEntity> COMPARATOR =
//            Comparator.comparing(WarehouseRecordEntity::getDateTime)
//            .reversed();
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    private LocalDateTime dateTime;
//
//    @PositiveOrZero
//    private Integer amount = 0;
//
//    @ManyToOne
//    @JoinColumn(name = "product_id")
//    private ProductEntity productEntity;
//
//    @ManyToOne
//    @JoinColumn(name = "warehouse_id")
//    private WarehouseEntity warehouseEntity;
//
//    @Enumerated(EnumType.STRING)
//    private BarnAction barnAction;
//
//    public WarehouseRecordEntity() {
//    }
//
//    public WarehouseRecordEntity(
//            ProductEntity productEntity,
//            BarnAction barnAction,
//            Integer amount
//    ) {
//        this(LocalDateTime.now(), productEntity, barnAction, amount);
//    }
//
//    public WarehouseRecordEntity(
//            LocalDateTime dateTime,
//            ProductEntity productEntity,
//            BarnAction barnAction,
//            Integer amount
//    ) {
//        this.dateTime = dateTime;
//        this.productEntity = productEntity;
//        this.barnAction = barnAction;
//        this.amount = amount;
//    }
//
//    @Override
//    public int compareTo(WarehouseRecordEntity that) {
//        return COMPARATOR.compare(this, that);
//    }
//
//    @Override
//    public final int hashCode() {
//        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer()
//                .getPersistentClass()
//                .hashCode() : getClass().hashCode();
//    }
//
//    @Override
//    public final boolean equals(Object object) {
//        if (this == object) return true;
//        if (object == null) return false;
//        Class<?> oEffectiveClass = object instanceof HibernateProxy
//                ? ((HibernateProxy) object).getHibernateLazyInitializer().getPersistentClass()
//                : object.getClass();
//        Class<?> thisEffectiveClass = this instanceof HibernateProxy
//                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
//                : this.getClass();
//        if (thisEffectiveClass != oEffectiveClass) return false;
//        WarehouseRecordEntity that = (WarehouseRecordEntity) object;
//        return getId() != null && Objects.equals(getId(), that.getId());
//    }
//
//    public enum BarnAction {
//        SAVE,
//        TAKE
//    }
//
//}
