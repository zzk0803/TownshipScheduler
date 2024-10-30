package zzk.townshipscheduler.backend.scheduling.model.entity;

public class GoodsReceiptMove extends PlantSlotReapMove{

    private BarnSaveMove barnSaveMove;

    @Override
    protected boolean boolWorkPlaceSupport() {
        return false;
    }

    @Override
    protected void doWorkInPlace() {

    }

}
