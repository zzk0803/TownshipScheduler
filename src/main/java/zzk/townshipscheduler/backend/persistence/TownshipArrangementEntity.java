package zzk.townshipscheduler.backend.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class TownshipArrangementEntity {

    @Id
    @EqualsAndHashCode.Include
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    private TownshipProblemEntity townshipProblemEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    private OrderEntity order;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    private ProductEntity orderProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    private ProductEntity targetProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    private ProductEntity currentProduct;

    private LocalDateTime assignedDateTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    private FieldFactoryEntity assignedFactoryInstance;

    private LocalDateTime shadowProducingDateTime;

    private LocalDateTime shadowCompletedDateTime;

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer()
                .getPersistentClass()
                .hashCode() : getClass().hashCode();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer()
                .getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer()
                .getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        TownshipArrangementEntity that = (TownshipArrangementEntity) o;
        return getUuid() != null && Objects.equals(getUuid(), that.getUuid());
    }

}
