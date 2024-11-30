//package zzk.townshipscheduler.backend.persistence;
//
//import jakarta.persistence.*;
//import lombok.*;
//import org.hibernate.annotations.DynamicUpdate;
//import org.hibernate.proxy.HibernateProxy;
//
//import java.time.LocalDateTime;
//import java.util.Map;
//import java.util.Objects;
//
//@Getter
//@Setter
//@ToString
//@RequiredArgsConstructor
//@Entity
//@DynamicUpdate
//public class WarehouseStocktakeEntity {
//
//    @Id
//    private Long id;
//
//    @MapsId
//    @OneToOne
//    private WarehouseEntity warehouseEntity;
//
//    @ElementCollection
//    @Column(name = "amount")
//    @MapKeyJoinColumn(
//            name = "product_id",
//            referencedColumnName = "id",
//            foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT)
//    )
//    private Map<ProductEntity, Integer> itemAmountMap;
//
//    private LocalDateTime lateStocktakeDateTime;
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
//        WarehouseStocktakeEntity that = (WarehouseStocktakeEntity) object;
//        return getId() != null && Objects.equals(getId(), that.getId());
//    }
//
//    @Override
//    public final int hashCode() {
//        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer()
//                .getPersistentClass()
//                .hashCode() : getClass().hashCode();
//    }
//
//}
