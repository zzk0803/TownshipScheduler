package zzk.townshipscheduler.backend.scheduling.model.fact;

import zzk.townshipscheduler.port.GoodId;
import zzk.townshipscheduler.backend.scheduling.model.WorkPlace;

public class SchedulingPlantSlot implements WorkPlace {

    private String category;

    private int parallel;

    private GoodId producing;

}
