package zzk.townshipscheduler.backend.scheduling.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class ArrangementIdRoller {

    private static final Map<String, ArrangementIdRoller> HOLDER_MAPPING = new LinkedHashMap<>();

    private final AtomicInteger atomicInteger;

    ArrangementIdRoller() {
        atomicInteger = new AtomicInteger(0);
    }

    public static ArrangementIdRoller forProblem(String uuid) {
        if (HOLDER_MAPPING.containsKey(uuid)) {
            return HOLDER_MAPPING.get(uuid);
        }

        ArrangementIdRoller idRoller = createIdRoller();
        HOLDER_MAPPING.put(uuid, idRoller);
        return idRoller;
    }

    private static ArrangementIdRoller createIdRoller() {
        return new ArrangementIdRoller();
    }

    public void setup(SchedulingProducingArrangement producingArrangement) {
        producingArrangement.setId(atomicInteger.incrementAndGet());
    }

//    public void setup(SchedulingPlayerWarehouseAction warehouseAction) {
//        warehouseAction.setActionId(atomicInteger.getAndIncrement());
//    }

}
