package zzk.townshipscheduler.backend.scheduling.model.utility;

import zzk.townshipscheduler.backend.scheduling.model.SchedulingPlayerFactoryAction;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingPlayerWarehouseAction;

import java.util.concurrent.atomic.AtomicInteger;

public class ActionIdRoller {

    private final AtomicInteger atomicInteger;

    ActionIdRoller() {
        atomicInteger = new AtomicInteger(1);
    }

    public static ActionIdRoller createIdRoller() {
        return new ActionIdRoller();
    }

    public void setup(SchedulingPlayerFactoryAction factoryAction) {
        factoryAction.setActionId(atomicInteger.getAndIncrement());
    }

    public void setup(SchedulingPlayerWarehouseAction warehouseAction) {
        warehouseAction.setActionId(atomicInteger.getAndIncrement());
    }

}
