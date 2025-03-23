package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@PlanningEntity
public class SchedulingTypeSlotFactoryInstance extends BaseSchedulingFactoryInstance {

    @InverseRelationShadowVariable(sourceVariableName = BaseProducingArrangement.PLANNING_FACTORY)
    private List<SchedulingFactorySlotProducingArrangement> producingArrangementFactorySlotList = new ArrayList<>();

}
