package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import zzk.townshipscheduler.backend.scheduling.ProductAmountBill;

import java.time.Duration;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SchedulingGameActionExecutionMode {

    @PlanningId
    @EqualsAndHashCode.Include
    private Integer id;

    private SchedulingProduct product;

    private ProductAmountBill materials;

    private Duration executeDuration = Duration.ZERO;

    @PlanningPin
    private boolean boolWeatherExecutionModeSingle;

    @PlanningPin
    private boolean boolKeepingBySchedulingProcess;

//    @InverseRelationShadowVariable(sourceVariableName = "planningProducingExecutionMode")
//    private List<SchedulingGameActionProductProducing> assignedProducingActionSet;

    public SchedulingGameActionExecutionMode() {
    }

    public boolean atomicProduct() {
        return materials == null || materials.isEmpty();
    }

    @Override
    public String toString() {
        return "{\"SchedulingProducingExecutionMode\":{"
               + "        \"materials\":" + materials
               + ",         \"executeDuration\":" + executeDuration
               + "}}";
    }

}
