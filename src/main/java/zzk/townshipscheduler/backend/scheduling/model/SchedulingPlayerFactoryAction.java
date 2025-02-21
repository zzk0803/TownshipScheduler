package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.builder.CompareToBuilder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;


@Data
@PlanningEntity
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class SchedulingPlayerFactoryAction implements Comparable<SchedulingPlayerFactoryAction>
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

//    private SchedulingPlayerFactoryAction sinkGameAction;

//    private List<SchedulingPlayerFactoryAction> materialActions = new ArrayList<>();

//    private List<SchedulingPlayerFactoryAction> succeedingActions = new ArrayList<>();

//    @PlanningVariable(
//            graphType = PlanningVariableGraphType.CHAINED,
//            valueRangeProviderRefs = {
//                    "factories", "factoryActions"
//            }
//    )
//    private BasePlanningChainSupportFactoryOrAction planningPrevious;

    @PlanningVariable(valueRangeProviderRefs = "actionSequenceValueRange")
    private Integer sequence;

    @PlanningVariable(
            valueRangeProviderRefs = "schedulingFactoryTimeSlotValueRange"
    )
    private SchedulingFactoryTimeSlotInstance planningTimeSlotFactory;

    //    @PlanningVariable(valueRangeProviderRefs = "producingExecutionMode")
    private SchedulingProducingExecutionMode producingExecutionMode;

    //    @PlanningVariable(valueRangeProviderRefs = "planningPlayerArrangeDateTimeValueRange")
    //    private LocalDateTime planningPlayerArrangeDateTime;

    //    @ShadowVariable(
    //            variableListenerClass = FactoryActionShadowGameProducingDataTimeVariableListener.class,
    //            sourceVariableName = "planningPlayerArrangeDateTime"
    //    )
    //    @ShadowVariable(
    //            variableListenerClass = FactoryActionShadowGameProducingDataTimeVariableListener.class,
    //            sourceVariableName = "sequence"
    //    )
    //    @ShadowVariable(
    //            variableListenerClass = FactoryActionShadowGameProducingDataTimeVariableListener.class,
    //            sourceVariableName = "schedulingPeriodFactory"
    //    )
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

//    private void appendMaterialAction(SchedulingPlayerFactoryAction that) {
//        this.materialActions.add(that);
//    }

//    private void appendSucceedingAction(SchedulingPlayerFactoryAction that) {
//        this.succeedingActions.add(that);
//    }

    public String getHumanReadable() {
        return getCurrentActionObject().readable();
    }

    public SchedulingDateTimeSlot getArrangeDateTimeSlot() {
        return getPlanningTimeSlotFactory().getDateTimeSlot();
    }

