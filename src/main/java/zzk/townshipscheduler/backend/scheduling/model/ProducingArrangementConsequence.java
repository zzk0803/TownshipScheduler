package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.solution.cloner.DeepPlanningClone;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.IntFunction;

@Data
@Builder
public final class ProducingArrangementConsequence {

    @PlanningId
    @EqualsAndHashCode.Include
    private String uuid;

    @DeepPlanningClone
    private SchedulingProducingArrangement producingArrangement;

    private SchedulingResource resource;

    private SchedulingResourceChange resourceChange;

    {
        this.uuid = UUID.randomUUID().toString();
    }

    @EqualsAndHashCode.Include
    public Integer getId() {
        return producingArrangement.getId();
    }

    public LocalDateTime getArrangeDateTime() {
        return producingArrangement.getArrangeDateTime();
    }

    public LocalDateTime getProducingDateTime() {
        return producingArrangement.getProducingDateTime();
    }

    public LocalDateTime getProductReapDateTime() {
        return getCompletedDateTime();
    }

    public LocalDateTime getCompletedDateTime() {
        return producingArrangement.getCompletedDateTime();
    }

    public LocalDateTime getFactorySlotOrQueueRestoreDateTime() {
        return getCompletedDateTime();
    }

    public interface SchedulingResource {

        static ProductStock productStock(SchedulingProduct product) {
            return new ProductStock(product);
        }

        static FactoryProducingLength factoryWaitQueue(SchedulingFactoryInstance factoryInstance) {
            return new FactoryProducingLength(factoryInstance);
        }

        public Object getRoot();

    }

    public interface SchedulingResourceChange extends IntFunction<Integer> {

        static Increase increase(int delta) {
            if (delta == 1) {
                return increase();
            }
            return new Increase(delta);
        }

        static Increase increase() {
            return Increase.ONE_INCREASE;
        }

        static Decrease decrease(int delta) {
            if (delta == 1) {
                return decrease();
            }
            return new Decrease(delta);
        }

        static Decrease decrease() {
            return Decrease.ONE_DECREASE;
        }

    }

    @Getter
    @AllArgsConstructor
    public static class ProductStock implements SchedulingResource {

        private SchedulingProduct schedulingProduct;

        @Override
        public SchedulingProduct getRoot() {
            return schedulingProduct;
        }

    }

    @Getter
    @AllArgsConstructor
    public static class FactoryProducingLength implements SchedulingResource {

        private SchedulingFactoryInstance factoryInstance;

        @Override
        public SchedulingFactoryInstance getRoot() {
            return factoryInstance;
        }

    }

    public static class Increase implements SchedulingResourceChange {

        public static final Increase ONE_INCREASE = new Increase(1);

        private final int delta;

        public Increase(int delta) {
            this.delta = delta;
        }

        @Override
        public Integer apply(int value) {
            return value + delta;
        }

    }

    public static class Decrease implements SchedulingResourceChange {

        public static final Decrease ONE_DECREASE = new Decrease(1);

        private final int delta;

        public Decrease(int delta) {
            this.delta = delta;
        }

        @Override
        public Integer apply(int value) {
            return value - delta;
        }

    }

}
