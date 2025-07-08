package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.solution.cloner.DeepPlanningClone;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.IntFunction;

@Data
public final class ProducingArrangementConsequence {

    @EqualsAndHashCode.Include
    private String uuid;

    private LocalDateTime arrangementDateTime;

    private LocalDateTime producingDateTime;

    private LocalDateTime completedDateTime;

    private SchedulingResource resource;

    private SchedulingResourceChange resourceChange;

    {
        this.uuid = UUID.randomUUID().toString();
    }

    ProducingArrangementConsequence(
            String uuid,
            LocalDateTime arrangementDateTime,
            LocalDateTime producingDateTime,
            LocalDateTime completedDateTime,
            SchedulingResource resource,
            SchedulingResourceChange resourceChange
    ) {
        this.uuid = uuid;
        this.arrangementDateTime = arrangementDateTime;
        this.producingDateTime = producingDateTime;
        this.completedDateTime = completedDateTime;
        this.resource = resource;
        this.resourceChange = resourceChange;
    }

    public static ProducingArrangementConsequenceBuilder builder() {
        return new ProducingArrangementConsequenceBuilder();
    }

    public LocalDateTime getArrangeDateTime() {
        return this.arrangementDateTime;
    }

    public LocalDateTime getProducingDateTime() {
        return this.producingDateTime;
    }


    public LocalDateTime getCompletedDateTime() {
        return this.completedDateTime;
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

    public static class ProducingArrangementConsequenceBuilder {

        private String uuid;

        private SchedulingProducingArrangement producingArrangement;

        private SchedulingResource resource;

        private SchedulingResourceChange resourceChange;

        ProducingArrangementConsequenceBuilder() {
        }

        public ProducingArrangementConsequenceBuilder uuid(String uuid) {
            this.uuid = uuid;
            return this;
        }

        public ProducingArrangementConsequenceBuilder producingArrangement(SchedulingProducingArrangement producingArrangement) {
            this.producingArrangement = producingArrangement;
            return this;
        }

        public ProducingArrangementConsequenceBuilder resource(SchedulingResource resource) {
            this.resource = resource;
            return this;
        }

        public ProducingArrangementConsequenceBuilder resourceChange(SchedulingResourceChange resourceChange) {
            this.resourceChange = resourceChange;
            return this;
        }

        public ProducingArrangementConsequence build() {
            return new ProducingArrangementConsequence(
                    this.uuid,
                    this.producingArrangement.getArrangeDateTime(),
                    this.producingArrangement.getProducingDateTime(),
                    this.producingArrangement.getCompletedDateTime(),
                    this.resource,
                    this.resourceChange
            );
        }

    }

}