//    public boolean boolExecutionModeMismatch() {
//        Set<SchedulingProducingExecutionMode> producingExecutionModeSet = getSchedulingProduct().getExecutionModeSet();
//        return !producingExecutionModeSet.contains(getProducingExecutionMode());
//    }

    public boolean boolFactoryMismatch() {
        SchedulingFactoryInfo planningFactoryType = getPlanningTimeSlotFactory().getSchedulingFactoryInfo();
        SchedulingProduct schedulingProduct = getSchedulingProduct();
        return !planningFactoryType.getPortfolio().contains(schedulingProduct);
    }

    public SchedulingProduct getSchedulingProduct() {
        return (SchedulingProduct) getCurrentActionObject();
    }

    @ValueRangeProvider(id = "schedulingFactoryTimeSlotValueRange")
    public List<SchedulingFactoryTimeSlotInstance> schedulingFactoryTimeSlotValueRange() {
        SchedulingProduct schedulingProduct = getSchedulingProduct();
        SchedulingFactoryInfo requireFactory = schedulingProduct.getRequireFactory();
        Set<SchedulingFactoryInstance> factoryInstances = requireFactory.getFactoryInstances();
        return factoryInstances.stream()
                .map(SchedulingFactoryInstance::getSchedulingFactoryTimeSlotInstances)
                .flatMap(Collection::stream)
                .toList();

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

    @Override
    public int compareTo(SchedulingPlayerFactoryAction that) {
        Comparator<SchedulingPlayerFactoryAction> nullsFirst
                = Comparator.nullsFirst(Comparator.comparing(SchedulingPlayerFactoryAction::getSequence));
        Comparator<SchedulingPlayerFactoryAction> nullsLast
                = Comparator.nullsLast(Comparator.comparing(SchedulingPlayerFactoryAction::getSequence));
        return new CompareToBuilder()
                .append(
                        this,
                        that,
                        nullsFirst.thenComparing(nullsLast)
                )
                .toComparison();
    }

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

    public Duration getActionDuration() {
        return getProducingExecutionMode().getExecuteDuration();
    }

    @Override
    public String toString() {
        return "{\"SchedulingPlayerFactoryAction\":{"
               + "        \"actionId\":\"" + actionId + "\""
               + ",         \"actionUuid\":\"" + actionUuid + "\""
               + ",         \"targetActionObject\":" + targetActionObject
               + ",         \"planningFactory\":" + Objects.toString(planningTimeSlotFactory, "N/A")
               + ",         \"planningProducingExecutionMode\":" + Objects.toString(
                producingExecutionMode,
                "N/A"
        )
               + ",         \"planningPlayerArrangeDateTime\":" + Objects.toString(
                getPlanningTimeSlotFactory().getDateTimeSlot(),
                "N/A"
        )
               + ",         \"shadowGameProducingDataTime\":" + Objects.toString(shadowGameProducingDataTime, "N/A")
               + ",         \"shadowGameCompleteDateTime\":" + Objects.toString(shadowGameCompleteDateTime, "N/A")
               + "}}";
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

    private List<SchedulingFactoryTimeSlotInstance> getAffectFollowingFactories() {
        List<SchedulingFactoryTimeSlotInstance> affectFactories = new ArrayList<>();
        SchedulingFactoryTimeSlotInstance planningFactory = this.getPlanningTimeSlotFactory();
        while (
                planningFactory.getNextPeriodOfFactory() != null
                && planningFactory.boolAffectByAction(this)
        ) {
            affectFactories.add(planningFactory.getNextPeriodOfFactory());
            planningFactory = planningFactory.getNextPeriodOfFactory();
        }
        return affectFactories;
    }

    public List<ActionConsequence> calcAccumulatedConsequence() {
        if (getPlanningTimeSlotFactory() == null) {
            return List.of();
        }
        if (getShadowGameProducingDataTime() == null) {
            return List.of();
        }

        SchedulingProducingExecutionMode executionMode = getProducingExecutionMode();
        List<ActionConsequence> actionConsequenceList = new ArrayList<>(10);
        //when arrange,materials was consumed
        if (!executionMode.boolAtomicProduct()) {
            ProductAmountBill materials = executionMode.getMaterials();
            materials.forEach((material, amount) -> {
                ActionConsequence consequence = ActionConsequence.builder()
                        .localDateTime(getPlanningTimeSlotFactory().getDateTimeSlot().getStart())
                        .resource(ActionConsequence.SchedulingResource.productStock(material))
                        .resourceChange(ActionConsequence.SchedulingResourceChange.decrease(amount))
                        .build();
                actionConsequenceList.add(consequence);
            });
        }

        //when arrange,factory wait queue was consumed
        actionConsequenceList.add(
                ActionConsequence.builder()
                        .localDateTime(getPlanningTimeSlotFactory().getDateTimeSlot().getStart())
                        .resource(ActionConsequence.SchedulingResource.factoryWaitQueue(
                                getPlanningTimeSlotFactory().getFactoryInstance()))
                        .resourceChange(ActionConsequence.SchedulingResourceChange.decrease())
                        .build()
        );

        //when completed ,factory wait queue was release
        actionConsequenceList.add(
                ActionConsequence.builder()
                        .localDateTime(getShadowGameCompleteDateTime())
                        .resource(ActionConsequence.SchedulingResource.factoryWaitQueue(
                                getPlanningTimeSlotFactory().getFactoryInstance()))
                        .resourceChange(ActionConsequence.SchedulingResourceChange.increase())
                        .build()
        );

        //when completed ,product stock was increase
        actionConsequenceList.add(
                ActionConsequence.builder()
                        .localDateTime(getShadowGameCompleteDateTime())
                        .resource(ActionConsequence.SchedulingResource.productStock(getSchedulingProduct()))
                        .resourceChange(ActionConsequence.SchedulingResourceChange.increase())
                        .build()
        );


        return actionConsequenceList;
    }

    public LocalDateTime getShadowGameProducingDataTime() {
        if (this.getPlanningTimeSlotFactory() == null) {
            return this.shadowGameProducingDataTime = null;
        }

        return this.shadowGameProducingDataTime
                = this.getPlanningTimeSlotFactory().getProducingDateTime(this);
    }

    public LocalDateTime getShadowGameCompleteDateTime() {
        if (this.getShadowGameProducingDataTime() == null) {
            return this.shadowGameCompleteDateTime = null;
        }

        if (this.shadowGameCompleteDateTime == null) {
            this.shadowGameCompleteDateTime
                    = this.shadowGameProducingDataTime.plus(
                    this.producingExecutionMode.getExecuteDuration()
            );
        }
        return this.shadowGameCompleteDateTime;
    }

}
