package zzk.townshipscheduler.backend.scheduling.model;

import lombok.Data;

import java.util.Map;

@Data
public class SchedulingWarehouse {

    private Map<SchedulingProduct, Integer> productAmountMap;

}
