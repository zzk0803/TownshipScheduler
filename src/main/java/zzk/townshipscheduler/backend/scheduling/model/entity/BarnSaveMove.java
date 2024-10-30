package zzk.townshipscheduler.backend.scheduling.model.entity;

import zzk.townshipscheduler.port.GoodId;

public class BarnSaveMove extends BarnMove {

    public BarnSaveMove(GoodId goodId, Integer amount) {
        super(goodId, amount);
    }

    @Override
    protected boolean boolWorkPlaceSupport() {
        return getWorkPlace().boolSave(super.getGoodId(), super.getAmount());
    }

    @Override
    protected void doWorkInPlace() {
        getWorkPlace().save(super.getGoodId(), super.getAmount());
    }

}
