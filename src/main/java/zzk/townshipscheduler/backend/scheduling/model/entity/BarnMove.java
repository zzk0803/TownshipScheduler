package zzk.townshipscheduler.backend.scheduling.model.entity;

import lombok.Getter;
import zzk.townshipscheduler.port.GoodId;
import zzk.townshipscheduler.backend.scheduling.model.fact.SchedulingBarn;

public abstract class BarnMove extends SchedulingMove {

    @Getter
    protected final GoodId goodId;

    @Getter
    protected final Integer amount;

    public BarnMove(GoodId goodId, Integer amount) {
        this.goodId = goodId;
        this.amount = amount;
    }

    @Override
    protected SchedulingBarn getWorkPlace() {
        return barn;
    }

}
