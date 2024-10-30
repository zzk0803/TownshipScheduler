package zzk.townshipscheduler.backend.scheduling.model.entity;

public class PlantSlotProducingMove extends PlantSlotMove {

    @Override
    protected boolean boolWorkPlaceSupport() {
        return false;
    }

    @Override
    protected void doWorkInPlace() {

    }

}
