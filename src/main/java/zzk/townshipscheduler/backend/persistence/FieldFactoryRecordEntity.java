//package zzk.townshipscheduler.backend.persistence;
//
//import jakarta.persistence.*;
//import lombok.*;
//import org.hibernate.proxy.HibernateProxy;
//
//import java.time.LocalDateTime;
//import java.util.Objects;
//
//@Getter
//@Setter
//@ToString
//@Entity
//@NoArgsConstructor
//public class FieldFactoryRecordEntity implements Comparable<FieldFactoryRecordEntity> {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    private LocalDateTime dateTime;
//
//    @OneToOne
//    @JoinColumn(name = "product_id")
//    private ProductEntity producingEntity;
//
//    @ManyToOne
//    @JoinColumn(name = "factory_info_id")
//    private FieldFactoryEntity fieldFactoryEntity;
//
//    @Enumerated(EnumType.STRING)
//    private FactoryAction factoryAction;
//
//    public FieldFactoryRecordEntity(ProductEntity producingEntity, FactoryAction factoryAction) {
//        this.dateTime = LocalDateTime.now();
//        this.producingEntity = producingEntity;
//        this.factoryAction = factoryAction;
//    }
//
//    public FieldFactoryRecordEntity(
//            Long id,
//            LocalDateTime dateTime,
//            ProductEntity producingEntity,
//            FactoryAction factoryAction
//    ) {
//        this.id = id;
//        this.dateTime = dateTime;
//        this.producingEntity = producingEntity;
//        this.factoryAction = factoryAction;
//    }
//
//    @Override
//    public int compareTo(FieldFactoryRecordEntity that) {
//        return this.dateTime.compareTo(that.dateTime);
//    }
//
//
//    public enum FactoryAction {
//        ARRANGE,
//        REAP
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
//        FieldFactoryRecordEntity that = (FieldFactoryRecordEntity) object;
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
