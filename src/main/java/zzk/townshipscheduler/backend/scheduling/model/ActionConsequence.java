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

    private LocalDateTime localDateTime;

    private SchedulingResource resource;

    private SchedulingResourceChange resourceChange;

    @Override
    public int compareTo(ActionConsequence that) {
        return Comparator.comparing(ActionConsequence::getLocalDateTime).compare(this, that);
    }

    public interface SchedulingResource {

        static ProductStock productStock(SchedulingProduct product) {
            return new ProductStock(product);
        }

        static FactoryWaitQueue factoryWaitQueue(SchedulingFactoryInstance factoryInstance) {
            return new FactoryWaitQueue(factoryInstance);
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

    }

    @AllArgsConstructor
    public static class FactoryWaitQueue implements SchedulingResource {

        @Getter
        private SchedulingFactoryInstance schedulingFactoryInstance;

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
