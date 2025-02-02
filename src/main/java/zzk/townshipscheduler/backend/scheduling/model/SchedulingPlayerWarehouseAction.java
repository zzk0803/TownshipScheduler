package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.AnchorShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariableGraphType;
import lombok.*;
import zzk.townshipscheduler.backend.scheduling.model.utility.ActionIdRoller;
import zzk.townshipscheduler.backend.scheduling.ProductAmountBill;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Data
@PlanningEntity
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class SchedulingPlayerWarehouseAction extends BasePlanningChainSupportWarehouseOrAction {

    @EqualsAndHashCode.Include
    @PlanningId
    protected Integer actionId;

    @EqualsAndHashCode.Include
    protected String actionUuid;

    public void readyElseThrow() {
        Objects.requireNonNull(getActionId());
    }

    protected SchedulingOrder schedulingOrder;

    @PlanningVariable(valueRangeProviderRefs = "planningPlayerDoItDateTimeValueRange")
    protected LocalDateTime planningPlayerDoItDateTime;

    @PlanningVariable(
            graphType = PlanningVariableGraphType.CHAINED,
            valueRangeProviderRefs = {
                    "warehouseActions","warehouse"
            }
    )
    private BasePlanningChainSupportWarehouseOrAction planningPrevious;

    @AnchorShadowVariable(sourceVariableName = "planningPrevious")
    private SchedulingWarehouse warehouse;

    public SchedulingPlayerWarehouseAction() {
        this.actionUuid = UUID.randomUUID().toString();
    }

    public SchedulingPlayerWarehouseAction(
            SchedulingOrder schedulingOrder
    ) {
        this();
        this.schedulingOrder = schedulingOrder;
    }

    public void idRoller(ActionIdRoller idRoller) {
        idRoller.setup(this);
    }

    public List<SchedulingWarehouse.Record> toWarehouseConsequence() {
        if (getPlanningPlayerDoItDateTime() == null) {
            return List.of();
        }

        ProductAmountBill productAmountBill = schedulingOrder.getProductAmountBill();
        List<SchedulingWarehouse.Record> recordList = new ArrayList<>(productAmountBill.size() * 3);
        productAmountBill.forEach((schedulingProduct, amount) -> {
            SchedulingWarehouse.Record record = SchedulingWarehouse.Record.builder()
                    .item(schedulingProduct)
                    .warehouseAction(this)
                    .playerDateTime(getPlanningPlayerDoItDateTime())
                    .gameFinishedDateTime(getPlanningPlayerDoItDateTime())
                    .delta(-amount)
                    .build();
            recordList.add(record);
        });

        return recordList;
    }
}
