package zzk.townshipscheduler.backend.scheduling.model.fact;

import zzk.townshipscheduler.port.GoodId;
import zzk.townshipscheduler.backend.scheduling.model.WorkPlace;

import java.util.Map;

public class SchedulingBarn implements WorkPlace {

    private Map<GoodId, Integer> stock;

    public void save(GoodId goodId, Integer amount) {
        stock.compute(goodId, (g, a) -> {
            if (stock.containsKey(g)) {
                return a + amount;
            }
            return amount;
        });
    }

    public boolean boolSave(GoodId goodId, Integer amount) {
        return true;
    }

    public void take(GoodId goodId, Integer amount) {
        if (boolTake(goodId, amount)) {
            stock.computeIfPresent(goodId, (g, a) -> a - amount);
        }
    }

    public boolean boolTake(GoodId goodId) {
        return boolTake(goodId, 1);
    }

    public boolean boolTake(GoodId goodId, Integer amount) {
        return stock.get(goodId) >= amount;
    }
}
