package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.cloner.DeepPlanningClone;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import lombok.Data;

import java.util.Map;
import java.util.function.BiFunction;

@Data
public class SchedulingWarehouse {

    @DeepPlanningClone
    private Map<SchedulingProduct, Integer> productAmountMap;

}
