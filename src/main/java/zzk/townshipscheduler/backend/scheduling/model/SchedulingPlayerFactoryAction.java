package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.*;
import lombok.*;
import zzk.townshipscheduler.backend.scheduling.model.utility.ActionIdRoller;
import zzk.townshipscheduler.backend.scheduling.ProductAmountBill;
import zzk.townshipscheduler.backend.scheduling.model.utility.FactoryActionShadowGameProducingDataTimeVariableListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


@Data
@PlanningEntity
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class SchedulingPlayerFactoryAction extends BasePlanningChainSupportFactoryOrAction {

    @EqualsAndHashCode.Include
    @PlanningId
    protected Integer actionId;

    @EqualsAndHashCode.Include
    protected String actionUuid;

    public void readyElseThrow() {
        Objects.requireNonNull(getActionId());
    }

    private PlayerFactoryActionType playerFactoryActionType;

    private SchedulingGameActionObject targetActionObject;

    private SchedulingGameActionObject currentActionObject;

    private SchedulingPlayerFactoryAction sourceGameAction;

    private SchedulingPlayerFactoryAction sinkGameAction;

    private List<SchedulingPlayerFactoryAction> prerequisiteActions;

    private List<SchedulingPlayerFactoryAction> succeedingActions;

    @PlanningVariable(valueRangeProviderRefs = "producingExecutionMode")
    private SchedulingGameActionExecutionMode planningProducingExecutionMode;

    @PlanningVariable(valueRangeProviderRefs = "planningPlayerDoItDateTimeValueRange")
    private LocalDateTime planningPlayerDoItDateTime;

    @PlanningVariable(
            graphType = PlanningVariableGraphType.CHAINED,
            valueRangeProviderRefs = {
                    "factories", "factoryActions"
            }
    )
    private BasePlanningChainSupportFactoryOrAction planningPrevious;

    @AnchorShadowVariable(sourceVariableName = "planningPrevious")
    private SchedulingFactoryInstance planningFactory;

    @ShadowVariable(
            variableListenerClass = FactoryActionShadowGameProducingDataTimeVariableListener.class,
            sourceVariableName = "planningProducingExecutionMode"
    )
    @ShadowVariable(
            variableListenerClass = FactoryActionShadowGameProducingDataTimeVariableListener.class,
            sourceEntityClass = SchedulingPlayerFactoryAction.class,
            sourceVariableName = "planningPlayerDoItDateTime"
    )
    @ShadowVariable(
            variableListenerClass = FactoryActionShadowGameProducingDataTimeVariableListener.class,
            sourceEntityClass = SchedulingPlayerFactoryAction.class,
            sourceVariableName = "planningPrevious"
    )
//    @ShadowVariable(
//            variableListenerClass = SchedulingGameActionProductProducingVariableListener.class,
//            sourceEntityClass = BaseSchedulingGameAction.class,
//            sourceVariableName = "planningWorkplace"
//    )
    private LocalDateTime shadowGameProducingDataTime;

    @PiggybackShadowVariable(shadowVariableName = "shadowGameProducingDataTime")
    private LocalDateTime shadowGameCompleteDateTime;

    private InnerFlag innerFlag = InnerFlag.SCHEDULING;

    public SchedulingPlayerFactoryAction(
            PlayerFactoryActionType actionType,
            SchedulingGameActionObject targetActionObject,
            SchedulingGameActionObject currentActionObject
    ) {
        this();
        this.playerFactoryActionType = actionType;
        this.targetActionObject = targetActionObject;
        this.currentActionObject = currentActionObject;
    }

    public SchedulingPlayerFactoryAction() {
        this.actionUuid = UUID.randomUUID().toString();
    }

    public void forceSetupExecutionMode(SchedulingGameActionExecutionMode executionMode) {
        if (getCurrentActionObject().getExecutionModeSet().contains(executionMode)) {
            setPlanningProducingExecutionMode(executionMode);
            executionMode.setBoolWeatherExecutionModeSingle(true);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public void idRoller(ActionIdRoller idRoller) {
        idRoller.setup(this);
    }

    public void biAssociateWholeToPart(SchedulingPlayerFactoryAction partAction) {
        this.appendPrerequisiteAction(partAction);
        partAction.appendSucceedingAction(this);
        partAction.setTargetActionObject(this.getCurrentActionObject());
    }

    private void appendPrerequisiteAction(SchedulingPlayerFactoryAction that) {
        if (this.prerequisiteActions == null) {
            this.prerequisiteActions = new ArrayList<>();
        }
        this.prerequisiteActions.add(that);
    }

    private void appendSucceedingAction(SchedulingPlayerFactoryAction that) {
        if (this.succeedingActions == null) {
            this.succeedingActions = new ArrayList<>();
        }
        this.succeedingActions.add(that);
    }

    public String getHumanReadable() {
        switch (playerFactoryActionType) {
            case ARRANGE_PRODUCING -> {
                return "Arrange Producing::" + getCurrentActionObject().readable();
            }
            case REAP_AND_STOCK -> {
                return "Reap And Stock::" + getCurrentActionObject().readable();
            }
        }
        return null;
    }

    public List<SchedulingWarehouse.Record> toWarehouseConsequence() {
        switch (playerFactoryActionType) {
            case ARRANGE_PRODUCING -> {
                SchedulingGameActionExecutionMode planningProducingExecutionMode = this.getPlanningProducingExecutionMode();
                if (getPlanningPlayerDoItDateTime() == null || planningProducingExecutionMode == null || getPlanningFactory() == null) {
                    return List.of();
                }
                ProductAmountBill productAmountBill = planningProducingExecutionMode.getMaterials();
                List<SchedulingWarehouse.Record> recordList = new ArrayList<>(productAmountBill.size() * 3);
                productAmountBill.forEach((material, amount) -> {
                    SchedulingWarehouse.Record record = SchedulingWarehouse.Record.builder()
                            .item(material)
                            .factoryAction(this)
                            .playerDateTime(getPlanningPlayerDoItDateTime())
                            .gameFinishedDateTime(getShadowGameCompleteDateTime())
                            .delta(-amount)
                            .build();
                    recordList.add(record);
                });
                return recordList;
            }

            case REAP_AND_STOCK -> {
                SchedulingProduct schedulingProduct = (SchedulingProduct) this.getCurrentActionObject();
                if (getPlanningPlayerDoItDateTime() == null || getPlanningFactory() == null) {
                    return List.of();
                }

                SchedulingWarehouse.Record record = SchedulingWarehouse.Record.builder()
                        .item(schedulingProduct)
                        .factoryAction(this)
                        .playerDateTime(getPlanningPlayerDoItDateTime())
                        .gameFinishedDateTime(getPlanningPlayerDoItDateTime())
                        .delta(schedulingProduct.getGainWhenCompleted())
                        .build();
                return List.of(record);
            }
        }
        return List.of();
    }

    public void clearShadowVariable() {
        setShadowGameProducingDataTime(null);
        setShadowGameCompleteDateTime(null);
    }

    public SchedulingProduct getSchedulingProduct() {
        return (SchedulingProduct) getCurrentActionObject();
    }

    public static enum InnerFlag {
        DUMMY_SOURCE,
        SCHEDULING,
        DUMMY_SINK
    }

}
