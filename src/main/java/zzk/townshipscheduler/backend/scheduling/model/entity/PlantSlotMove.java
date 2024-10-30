package zzk.townshipscheduler.backend.scheduling.model.entity;

import zzk.townshipscheduler.backend.scheduling.model.fact.SchedulingPlantSlot;

public abstract class PlantSlotMove extends SchedulingMove {

    private SchedulingPlantSlot schedulingPlantSlot;

    @Override
    protected SchedulingPlantSlot getWorkPlace() {
        return schedulingPlantSlot;
    }

}
