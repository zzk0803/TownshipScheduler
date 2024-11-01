package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.cloner.DeepPlanningClone;

import java.util.Map;

public class SchedulingWarehouse extends BaseSchedulingProducingOrWarehouse{

    private Map<SchedulingGoods, Integer> itemAmountMap;

}
