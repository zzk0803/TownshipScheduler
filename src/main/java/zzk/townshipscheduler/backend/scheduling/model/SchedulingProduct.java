package zzk.townshipscheduler.backend.scheduling.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Value;
import zzk.townshipscheduler.backend.persistence.ProductEntity;
import zzk.townshipscheduler.backend.persistence.select.ProductEntityDtoForScheduling;
import zzk.townshipscheduler.backend.persistence.select.ProductEntityDtoJustId;
import zzk.townshipscheduler.backend.persistence.select.ProductEntityProjectionJustId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class SchedulingProduct implements SchedulingGameActionObject {

    @EqualsAndHashCode.Include
    private Id id;

    private String name;

    private int level;

    private int gainWhenCompleted = 1;

    private SchedulingFactoryInfo requireFactory;

    private Set<SchedulingGameActionExecutionMode> producingExecutionModeSet;

    public SchedulingProduct(
            Id id,
            String name,
            int level,
            SchedulingFactoryInfo requireFactory,
            Set<SchedulingGameActionExecutionMode> executionModeSet
    ) {
        this.id = id;
        this.name = name;
        this.level = level;
        this.requireFactory = requireFactory;
        this.producingExecutionModeSet = executionModeSet;
    }

    @Override
    public String readable() {
        return getName();
    }

    @Override
    public List<SchedulingPlayerWarehouseAction> calcWarehouseActions() {
        return List.of();
    }

    @Override
    public List<SchedulingPlayerFactoryAction> calcFactoryActions() {
        return List.of(
                new SchedulingPlayerFactoryAction(
                        PlayerFactoryActionType.ARRANGE_PRODUCING,
                        this,
                        this
                ),
                new SchedulingPlayerFactoryAction(
                        PlayerFactoryActionType.REAP_AND_STOCK,
                        this,
                        this
                )
        );
    }

    @Override
    public List<SchedulingPlayerFactoryAction> calcFactoryActions(SchedulingGameActionObject targetObject) {
        return List.of(
                new SchedulingPlayerFactoryAction(
                        PlayerFactoryActionType.ARRANGE_PRODUCING,
                        targetObject,
                        this
                ),
                new SchedulingPlayerFactoryAction(
                        PlayerFactoryActionType.REAP_AND_STOCK,
                        targetObject,
                        this
                )
        );
    }

    @Override
    public Set<SchedulingGameActionExecutionMode> getExecutionModeSet() {
        return getProducingExecutionModeSet();
    }

    @Override
    public Optional<LocalDateTime> optionalDeadline() {
        return Optional.empty();
    }

    @Override
    public String toString() {
        return "{\"SchedulingProduct\":{"
               + "        \"id\":" + id
               + ",         \"name\":\"" + name + "\""
               + ",         \"level\":\"" + level + "\""
               + ",         \"requireFactory\":" + requireFactory.getCategoryName()
               + "}}";
    }

    @Value
    public static class Id {

        long value;

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

    }

}
