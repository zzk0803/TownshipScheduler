package zzk.townshipscheduler.backend.scheduling.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class ActionIdRoller {

    private static final Map<String, ActionIdRoller> HOLDER_MAPPING = new LinkedHashMap<>();

    private final AtomicInteger atomicInteger;

    ActionIdRoller() {
        atomicInteger = new AtomicInteger(1);
    }

    public static ActionIdRoller forProblem(String uuid) {
        if (HOLDER_MAPPING.containsKey(uuid)) {
            return HOLDER_MAPPING.get(uuid);
        }

        ActionIdRoller idRoller = createIdRoller();
        HOLDER_MAPPING.put(uuid, idRoller);
        return idRoller;
    }

    private static ActionIdRoller createIdRoller() {
        return new ActionIdRoller();
    }

    public void setup(SchedulingPlayerProducingArrangement producingArrangement) {
        producingArrangement.setActionId(atomicInteger.getAndIncrement());
    }

    public void setup(SchedulingPlayerFactoryProducingArrangement producingArrangement) {
        producingArrangement.setActionId(atomicInteger.getAndIncrement());
    }

    public void setup(AbstractPlayerProducingArrangement abstractProducingArrangement) {
        abstractProducingArrangement.setActionId(atomicInteger.getAndIncrement());
    }

//    public void setup(SchedulingPlayerWarehouseAction warehouseAction) {
//        warehouseAction.setActionId(atomicInteger.getAndIncrement());
//    }

}
