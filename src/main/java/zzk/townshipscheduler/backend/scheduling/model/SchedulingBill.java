package zzk.townshipscheduler.backend.scheduling.model;

import zzk.townshipscheduler.backend.persistence.BillType;
import zzk.townshipscheduler.backend.persistence.Goods;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class SchedulingBill {

    private Long id;

    private BillType billType;

    private LocalDateTime schedulingDateTime;

    private LocalDateTime createdDateTime;

    private boolean boolDeadLine;

    private LocalDateTime deadLine;

    private Map<Goods, Integer> productAmountPairs = new HashMap<>();

    private boolean boolFinished;

    private LocalDateTime finishedDateTime;

}
