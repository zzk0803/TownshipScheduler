package zzk.townshipscheduler.backend.scheduling.model;

import com.fasterxml.jackson.annotation.*;
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

    @JsonUnwrapped
    @EqualsAndHashCode.Include
    private Id id;

    @EqualsAndHashCode.Include
    @ToString.Include
    private String name;

    private int level;

    private int gainWhenCompleted = 1;

    @ToString.Include
    @JsonManagedReference
    private SchedulingFactoryInfo requireFactory;

    @JsonIgnore
    @JsonBackReference
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
    public List<BaseSchedulingProducingArrangement> calcFactoryActions() {
        return calcFactoryActions(this);
    }

    @Override
    public List<BaseSchedulingProducingArrangement> calcFactoryActions(IGameActionObject targetObject) {
        switch (getRequireFactory().getProducingStructureType()) {
            case QUEUE -> {
                return List.of(
                        BaseSchedulingProducingArrangement.createProducingArrangementFactoryQueue(
                        targetObject,
                                this
                )
                );
            }

            case SLOT -> {
                return List.of(
                        BaseSchedulingProducingArrangement.createProducingArrangementFactorySlot(
                        targetObject,
                                this
                )
                );
            }

            case null, default -> {
                throw new IllegalStateException(
                        "getRequireFactory().getProducingStructureType() not queue or slot either,so that error"
                );
            }
        }
    }

    @Override
    public Optional<LocalDateTime> optionalDeadline() {
        return Optional.empty();
    }

    @Value
    public static class Id implements Comparable<Id> {

        @JsonProperty("id")
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
