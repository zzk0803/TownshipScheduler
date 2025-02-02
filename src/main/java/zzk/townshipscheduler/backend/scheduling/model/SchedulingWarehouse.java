package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.solution.cloner.DeepPlanningClone;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import zzk.townshipscheduler.backend.scheduling.model.utility.WarehouseStockRecordUpdateVariableListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Data
@PlanningEntity
public class SchedulingWarehouse extends BasePlanningChainSupportWarehouseOrAction {

    @PlanningId
    private String warehouseId = "WAREHOUSE";

    private int capacity = 999;

    @DeepPlanningClone
    @ShadowVariable(
            variableListenerClass = WarehouseStockRecordUpdateVariableListener.class,
            sourceEntityClass = BasePlanningChainSupportWarehouseOrAction.class,
            sourceVariableName = "planningNext"
    )
    @ShadowVariable(
            variableListenerClass = WarehouseStockRecordUpdateVariableListener.class,
            sourceEntityClass = BasePlanningChainSupportFactoryOrAction.class,
            sourceVariableName = "planningNext"
    )
    private List<Record> records;

    private Map<SchedulingProduct, Integer> productAmountMap;

    public Map<SchedulingProduct, Integer> toProductAmountMap() {
        return records.stream()
                .collect(Collectors.groupingBy(
                        Record::getItem,
                        HashMap::new,
                        Collectors.summingInt(Record::getDelta)
                ));
    }

    public void renew(List<SchedulingWarehouse.Record> warehouseRecord) {
        this.records = new ArrayList<>(warehouseRecord);
    }

    @Data
    @Builder
    public static class Record {

        SchedulingPlayerFactoryAction factoryAction;

        SchedulingPlayerWarehouseAction warehouseAction;

        LocalDateTime playerDateTime;

        LocalDateTime gameFinishedDateTime;

        SchedulingProduct item;

        Integer delta;

    }

}
