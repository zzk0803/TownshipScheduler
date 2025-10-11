package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SchedulingArrangementHierarchies {

    String uuid;

    SchedulingProducingArrangement whole;

    SchedulingProducingArrangement partial;

}
