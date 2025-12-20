package zzk.townshipscheduler.backend.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
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
@NamedEntityGraph(
        name = "problem.g.full",
        includeAllAttributes = true,
        attributeNodes = {
                @NamedAttributeNode(value = "problemSerialized")
        }
)
public class TownshipProblemEntity {

    @Id
    @EqualsAndHashCode.Include
    private String uuid;

    @Lob
    private byte[] problemSerialized;

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
        TownshipProblemEntity that = (TownshipProblemEntity) o;
        return getUuid() != null && Objects.equals(getUuid(), that.getUuid());
    }

}
