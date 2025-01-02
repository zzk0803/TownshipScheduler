package zzk.townshipscheduler.backend.scheduling.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Value;
import zzk.townshipscheduler.backend.persistence.ProductEntityDtoForScheduling;
import zzk.townshipscheduler.backend.persistence.ProductEntityDtoJustId;
import zzk.townshipscheduler.backend.persistence.ProductEntityProjectionJustId;

import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class SchedulingProduct implements SchedulingGameActionObject {

    @EqualsAndHashCode.Include
    private Id id;

    private String name;

    private int level;

    private SchedulingFactoryInfo requireFactory;

    private Set<SchedulingProducingExecutionMode> producingExecutionModeSet;

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
        this.producingExecutionModeSet = executionModeSet;
    }

    @Override
    public List<SchedulingGameAction> getGameActionSet() {
        return List.of(
                new SchedulingGameActionProductProducing(this, this.producingExecutionModeSet),
                new SchedulingGameActionProductStocking(this)
        );
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

    }

}
