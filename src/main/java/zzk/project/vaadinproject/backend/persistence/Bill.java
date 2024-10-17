package zzk.project.vaadinproject.backend.persistence;

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
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private BillType billType;

    private LocalDateTime createdDateTime;

    private boolean boolDeadLine;

    private LocalDateTime deadLine;

    @ElementCollection
    @Column(name = "amount")
    @MapKeyJoinColumn(
            name = "good_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT)
    )
    private Map<Goods, Integer> productAmountPairs = new HashMap<>();

    private boolean boolFinished;

    private LocalDateTime finishedDateTime;

    public Integer addItem(Goods key, Integer value) {
        return productAmountPairs.put(key, value);
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
        Bill bill = (Bill) o;
        return getId() != null && Objects.equals(getId(), bill.getId());
    }

    public Integer remove(Object key) {
        return productAmountPairs.remove(key);
    }

    public int size() {
        return productAmountPairs.size();
    }

}
