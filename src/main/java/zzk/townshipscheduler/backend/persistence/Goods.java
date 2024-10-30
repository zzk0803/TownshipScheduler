package zzk.townshipscheduler.backend.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@DynamicInsert
public class Goods {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String category;

    private Integer level;

    private Integer cost;

    private Integer sellPrice;

    private Integer xp;

    private Integer dealerValue;

    private Integer helpValue;

    private String bomString;

    private String durationString;

    @Lob
    private byte[] imageBytes;

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
        Class<?> thisEffectiveClass = this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Goods goods = (Goods) o;
        return getId() != null && Objects.equals(getId(), goods.getId());
    }

    @Override
    public String toString() {
        return "Goods{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", category='" + category + '\'' +
               ", level='" + level + '\'' +
               ", cost='" + cost + '\'' +
               ", sellPrice='" + sellPrice + '\'' +
               ", xp='" + xp + '\'' +
               ", dealerValue='" + dealerValue + '\'' +
               ", helpValue='" + helpValue + '\'' +
               '}';
    }

}
