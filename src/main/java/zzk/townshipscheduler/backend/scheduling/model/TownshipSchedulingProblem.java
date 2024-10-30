package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import zzk.townshipscheduler.backend.scheduling.model.fact.*;

import java.util.List;

@PlanningSolution
public class TownshipSchedulingProblem {

    private String playerName;

    private Integer playerLevel;

    private SchedulingBarn schedulingBarn;

    @ProblemFactCollectionProperty
    private List<SchedulingGoods> goods;

    @ProblemFactCollectionProperty
    private List<SchedulingGoodsProducingInfo> goodsProducingInfos;

    @ProblemFactCollectionProperty
    private List<SchedulingOrder> orders;

    private List<SchedulingPlantSlot> plantSlots;


}
