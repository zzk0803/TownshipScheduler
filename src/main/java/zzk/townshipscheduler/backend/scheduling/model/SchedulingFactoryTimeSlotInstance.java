//package zzk.townshipscheduler.backend.scheduling.model;
//
//import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
//import ai.timefold.solver.core.api.domain.lookup.PlanningId;
//import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
//import lombok.Data;
//import lombok.EqualsAndHashCode;
//import org.jspecify.annotations.NonNull;
//import org.springframework.util.Assert;
//import zzk.townshipscheduler.backend.ProducingStructureType;
//
//import java.time.Duration;
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.List;
//
//@Data
//@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
//@PlanningEntity
//public class SchedulingFactoryTimeSlotInstance {
//
//    @EqualsAndHashCode.Include
//    @PlanningId
//    private Integer id;
//
//    private SchedulingDateTimeSlot dateTimeSlot;
//
//    private SchedulingFactoryInstance factoryInstance;
//
//    private SchedulingFactoryTimeSlotInstance previousPeriodOfFactory;
//
//    private SchedulingFactoryTimeSlotInstance nextPeriodOfFactory;
//
//    @InverseRelationShadowVariable(sourceVariableName = "planningTimeSlotFactory")
//    private List<SchedulingPlayerFactoryAction> inversedPlanningActionList = new ArrayList<>();
//
//    private transient List<SchedulingPlayerFactoryAction> followingFactoryActions = new ArrayList<>();
//
////    @ShadowVariable(
////            variableListenerClass = FactoryActionShadowProducingDateTimeTableVariableListener.class,
////            sourceVariableName = "inversedPlanningActionList"
////    )
////    private Map<SchedulingPlayerFactoryAction, LocalDateTime> shadowProducingDateTimeTable = new LinkedHashMap<>();
//
//    @Override
//    public String toString() {
//        return this.getSchedulingFactoryInfo().getCategoryName() + "#" + this.getSeqNum()
//               + ",size=" + this.getProducingLength()
//               + ",(start,end)=" + "(" + getDateTimeSlot().getStart() + "," + getDateTimeSlot().getEnd() + ")";
//    }
//
//    public SchedulingFactoryInfo getSchedulingFactoryInfo() {
//        return factoryInstance.getSchedulingFactoryInfo();
//    }
//
//    public int getSeqNum() {
//        return factoryInstance.getSeqNum();
//    }
//
//    public int getProducingLength() {
//        return factoryInstance.getProducingLength();
//    }
//
//    public int calcRemainProducingQueueSize() {
//        int producingLength = getProducingLength();
//        List<ActionConsequence> factoryConsequence = calcFactoryConsequence();
//        return factoryConsequence.stream()
//                .sorted()
//                .filter(consequence -> consequence.getResource() instanceof ActionConsequence.FactoryWaitQueue)
//                .takeWhile(consequence -> consequence.getLocalDateTime().isBefore(getEnd()))
//                .map(ActionConsequence::getResourceChange)
//                .reduce(
//                        producingLength,
//                        (integer, resourceChange) -> resourceChange.apply(integer),
//                        Integer::sum
//                );
//    }
//
//    public List<ActionConsequence> calcFactoryConsequence() {
//        List<ActionConsequence> actionConsequences = new ArrayList<>();
//        List<List<ActionConsequence>> collectingPreviousConsequences = new ArrayList<>();
//        SchedulingFactoryTimeSlotInstance iterating = this;
//        SchedulingFactoryTimeSlotInstance previous = null;
//        collectingPreviousConsequences.add(this.calcTimeSlotFactoryConsequence());
//        while (iterating.getPreviousPeriodOfFactory() != null) {
//            previous = iterating.getPreviousPeriodOfFactory();
//            collectingPreviousConsequences.add(previous.calcTimeSlotFactoryConsequence());
//            iterating = previous;
//        }
//
//        Collections.reverse(collectingPreviousConsequences);
//        for (List<ActionConsequence> consequences : collectingPreviousConsequences) {
//            actionConsequences.addAll(consequences);
//        }
//        return actionConsequences;
//    }
//
//    public LocalDateTime getStart() {
//        return dateTimeSlot.getStart();
//    }
//
//    public List<ActionConsequence> calcTimeSlotFactoryConsequence() {
//        return getInversedPlanningActionList().stream()
//                .sorted()
//                .map(SchedulingPlayerFactoryAction::calcActionConsequence)
//                .flatMap(Collection::stream)
//                .toList();
//    }
//
//    public List<ActionConsequence> calcFactoryConsequenceBefore(SchedulingPlayerFactoryAction action) {
//        List<ActionConsequence> actionConsequences = new ArrayList<>();
//        List<List<ActionConsequence>> collectingPreviousConsequences = new ArrayList<>();
//        SchedulingFactoryTimeSlotInstance iterating = this;
//        SchedulingFactoryTimeSlotInstance previous = null;
//        while (iterating.getPreviousPeriodOfFactory() != null) {
//            previous = iterating.getPreviousPeriodOfFactory();
//            collectingPreviousConsequences.add(previous.calcTimeSlotFactoryConsequence());
//            iterating = previous;
//        }
//
//        Collections.reverse(collectingPreviousConsequences);
//        for (List<ActionConsequence> consequences : collectingPreviousConsequences) {
//            actionConsequences.addAll(consequences);
//        }
//        List<ActionConsequence> thisBeforeActionConsequences
//                = getInversedPlanningActionList().stream()
//                .takeWhile(iteratingAction -> iteratingAction != action)
//                .sorted()
//                .map(SchedulingPlayerFactoryAction::calcActionConsequence)
//                .flatMap(Collection::stream)
//                .filter(consequence -> consequence.getResource().getRoot() == this.getFactoryInstance())
//                .toList();
//        actionConsequences.addAll(thisBeforeActionConsequences);
//        return actionConsequences;
//    }
//
//    public boolean boolAffectByAction(@NonNull SchedulingPlayerFactoryAction schedulingPlayerFactoryAction) {
//        LocalDateTime arrangeStart = schedulingPlayerFactoryAction.getArrangeDateTimeSlot().getStart();
////        LocalDateTime arrangeEnd = schedulingPlayerFactoryAction.getArrangeDateTimeSlot().getEnd();
//        LocalDateTime producingDataTime = schedulingPlayerFactoryAction.getShadowGameProducingDataTime();
//        LocalDateTime completeDateTime = schedulingPlayerFactoryAction.getShadowGameCompleteDateTime();
//        if (producingDataTime == null || completeDateTime == null) {
//            return false;
//        }
//
//        Assert.isTrue(
//                producingDataTime.isBefore(completeDateTime),
//                "producingDataTime should before completeDateTime "
//        );
//
//        boolean arrangeIsBeforeThis
//                = arrangeStart.isBefore(getStart());
//
//        boolean exceptAffectingFollowing
//                = producingDataTime.isAfter(getStart())
//                  && (completeDateTime.isAfter(getStart()) || completeDateTime.isEqual(getStart()));
//
//        return arrangeIsBeforeThis && !exceptAffectingFollowing;
//    }
//
//    public LocalDateTime getEnd() {
//        return dateTimeSlot.getEnd();
//    }
//
//    public LocalDateTime calcProducingDateTime(SchedulingPlayerFactoryAction factoryAction) {
//        if (!inversedPlanningActionList.contains(factoryAction)) {
//            if (factoryAction.getAffectFollowingFactories().contains(this)) {
//                SchedulingFactoryTimeSlotInstance thatTimeSlotFactory
//                        = factoryAction.getAffectFollowingFactories()
//                        .getFirst()
//                        .getPreviousPeriodOfFactory();
//
//                if (thatTimeSlotFactory.getProducingStructureType() == ProducingStructureType.SLOT) {
//                    return thatTimeSlotFactory.getStart();
//                } else {
//                    LocalDateTime thatStart = thatTimeSlotFactory.getStart();
//                    return thatTimeSlotFactory.getInversedPlanningActionList()
//                            .stream()
//                            .sorted()
//                            .map(SchedulingPlayerFactoryAction::getActionDuration)
//                            .reduce(Duration::plus)
//                            .map(thatStart::plus)
//                            .orElse(thatStart);
//                }
//
//            } else {
//                return null;
//            }
//        }
//
//        if (getProducingStructureType() == ProducingStructureType.SLOT) {
//            return this.getStart();
//        }
//
//        LocalDateTime result = getStart();
//        return getInversedPlanningActionList().stream()
//                .sorted()
//                .takeWhile(iteratingAction -> iteratingAction.compareTo(factoryAction) < 0)
//                .map(SchedulingPlayerFactoryAction::getActionDuration)
//                .reduce(Duration::plus)
//                .map(result::plus)
//                .orElse(result);
//    }
//
//    public ProducingStructureType getProducingStructureType() {
//        return getFactoryInstance().getProducingStructureType();
//    }
//
//}
