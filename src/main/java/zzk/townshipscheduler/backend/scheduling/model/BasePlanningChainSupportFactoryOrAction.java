package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@PlanningEntity
public abstract class BasePlanningChainSupportFactoryOrAction {

    @InverseRelationShadowVariable(sourceVariableName = "planningPrevious")
    private SchedulingPlayerFactoryAction planningNext;

}
