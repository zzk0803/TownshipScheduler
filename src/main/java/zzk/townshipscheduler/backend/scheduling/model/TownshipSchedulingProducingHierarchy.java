package zzk.townshipscheduler.backend.scheduling.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

public class TownshipSchedulingProducingHierarchy {

    @Getter
    private int level;

    @Getter
    private Map<SchedulingGoods, Integer> itemAmountMap;

    @Getter
    @Setter
    private TownshipSchedulingProducingHierarchy deeper;

    private TownshipSchedulingProducingHierarchy() {

    }

    private TownshipSchedulingProducingHierarchy(int level, Map<SchedulingGoods, Integer> itemAmountMap) {
        this.level = level;
        this.itemAmountMap = itemAmountMap;
    }

    public static TownshipSchedulingProducingHierarchy init(Map<SchedulingGoods, Integer> itemAmountMap) {
        return new TownshipSchedulingProducingHierarchy(
                1,
                itemAmountMap
        );
    }

    public TownshipSchedulingProducingHierarchy deeper(Map<SchedulingGoods, Integer> itemAmountMap) {
        return new TownshipSchedulingProducingHierarchy(
                this.level + 1,
                itemAmountMap
        );
    }

}
