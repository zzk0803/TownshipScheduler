package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.AnchorShadowVariable;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;

@PlanningEntity
public abstract class BaseSchedulingProducingOrWarehouse {

    @InverseRelationShadowVariable(sourceVariableName = "previousProducing")
    private SchedulingProducing nextProducing;

}
