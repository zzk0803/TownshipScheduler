package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.solution.cloner.DeepPlanningClone;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SchedulingArrangementHierarchies {

    @PlanningId
    String uuid;

    SchedulingProducingArrangement whole;

    SchedulingProducingArrangement partial;

}
