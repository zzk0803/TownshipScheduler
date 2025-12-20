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
public class TownshipProblemEntity {

    @Id
    @EqualsAndHashCode.Include
    private String uuid;

    @OneToMany(
            targetEntity = TownshipArrangementEntity.class,
            mappedBy = "townshipProblemEntity",
            orphanRemoval = true,
            cascade = {CascadeType.PERSIST, CascadeType.REMOVE}
    )
    @Builder.Default
    @ToString.Exclude
    private Set<TownshipArrangementEntity> townshipArrangementEntitySet = new HashSet<>();

    private LocalDateTime workCalendarStart;

    private LocalDateTime workCalendarEnd;

    @Builder.Default
    private int dateTimeSlotSizeInMinute = 60;

    @ManyToOne(targetEntity = PlayerEntity.class, fetch = FetchType.LAZY)
    @ToString.Exclude
    private PlayerEntity player;

    @Builder.Default
    @ToString.Exclude
    @OneToMany(targetEntity = OrderEntity.class)
    @JoinTable(name = "jointable_problem_order")
    private Set<OrderEntity> orderEntitySet = new HashSet<>();

    private LocalTime sleepStartPickerValue;

    private LocalTime sleepEndPickerValue;

    private String scoreReadable;

    public Set<TownshipArrangementEntity> getTownshipArrangementEntitySet() {
        return Collections.synchronizedSet(townshipArrangementEntitySet);
    }

    public void setTownshipArrangementEntitySet(Set<TownshipArrangementEntity> townshipArrangementEntitySet) {
        if (!this.townshipArrangementEntitySet.isEmpty()) {
            this.townshipArrangementEntitySet.forEach(this::detachArrangementEntity);
        }
        this.townshipArrangementEntitySet = townshipArrangementEntitySet;
        this.townshipArrangementEntitySet.forEach(this::attachArrangementEntity);
    }

    public void attachArrangementEntity(TownshipArrangementEntity townshipArrangementEntity) {
        Objects.requireNonNull(townshipArrangementEntity);
        this.townshipArrangementEntitySet.add(townshipArrangementEntity);
        townshipArrangementEntity.setTownshipProblemEntity(this);
    }

    public void detachArrangementEntity(TownshipArrangementEntity townshipArrangementEntity) {
        Objects.requireNonNull(townshipArrangementEntity);
        this.townshipArrangementEntitySet.remove(townshipArrangementEntity);
        townshipArrangementEntity.setTownshipProblemEntity(null);
    }

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
