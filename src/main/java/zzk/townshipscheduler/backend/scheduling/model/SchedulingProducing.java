package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.AnchorShadowVariable;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariableGraphType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@PlanningEntity
public class SchedulingProducing extends BaseSchedulingProducingOrWarehouse{

    private SchedulingGoods producingGood;

    @PlanningVariable
    private SchedulingPlantFieldSlot plantSlot;

    @PlanningVariable(graphType = PlanningVariableGraphType.CHAINED, allowsUnassigned = true,valueRangeProviderRefs = {"prerequisite-produced"})
    private BaseSchedulingProducingOrWarehouse previousProducingOrWarehouse;

    @AnchorShadowVariable(sourceVariableName = "previousProducing")
    private SchedulingWarehouse warehouse;

    private LocalDateTime arrangeDateTime;

    private LocalDateTime finishDateTime;

    public SchedulingProducing(SchedulingGoods producingGood) {
        this.producingGood = producingGood;
    }

    @ValueRangeProvider(id = "prerequisite-produced")
    public List<SchedulingProducing> calcSubtreeProducing() {
        List<SchedulingProducing> subtreeProducingList = new ArrayList<>();
        Map<SchedulingGoods, Integer> bom = this.producingGood.getBom();
        for (Map.Entry<SchedulingGoods, Integer> entry : bom.entrySet()) {
            SchedulingGoods material = entry.getKey();
            Integer amount = entry.getValue();
            for (int i = 0; i < amount; i++) {
                subtreeProducingList.add(new SchedulingProducing(material));
            }
        }
        return subtreeProducingList;
    }

}
