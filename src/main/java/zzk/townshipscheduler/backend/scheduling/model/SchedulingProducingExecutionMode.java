package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import zzk.townshipscheduler.backend.scheduling.ProductAmountBill;

import java.time.Duration;
import java.util.List;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@PlanningEntity
public class SchedulingProducingExecutionMode {

    @EqualsAndHashCode.Include
    private String uuid;

    private SchedulingProduct product;

    private ProductAmountBill materials;

    private Duration executeDuration = Duration.ZERO;

    @PlanningPin
    private boolean boolWeatherExecutionModeSingle;

    @PlanningPin
    private boolean boolKeepingBySchedulingProcess;

    @InverseRelationShadowVariable(sourceVariableName = "planningProducingExecutionMode")
    private List<SchedulingGameActionProductProducing> assignedProducingActionSet;

    public SchedulingProducingExecutionMode() {
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
