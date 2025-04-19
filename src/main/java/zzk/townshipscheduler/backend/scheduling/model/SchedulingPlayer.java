package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalTime;
import java.util.Map;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class SchedulingPlayer {

    @EqualsAndHashCode.Include
    @ToString.Include
    private String id = "test";

    private LocalTime sleepStart = LocalTime.of(22, 30);

    private LocalTime sleepEnd = LocalTime.of(7, 30);

    private Map<SchedulingProduct, Integer> productAmountMap;


}
