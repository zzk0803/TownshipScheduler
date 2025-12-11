package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@PlanningEntity
public abstract class SchedulingArrangementOrFactorySlot {

    public static final String PLANNING_PREVIOUS_PRODUCING_ARRANGEMENT = "planningPreviousProducingArrangement";

    @Getter
    @Setter
    @InverseRelationShadowVariable(sourceVariableName = PLANNING_PREVIOUS_PRODUCING_ARRANGEMENT)
    private SchedulingProducingArrangement nextProducingArrangement;

}
