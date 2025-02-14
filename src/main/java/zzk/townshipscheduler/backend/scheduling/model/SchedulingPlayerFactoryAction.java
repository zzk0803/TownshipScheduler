package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.solution.cloner.DeepPlanningClone;
import ai.timefold.solver.core.api.domain.variable.AnchorShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariableGraphType;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import zzk.townshipscheduler.backend.scheduling.model.utility.ActionDifficultyComparator;
import zzk.townshipscheduler.backend.scheduling.model.utility.FactoryActionShadowGameCompletedDataTimeVariableListener;
import zzk.townshipscheduler.backend.scheduling.model.utility.FactoryActionShadowGameProducingDataTimeVariableListener;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;


@Data
@PlanningEntity(difficultyComparatorClass = ActionDifficultyComparator.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class SchedulingPlayerFactoryAction extends BasePlanningChainSupportFactoryOrAction
//        implements IGameAction
{

    @EqualsAndHashCode.Include
    @PlanningId
    private Integer actionId;

    @EqualsAndHashCode.Include
    private String actionUuid;

    @EqualsAndHashCode.Include
    private IGameActionObject targetActionObject;

    @EqualsAndHashCode.Include
    private IGameActionObject currentActionObject;

//    private SchedulingPlayerFactoryAction sourceGameAction;
//
//    private SchedulingPlayerFactoryAction sinkGameAction;

    private List<SchedulingPlayerFactoryAction> prerequisiteActions = new ArrayList<>();

    private List<SchedulingPlayerFactoryAction> succeedingActions = new ArrayList<>();

    @PlanningVariable(
            graphType = PlanningVariableGraphType.CHAINED,
            valueRangeProviderRefs = {
                    "factories", "factoryActions"
            }
    )
    private BasePlanningChainSupportFactoryOrAction planningPrevious;

    @AnchorShadowVariable(sourceVariableName = "planningPrevious")
    private SchedulingFactoryInstance planningFactory;

    @PlanningVariable(valueRangeProviderRefs = "producingExecutionMode")
    private SchedulingProducingExecutionMode planningProducingExecutionMode;

    @PlanningVariable(valueRangeProviderRefs = "planningPlayerArrangeDateTimeValueRange")
    private LocalDateTime planningPlayerArrangeDateTime;

    @ShadowVariable(
            variableListenerClass = FactoryActionShadowGameProducingDataTimeVariableListener.class,
            sourceVariableName = "planningPlayerArrangeDateTime"
    )
    @ShadowVariable(
            variableListenerClass = FactoryActionShadowGameProducingDataTimeVariableListener.class,
            sourceVariableName = "planningPrevious"
    )
    @ShadowVariable(
            variableListenerClass = FactoryActionShadowGameProducingDataTimeVariableListener.class,
            sourceVariableName = "planningFactory"
    )
    private LocalDateTime shadowGameProducingDataTime;

    @ShadowVariable(
            variableListenerClass = FactoryActionShadowGameCompletedDataTimeVariableListener.class,
            sourceVariableName = "planningProducingExecutionMode"
    )
    @ShadowVariable(
            variableListenerClass = FactoryActionShadowGameCompletedDataTimeVariableListener.class,
            sourceVariableName = "shadowGameProducingDataTime"
    )
    private LocalDateTime shadowGameCompleteDateTime;

    private InnerFlag innerFlag = InnerFlag.SCHEDULING;

    public SchedulingPlayerFactoryAction(
            IGameActionObject targetActionObject,
            IGameActionObject currentActionObject
    ) {
        this();
        this.targetActionObject = targetActionObject;
        this.currentActionObject = currentActionObject;
    }

    public SchedulingPlayerFactoryAction() {
        this.actionUuid = UUID.randomUUID().toString();
    }

    public void readyElseThrow() {
        Objects.requireNonNull(this.getCurrentActionObject());
        Objects.requireNonNull(getActionId());
    }

    public void forceSetupExecutionMode(SchedulingProducingExecutionMode executionMode) {
        if (getCurrentActionObject().getExecutionModeSet().contains(executionMode)) {
            setPlanningProducingExecutionMode(executionMode);
            executionMode.setBoolForceSetupExecutionMode(true);
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
        this.prerequisiteActions.add(that);
    }

    private void appendSucceedingAction(SchedulingPlayerFactoryAction that) {
        this.succeedingActions.add(that);
    }

    public String getHumanReadable() {
        return "Arrange Producing::" + getCurrentActionObject().readable();
    }

    public boolean boolExecutionModeMismatch() {
        Set<SchedulingProducingExecutionMode> producingExecutionModeSet = getSchedulingProduct().getExecutionModeSet();
        return !producingExecutionModeSet.contains(getPlanningProducingExecutionMode());
    }

    public SchedulingProduct getSchedulingProduct() {
        return (SchedulingProduct) getCurrentActionObject();
    }

    public boolean boolFactoryMismatch() {
        SchedulingFactoryInfo planningFactoryType = getPlanningFactory().getSchedulingFactoryInfo();
        SchedulingFactoryInfo requireFactoryType = getSchedulingProduct().getRequireFactory();
        return !planningFactoryType.equals(requireFactoryType);
    }

    public boolean boolArrangeOutOfFactoryProducingCapacity() {
        int queueSizeWhen
                = getPlanningFactory().availableProducingQueueSizeWhen(
                        getPlanningPlayerArrangeDateTime()
        );
        return queueSizeWhen == 0;
    }

    public boolean boolArrangeValidForPrerequisite() {
        LocalDateTime prerequisiteDoneDT = null;
        if (this.prerequisiteActions == null || this.prerequisiteActions.isEmpty()) {
            return false;
        } else {
            for (SchedulingPlayerFactoryAction prerequisiteAction : this.prerequisiteActions) {
                LocalDateTime ldt = prerequisiteAction.getShadowGameCompleteDateTime();
                if (ldt == null) {
                    return false;
                } else {
                    if (prerequisiteDoneDT == null) {
                        prerequisiteDoneDT = ldt;
                        continue;
                    } else {
                        prerequisiteDoneDT = ldt.isAfter(prerequisiteDoneDT)
                                ? ldt
                                : prerequisiteDoneDT;
                    }
                }
            }
        }
        return this.getPlanningPlayerArrangeDateTime().isBefore(prerequisiteDoneDT);
    }

    public LocalDateTime prerequisiteDoneTime() {
        LocalDateTime result = null;
        for (SchedulingPlayerFactoryAction prerequisiteAction : this.prerequisiteActions) {
            LocalDateTime prerequisiteActionDoneDateTime = prerequisiteAction.getShadowGameCompleteDateTime();
            if (prerequisiteActionDoneDateTime == null) {
                return null;
            }
            if (result == null) {
                result = prerequisiteActionDoneDateTime;
                continue;
            }
            result = prerequisiteActionDoneDateTime.isAfter(result)
                    ? prerequisiteActionDoneDateTime
                    : result;
        }
        return result;
    }

    @Override
    public String toString() {
        return "{\"SchedulingPlayerFactoryAction\":{"
               + "        \"actionId\":\"" + actionId + "\""
               + ",         \"actionUuid\":\"" + actionUuid + "\""
               + ",         \"targetActionObject\":" + targetActionObject
               + ",         \"planningFactory\":" + Objects.toString(planningFactory, "N/A")
               + ",         \"planningProducingExecutionMode\":" + Objects.toString(
                planningProducingExecutionMode,
                "N/A"
        )
               + ",         \"planningPlayerArrangeDateTime\":" + Objects.toString(planningPlayerArrangeDateTime, "N/A")
               + ",         \"shadowGameProducingDataTime\":" + Objects.toString(shadowGameProducingDataTime, "N/A")
               + ",         \"shadowGameCompleteDateTime\":" + Objects.toString(shadowGameCompleteDateTime, "N/A")
               + "}}";
    }

    public Duration nextAvailableAsDuration(LocalDateTime dateTime) {
        LocalDateTime completeDateTime = getShadowGameCompleteDateTime();
        return completeDateTime == null
                ? null
                : Duration.between(dateTime, completeDateTime);
    }

    public enum InnerFlag {
        DUMMY_SOURCE,
        SCHEDULING,
        DUMMY_SINK
    }

//    @Override
//    public List<Consequence> toConsequence() {
//        SchedulingGameActionExecutionMode planningProducingExecutionMode = this.getPlanningProducingExecutionMode();
//        if (getPlanningPlayerDoItDateTime() == null || planningProducingExecutionMode == null || getPlanningFactory() == null) {
//            return List.of();
//        }
//        ProductAmountBill productAmountBill = planningProducingExecutionMode.getMaterials();
//        List<Consequence> recordList = new ArrayList<>(productAmountBill.size() * 3);
//        productAmountBill.forEach((material, amount) -> {
//            Consequence record = Consequence.builder()
//                    .schedulingProduct(material)
//                    .actionUuid(this.actionUuid())
//                    .playerArrangeDateTime(getPlanningPlayerDoItDateTime())
//                    .gameFinishedDateTime(getShadowGameCompleteDateTime())
//                    .delta(-amount)
//                    .build();
//            recordList.add(record);
//        });
//        return recordList;
//    }

}
