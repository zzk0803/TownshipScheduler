package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.solution.*;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@PlanningSolution
public class TownshipSchedulingProblem {

    private UUID uuid;

    private String playerName;

    private Integer playerLevel;

    @ProblemFactCollectionProperty
    private List<SchedulingGoods> goods;

    @ProblemFactCollectionProperty
    private List<SchedulingOrder> orders;

    @PlanningEntityProperty
    private SchedulingWarehouse warehouse;

    @PlanningEntityCollectionProperty
    private List<SchedulingPlantFieldSlot> plantSlots;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    private List<SchedulingProducing> schedulingProducingList;

    @PlanningScore
    private HardMediumSoftLongScore score;

}
