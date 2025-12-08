package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SchedulingArrangementHierarchies {

    @EqualsAndHashCode.Include
    String uuid;

    SchedulingProducingArrangement whole;

    SchedulingProducingArrangement partial;

}
