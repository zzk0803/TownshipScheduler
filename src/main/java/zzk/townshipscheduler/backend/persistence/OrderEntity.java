package zzk.townshipscheduler.backend.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.proxy.HibernateProxy;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderEntity {

    public static final OrderType DEFAULT_ORDER_TYPE = OrderType.HELICOPTER;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private OrderType orderType=OrderType.HELICOPTER;

    private LocalDateTime createdDateTime;

    private boolean boolDeadLine;

    private LocalDateTime deadLine;

    @Enumerated(EnumType.ORDINAL)
    private BillScheduleState billScheduleState = BillScheduleState.NONE;

    @ElementCollection
    @Column(name = "amount")
    @MapKeyJoinColumn(
            name = "product_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT)
    )
    private Map<ProductEntity, Integer> productAmountPairs = new HashMap<>();

    private boolean boolFinished;

    private LocalDateTime finishedDateTime;

    public void addItem(ProductEntity productEntity, Integer amount) {
        this.productAmountPairs.put(productEntity, amount);
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
        return productAmountPairs.remove(key);
    }

    public int size() {
        return productAmountPairs.size();
    }

    enum BillScheduleState {
        NONE(0, "Just Create"),
        HAS_ARRANGE(1, "Arranged Schedule,Not Finish"),
        HAS_SCHEDULE(2, "Schedule Complete"),
        ;

        private final int code;

        private final String string;

        BillScheduleState(int code, String string) {
            this.code = code;
            this.string = string;
        }
    }

}
