package zzk.townshipscheduler.backend.scheduling.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.function.IntFunction;

@Data
@Builder
public final class ActionConsequence implements Comparable<ActionConsequence> {

    private Integer actionId;

    private LocalDateTime localDateTime;

    private SchedulingResource resource;

    private SchedulingResourceChange resourceChange;

    @Override
    public int compareTo(ActionConsequence that) {
        return Comparator.comparing(ActionConsequence::getLocalDateTime).compare(this, that);
    }

    public interface SchedulingResource {

        public Object getRoot();

        static ProductStock productStock(SchedulingProduct product) {
            return new ProductStock(product);
        }

        static FactoryProducingQueue factoryWaitQueue(AbstractFactoryInstance factoryInstance) {
            return new FactoryProducingQueue(factoryInstance);
        }

        static FactoryProducingQueue factoryWaitQueue(SchedulingFactoryInstanceMultiple factoryInstance) {
            return new FactoryProducingQueue(factoryInstance);
        }

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

    @AllArgsConstructor
    public static class ProductStock implements SchedulingResource {

        @Getter
        private SchedulingProduct schedulingProduct;

        @Override
        public Object getRoot() {
            return schedulingProduct;
        }

    }

    public static class FactoryProducingQueue implements SchedulingResource {

        @Getter
        private AbstractFactoryInstance schedulingFactoryInstanceSingle;

        @Getter
        private SchedulingFactoryInstanceMultiple schedulingFactoryInstanceMultiple;

        public FactoryProducingQueue(AbstractFactoryInstance schedulingFactoryInstanceSingle) {
            this.schedulingFactoryInstanceSingle = schedulingFactoryInstanceSingle;
        }

        public FactoryProducingQueue(SchedulingFactoryInstanceMultiple factoryInstance) {
            this.schedulingFactoryInstanceMultiple = factoryInstance;
        }

        @Override
        public Object getRoot() {
            return schedulingFactoryInstanceSingle;
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
