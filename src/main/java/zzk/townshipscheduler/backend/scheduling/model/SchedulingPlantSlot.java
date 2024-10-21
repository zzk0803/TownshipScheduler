package zzk.townshipscheduler.backend.scheduling.model;

import java.time.LocalDateTime;

public class SchedulingPlantSlot {

    private Long id;

    private String factory;

    private SchedulingPlayer belongPlayer;

    private SchedulingGoods schedulingGoods;

    private LocalDateTime startTime;

}
