//package zzk.townshipscheduler.backend.scheduling.model;
//
//import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
//import ai.timefold.solver.core.api.domain.lookup.PlanningId;
//import ai.timefold.solver.core.api.domain.variable.AnchorShadowVariable;
//import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
//import ai.timefold.solver.core.api.domain.variable.PlanningVariableGraphType;
//import lombok.Data;
//import lombok.EqualsAndHashCode;
//import lombok.ToString;
//import zzk.townshipscheduler.backend.scheduling.model.ProductAmountBill;
//
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Objects;
//import java.util.UUID;
//
//@Data
//@PlanningEntity
//@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
//public class SchedulingPlayerWarehouseAction extends BasePlanningChainSupportWarehouseOrAction implements IGameAction {
//
//    @EqualsAndHashCode.Include
//    @PlanningId
//    private Integer actionId;
//
//    @EqualsAndHashCode.Include
//    private String actionUuid;
//
//    @PlanningVariable(valueRangeProviderRefs = "planningPlayerDoItDateTimeValueRange")
//    private LocalDateTime planningPlayerDoItDateTime;
//
//    private IGameActionObject gameActionObject;
//
//    @ToString.Exclude
//    @PlanningVariable(
//            graphType = PlanningVariableGraphType.CHAINED,
//            valueRangeProviderRefs = {
//                    "warehouseActions", "warehouse"
//            }
//    )
//    private BasePlanningChainSupportWarehouseOrAction planningPrevious;
//
//    @ToString.Exclude
//    @AnchorShadowVariable(sourceVariableName = "planningPrevious")
//    private SchedulingWarehouse warehouse;
//
//    public SchedulingPlayerWarehouseAction(
//            IGameActionObject gameActionObject
//    ) {
//        this();
//        this.gameActionObject = gameActionObject;
//    }
//
//    public SchedulingPlayerWarehouseAction() {
//        this.actionUuid = UUID.randomUUID().toString();
//    }
//
//    public SchedulingPlayerWarehouseAction(
//            SchedulingProduct schedulingProduct
//    ) {
//        this();
//        this.gameActionObject = schedulingProduct;
//    }
//
//    public SchedulingPlayerWarehouseAction(
//            SchedulingOrder schedulingOrder
//    ) {
//        this();
//        this.gameActionObject = schedulingOrder;
//    }
//
//    public void readyElseThrow() {
//        Objects.requireNonNull(getActionId());
//    }
//
//    public void activate(ActionIdRoller activate) {
//        activate.setup(this);
//    }
//
//    @Override
//    public String actionUuid() {
//        return getActionUuid();
//    }
//
//    @Override
//    public List<Consequence> actionConsequence() {
//
//        if (getPlanningPlayerDoItDateTime() == null) {
//            return List.of();
//        }
//
//        IGameActionObject schedulingGameActionObject = getGameActionObject();
//        if (schedulingGameActionObject instanceof SchedulingProduct schedulingProduct) {
//            Consequence consequence = Consequence.builder()
//                    .schedulingProduct(schedulingProduct)
//                    .actionUuid(this.getActionUuid())
//                    .playerArrangeDateTime(getPlanningPlayerDoItDateTime())
//                    .gameFinishedDateTime(getPlanningPlayerDoItDateTime())
//                    .delta(schedulingProduct.getGainWhenCompleted())
//                    .build();
//            return List.of(consequence);
//        } else if (schedulingGameActionObject instanceof SchedulingOrder schedulingOrder) {
//            ProductAmountBill productAmountBill = schedulingOrder.getProductAmountBill();
//            List<Consequence> recordList = new ArrayList<>(productAmountBill.size() * 3);
//            productAmountBill.forEach((schedulingProduct, amount) -> {
//                Consequence record = Consequence.builder()
//                        .schedulingProduct(schedulingProduct)
//                        .actionUuid(this.getActionUuid())
//                        .playerArrangeDateTime(getPlanningPlayerDoItDateTime())
//                        .gameFinishedDateTime(getPlanningPlayerDoItDateTime())
//                        .delta(-amount)
//                        .build();
//                recordList.add(record);
//            });
//
//            return recordList;
//        }
//
//        return List.of();
//    }
//
//}
