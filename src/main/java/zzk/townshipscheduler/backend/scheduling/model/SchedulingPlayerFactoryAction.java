package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import zzk.townshipscheduler.backend.scheduling.model.utility.FactoryActionDifficultyComparator;
import zzk.townshipscheduler.backend.scheduling.model.utility.FactoryActionComputeAndPushVariableListener;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;


@Data
@PlanningEntity(difficultyComparatorClass = FactoryActionDifficultyComparator.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class SchedulingPlayerFactoryAction
//        implements IGameAction
{

    @EqualsAndHashCode.Include
    @PlanningId
    private Integer actionId;

    @EqualsAndHashCode.Include
    private String actionUuid;

    private IGameActionObject targetActionObject;

    private IGameActionObject currentActionObject;

//    private SchedulingPlayerFactoryAction sourceGameAction;

//    private SchedulingPlayerFactoryAction sinkGameAction;

//    private List<SchedulingPlayerFactoryAction> materialActions = new ArrayList<>();
//
//    private List<SchedulingPlayerFactoryAction> succeedingActions = new ArrayList<>();

//    @PlanningVariable(
//            graphType = PlanningVariableGraphType.CHAINED,
//            valueRangeProviderRefs = {
//                    "factories", "factoryActions"
//            }
//    )
//    private BasePlanningChainSupportFactoryOrAction planningPrevious;

    @PlanningVariable(
            valueRangeProviderRefs = {"planningPlayerArrangeDateTimeValueRange"}
    )
    private SchedulingDateTimeSlot planningDateTimeSlot;

    @PlanningVariable(
            valueRangeProviderRefs = "valueRangeForSequence"
    )
    private Integer planningSequence;

//    @PlanningVariable(
//            valueRangeProviderRefs = "schedulingFactoryTimeSlotValueRange",
//            strengthComparatorClass = PlanningTimeSlotFactoryStrengthComparator.class
//    )
//    private SchedulingFactoryTimeSlotInstance planningTimeSlotFactory;


    @PlanningVariable(
            valueRangeProviderRefs = {"valueRangeForSchedulingFactoryInstance"}
    )
    private SchedulingFactoryInstance planningFactory;

    //    @PlanningVariable(valueRangeProviderRefs = "producingExecutionMode")
    private SchedulingProducingExecutionMode producingExecutionMode;

//        @PlanningVariable(valueRangeProviderRefs = "planningPlayerArrangeDateTimeValueRange")
//        private LocalDateTime planningPlayerArrangeDateTime;

    @ShadowVariable(
            variableListenerClass = FactoryActionComputeAndPushVariableListener.class,
            sourceVariableName = "planningDateTimeSlot"
    )
    @ShadowVariable(
            variableListenerClass = FactoryActionComputeAndPushVariableListener.class,
            sourceVariableName = "planningSequence"
    )
    @ShadowVariable(
            variableListenerClass = FactoryActionComputeAndPushVariableListener.class,
            sourceVariableName = "planningFactory"
    )
    private Long shadowRollingChange = 0L;

    private LocalDateTime shadowGameProducingDataTime;

    //    @ShadowVariable(
    //            variableListenerClass = FactoryActionShadowGameCompletedDataTimeVariableListener.class,
    //            sourceVariableName = "planningProducingExecutionMode"
    //    )
    //    @ShadowVariable(
    //            variableListenerClass = FactoryActionShadowGameCompletedDataTimeVariableListener.class,
    //            sourceVariableName = "shadowGameProducingDataTime"
    //    )

    private LocalDateTime shadowGameCompleteDateTime;

    //    @ShadowVariable(
    //            sourceVariableName = "shadowGameCompleteDateTime",
    //            variableListenerClass = FactoryActionMaterialDoneDateTimeVariableListener.class
    //    )
    //    private LocalDateTime materialDoneDateTime;

    private SchedulingWorkTimeLimit workTimeLimit;

    private SchedulingWarehouse schedulingWarehouse;

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

    @SuppressWarnings("ClassEscapesDefinedScope")
    public void activate(
            ActionIdRoller idRoller,
            SchedulingWorkTimeLimit workTimeLimit,
            SchedulingWarehouse schedulingWarehouse
    ) {
        idRoller.setup(this);
        this.workTimeLimit = workTimeLimit;
        this.schedulingWarehouse = schedulingWarehouse;
    }

//    public void biAssociateWholeToPart(SchedulingPlayerFactoryAction partAction) {
//        this.appendMaterialAction(partAction);
//        partAction.appendSucceedingAction(this);
//        partAction.setTargetActionObject(this.getCurrentActionObject());
//    }
//
//    private void appendMaterialAction(SchedulingPlayerFactoryAction that) {
//        this.materialActions.add(that);
//    }
//
//    private void appendSucceedingAction(SchedulingPlayerFactoryAction that) {
//        this.succeedingActions.add(that);
//    }

    public String getHumanReadable() {
        return getCurrentActionObject().readable();
    }

//    public boolean boolExecutionModeMismatch() {
//        Set<SchedulingProducingExecutionMode> producingExecutionModeSet = getSchedulingProduct().getExecutionModeSet();
//        return !producingExecutionModeSet.contains(getProducingExecutionMode());
//    }

//    public boolean boolFactoryMismatch() {
//        SchedulingFactoryInfo planningFactoryType = getPlanningFactory().getSchedulingFactoryInfo();
//        SchedulingProduct schedulingProduct = getSchedulingProduct();
//        return !planningFactoryType.getPortfolio().contains(schedulingProduct);
//    }

    @ValueRangeProvider(id = "valueRangeForSchedulingFactoryInstance")
    public Set<SchedulingFactoryInstance> valueRangeForSchedulingFactoryInstance() {
        SchedulingProduct schedulingProduct = getSchedulingProduct();
        SchedulingFactoryInfo requireFactory = schedulingProduct.getRequireFactory();
        return requireFactory.getFactoryInstances();
    }

    public SchedulingProduct getSchedulingProduct() {
        return (SchedulingProduct) getCurrentActionObject();
    }

//    public boolean boolArrangeBeforePrerequisiteDone() {
//        LocalDateTime prerequisiteDoneDateTime = null;
//        if (this.materialActions == null || this.materialActions.isEmpty()) {
//            return false;
//        } else {
//            prerequisiteDoneDateTime = this.materialActions.stream()
//                    .map(factoryAction -> Optional.ofNullable(factoryAction.getShadowGameCompleteDateTime()))
//                    .reduce(
//                            this.workTimeLimit.getStartDateTime(),
//                            (formerDateTime, optionalPrerequisiteDoneDateTime) -> {
//                                LocalDateTime latterDateTime = optionalPrerequisiteDoneDateTime.orElse(this.workTimeLimit.getStartDateTime());
//                                return latterDateTime.isAfter(formerDateTime)
//                                        ? latterDateTime
//                                        : formerDateTime;
//                            },
//                            (formerDateTime, latterDateTime) -> {
//                                return latterDateTime.isAfter(formerDateTime)
//                                        ? latterDateTime
//                                        : formerDateTime;
//                            }
//                    );
//        }
//
//        SchedulingDateTimeSlot dateTimeSlot = getSchedulingPeriodFactory().getDateTimeSlot();
//        LocalDateTime slotStart = dateTimeSlot.getStart();
//        return slotStart.isBefore(prerequisiteDoneDateTime);
//    }


//    public LocalDateTime prerequisiteDoneTime() {
//        LocalDateTime result = null;
//        for (SchedulingPlayerFactoryAction prerequisiteAction : this.materialActions) {
//            LocalDateTime prerequisiteActionDoneDateTime = prerequisiteAction.getShadowGameCompleteDateTime();
//            if (prerequisiteActionDoneDateTime == null) {
//                return null;
//            }
//            if (result == null) {
//                result = prerequisiteActionDoneDateTime;
//                continue;
//            }
//            result = prerequisiteActionDoneDateTime.isAfter(result)
//                    ? prerequisiteActionDoneDateTime
//                    : result;
//        }
//        return result;
//    }

    public Duration getProducingDuration() {
        return getProducingExecutionMode().getExecuteDuration();
    }

    public ProductAmountBill getMaterials() {
        return getProducingExecutionMode().getMaterials();
    }

//    public Duration nextAvailableAsDuration(LocalDateTime dateTime) {
//        List<SchedulingFactoryTimeSlotInstance> affectPeriodFactories = getAffectFollowingFactories();
//        if (affectPeriodFactories.isEmpty()) {
//            LocalDateTime completeDateTime = getShadowGameCompleteDateTime();
//            return completeDateTime == null
//                    ? null
//                    : Duration.between(dateTime, completeDateTime);
//        } else {
//            SchedulingFactoryTimeSlotInstance periodFactory
//                    = affectPeriodFactories.getFirst().getPreviousPeriodOfFactory();
//            return periodFactory.nextAvailableAsDuration(dateTime);
//        }
//    }

    public List<ActionConsequence> calcActionConsequence() {
        if (
                getPlanningPlayerArrangeDateTime() == null
                || getPlanningSequence() == null
                || getPlanningFactory() == null
        ) {
            return List.of();
        }

        SchedulingProducingExecutionMode executionMode = getProducingExecutionMode();
        List<ActionConsequence> actionConsequenceList = new ArrayList<>(5);
        //when arrange,materials was consumed
        if (!executionMode.boolAtomicProduct()) {
            ProductAmountBill materials = executionMode.getMaterials();
            materials.forEach((material, amount) -> {
                ActionConsequence consequence = ActionConsequence.builder()
                        .actionId(getActionId())
                        .localDateTime(getPlanningPlayerArrangeDateTime())
                        .resource(ActionConsequence.SchedulingResource.productStock(material))
                        .resourceChange(ActionConsequence.SchedulingResourceChange.decrease(amount))
                        .build();
                actionConsequenceList.add(consequence);
            });
        }

        //when arrange,factory wait queue was consumed
        actionConsequenceList.add(
                ActionConsequence.builder()
                        .actionId(getActionId())
                        .localDateTime(getPlanningPlayerArrangeDateTime())
                        .resource(ActionConsequence.SchedulingResource.factoryWaitQueue(
                                getPlanningFactory()
                        ))
                        .resourceChange(ActionConsequence.SchedulingResourceChange.decrease())
                        .build()
        );

        //when completed ,factory wait queue was release
        actionConsequenceList.add(
                ActionConsequence.builder()
                        .actionId(getActionId())
                        .localDateTime(getShadowGameCompleteDateTime())
                        .resource(ActionConsequence.SchedulingResource.factoryWaitQueue(
                                getPlanningFactory()))
                        .resourceChange(ActionConsequence.SchedulingResourceChange.increase())
                        .build()
        );

        //when completed ,product stock was increase
        actionConsequenceList.add(
                ActionConsequence.builder()
                        .actionId(getActionId())
                        .localDateTime(getShadowGameCompleteDateTime())
                        .resource(ActionConsequence.SchedulingResource.productStock(getSchedulingProduct()))
                        .resourceChange(ActionConsequence.SchedulingResourceChange.increase())
                        .build()
        );


        return actionConsequenceList;
    }

    public LocalDateTime getPlanningPlayerArrangeDateTime() {
        SchedulingDateTimeSlot dateTimeSlot = this.getPlanningDateTimeSlot();
        return dateTimeSlot != null
                ? dateTimeSlot.getStart()
                : null;
    }

    public void acceptComputedDateTime(
            LocalDateTime producingDateTime,
            LocalDateTime completedDateTime
    ) {
        setShadowGameProducingDataTime(producingDateTime);
        setShadowGameCompleteDateTime(completedDateTime);
    }

//    public LocalDateTime getShadowGameProducingDataTime() {
//        if (this.getPlanningSequence() == null || getPlanningPlayerArrangeDateTime() == null || this.getPlanningFactory() == null) {
//            return this.shadowGameProducingDataTime = null;
//        }
//
//        return this.shadowGameProducingDataTime
//                = this.getPlanningFactory().calcProducingDateTime(this);
//    }
//
//    public LocalDateTime getShadowGameCompleteDateTime() {
//        if (this.getShadowGameProducingDataTime() == null) {
//            return this.shadowGameCompleteDateTime = null;
//        }
//
//        return this.shadowGameCompleteDateTime
//                = this.getShadowGameProducingDataTime()
//                .plus(this.producingExecutionMode.getExecuteDuration());
//    }

    public boolean isEqual(SchedulingPlayerFactoryAction that) {
        SchedulingProduct thisProducing = this.getSchedulingProduct();
        SchedulingProduct thatProducing = that.getSchedulingProduct();
        if (thisProducing == null || thatProducing == null) {
            return false;
        } else {
            return thisProducing.equals(thatProducing);
        }
    }

}
