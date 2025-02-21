package zzk.townshipscheduler.backend.scheduling.model;

import lombok.*;
import zzk.townshipscheduler.backend.persistence.ProductEntity;
import zzk.townshipscheduler.backend.persistence.select.ProductEntityDtoForScheduling;
import zzk.townshipscheduler.backend.persistence.select.ProductEntityDtoJustId;
import zzk.townshipscheduler.backend.persistence.select.ProductEntityProjectionJustId;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public final class SchedulingProduct implements IGameActionObject {

    @EqualsAndHashCode.Include
    private Id id;

    @ToString.Include
    private String name;

    private int level;

    private int gainWhenCompleted = 1;

    @ToString.Include
    private SchedulingFactoryInfo requireFactory;

    private Set<SchedulingProducingExecutionMode> executionModeSet;

    public SchedulingProduct(
            Id id,
            String name,
            int level,
            SchedulingFactoryInfo requireFactory,
            Set<SchedulingProducingExecutionMode> executionModeSet
    ) {
        this.id = id;
        this.name = name;
        this.level = level;
        this.requireFactory = requireFactory;
        this.executionModeSet = executionModeSet;
    }

    @Override
    public Long longIdentity() {
        return getId().getValue();
    }

    @Override
    public String readable() {
        return getName();
    }

//    @Override
//    public List<SchedulingPlayerWarehouseAction> calcWarehouseActions() {
//        return List.of(
//                new SchedulingPlayerWarehouseAction(this)
//        );
//    }
//
//    @Override
//    public List<SchedulingPlayerWarehouseAction> calcWarehouseActions(IGameActionObject targetObject) {
//        return List.of(new SchedulingPlayerWarehouseAction(targetObject));
//    }

    @Override
    public List<SchedulingPlayerFactoryAction> calcFactoryActions() {
        return List.of(
                new SchedulingPlayerFactoryAction(
                        this,
                        this
                )
        );
    }

    @Override
    public List<SchedulingPlayerFactoryAction> calcFactoryActions(IGameActionObject targetObject) {
        return List.of(
                new SchedulingPlayerFactoryAction(
                        targetObject,
                        this
                )
        );
    }

    @Override
    public Optional<LocalDateTime> optionalDeadline() {
        return Optional.empty();
    }

    @Value
    public static class Id implements Comparable<Id> {

        @Getter
        private long value;

        public static Id of(ProductEntityDtoForScheduling productDto) {
            return of(productDto.getId());
        }

        public static Id of(long value) {
            return new Id(value);
        }

        public static Id of(ProductEntityDtoJustId productDto) {
            return of(productDto.getId());
        }

        public static Id of(ProductEntityProjectionJustId productEntityDtoJustId) {
            return of(productEntityDtoJustId.getId());
        }

        public static Id of(ProductEntity productEntity) {
            return of(productEntity.getId());
        }

        @Override
        public String toString() {
            return String.valueOf(getValue());
        }

        @Override
        public int compareTo(Id that) {
            Comparator<Id> comparator = Comparator.comparing(Id::getValue);
            return comparator.compare(this, that);
        }

    }

}
