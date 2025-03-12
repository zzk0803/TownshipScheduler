package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.*;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true,callSuper = true)
@PlanningEntity
public class SchedulingFactoryInstanceMultiple extends AbstractFactoryInstance {

    public static final String PLANNING_ACTIONS = "planningFactoryActionList";

    @InverseRelationShadowVariable(sourceVariableName = SchedulingPlayerFactoryProducingArrangement.PLANNING_FACTORY)
    private List<SchedulingPlayerFactoryProducingArrangement> planningFactoryActionList = new ArrayList<>();

}
