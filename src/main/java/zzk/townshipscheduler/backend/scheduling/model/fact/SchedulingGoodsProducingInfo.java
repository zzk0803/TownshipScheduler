package zzk.townshipscheduler.backend.scheduling.model.fact;

import lombok.Data;
import zzk.townshipscheduler.port.GoodId;

import java.time.Duration;
import java.util.Map;

@Data
public class SchedulingGoodsProducingInfo {

    private GoodId goodId;

    private String bomString;

    private Map<GoodId, Integer> bom;

    private String durationString;

    private Duration producingDuration;

}
