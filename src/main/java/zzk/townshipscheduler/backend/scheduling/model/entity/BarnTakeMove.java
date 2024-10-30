package zzk.townshipscheduler.backend.scheduling.model.entity;

import zzk.townshipscheduler.port.GoodId;

public class BarnTakeMove extends BarnMove {

    public BarnTakeMove(GoodId goodId, Integer amount) {
        super(goodId, amount);
    }

    @Override
    protected boolean boolWorkPlaceSupport() {
        return getBarn().boolTake(super.getGoodId(),super.getAmount());
    }

    @Override
    protected void doWorkInPlace() {
        getWorkPlace().take(super.getGoodId(),super.getAmount());
    }

}
