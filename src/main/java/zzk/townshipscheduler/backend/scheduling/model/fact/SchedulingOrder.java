package zzk.townshipscheduler.backend.scheduling.model.fact;

import zzk.townshipscheduler.port.GoodId;
import zzk.townshipscheduler.backend.scheduling.model.WorkObject;

import java.time.LocalDateTime;
import java.util.Map;

public class SchedulingOrder implements WorkObject {

    private long id;

    private String orderType;

    private Map<GoodId, Integer> bill;

    private LocalDateTime deadline;

}
