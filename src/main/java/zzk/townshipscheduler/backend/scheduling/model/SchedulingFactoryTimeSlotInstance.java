package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jspecify.annotations.NonNull;
import org.springframework.util.Assert;
import zzk.townshipscheduler.backend.ProducingStructureType;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@PlanningEntity
public class SchedulingFactoryTimeSlotInstance {

    @EqualsAndHashCode.Include
    @PlanningId
    private Integer id;

    private SchedulingDateTimeSlot dateTimeSlot;

    private SchedulingFactoryInstance factoryInstance;

    private SchedulingFactoryTimeSlotInstance previousPeriodOfFactory;

    private SchedulingFactoryTimeSlotInstance nextPeriodOfFactory;

    @InverseRelationShadowVariable(sourceVariableName = "planningTimeSlotFactory")
    private List<SchedulingPlayerFactoryAction> inversedPlanningActionList = new ArrayList<>();

//    @ShadowVariable(
//            variableListenerClass = FactoryActionShadowProducingDateTimeTableVariableListener.class,
//            sourceVariableName = "inversedPlanningActionList"
//    )
//    private Map<SchedulingPlayerFactoryAction, LocalDateTime> shadowProducingDateTimeTable = new LinkedHashMap<>();

    @Override
    public String toString() {
        return this.getSchedulingFactoryInfo().getCategoryName() + "#" + this.getSeqNum()
               + ",size=" + this.getProducingLength()
               + ",(start,end)=" + "(" + getDateTimeSlot().getStart() + "," + getDateTimeSlot().getEnd() + ")";
    }

    public SchedulingFactoryInfo getSchedulingFactoryInfo() {
        return factoryInstance.getSchedulingFactoryInfo();
    }

    public int getSeqNum() {
        return factoryInstance.getSeqNum();
    }

    public int getProducingLength() {
        return factoryInstance.getProducingLength();
    }

    public int calcRemainProducingQueueSize() {
        int producingLength = getProducingLength();
        List<ActionConsequence> factoryConsequence = calcFactoryConsequence();
        return factoryConsequence.stream()
                .sorted()
                .filter(consequence -> consequence.getResource() instanceof ActionConsequence.FactoryWaitQueue)
                .map(ActionConsequence::getResourceChange)
                .reduce(
                        producingLength,
                        (integer, resourceChange) -> resourceChange.apply(integer),
                        Integer::sum
                );
    }

    public List<ActionConsequence> calcFactoryConsequenceBefore(SchedulingPlayerFactoryAction action) {
        if (!this.getInversedPlanningActionList().contains(action)) {
            throw new IllegalArgumentException("action not in this factory happen");
        }
        List<ActionConsequence> actionConsequences = new ArrayList<>();
        List<List<ActionConsequence>> collectingPreviousConsequences = new ArrayList<>();
        SchedulingFactoryTimeSlotInstance iterating = this;
        SchedulingFactoryTimeSlotInstance previous = null;
        while (iterating.getPreviousPeriodOfFactory() != null) {
            previous = iterating.getPreviousPeriodOfFactory();
            collectingPreviousConsequences.add(previous.calcTimeSlotFactoryConsequence());
            iterating = previous;
        }

        Collections.reverse(collectingPreviousConsequences);
        for (List<ActionConsequence> consequences : collectingPreviousConsequences) {
            actionConsequences.addAll(consequences);
        }
        List<ActionConsequence> thisBeforeActionConsequences = this.inversedPlanningActionList.stream()
                .takeWhile(inversedAction -> inversedAction != action)
                .map(SchedulingPlayerFactoryAction::calcActionConsequence)
                .flatMap(Collection::stream)
                .toList();
        actionConsequences.addAll(thisBeforeActionConsequences);
        return actionConsequences;
    }
    public List<ActionConsequence> calcFactoryConsequence() {
        List<ActionConsequence> actionConsequences = new ArrayList<>();
        List<List<ActionConsequence>> collectingPreviousConsequences = new ArrayList<>();
        SchedulingFactoryTimeSlotInstance iterating = this;
        SchedulingFactoryTimeSlotInstance previous = null;
        collectingPreviousConsequences.add(this.calcTimeSlotFactoryConsequence());
        while (iterating.getPreviousPeriodOfFactory() != null) {
            previous = iterating.getPreviousPeriodOfFactory();
            collectingPreviousConsequences.add(previous.calcTimeSlotFactoryConsequence());
            iterating = previous;
        }

        Collections.reverse(collectingPreviousConsequences);
        for (List<ActionConsequence> consequences : collectingPreviousConsequences) {
            actionConsequences.addAll(consequences);
        }
        return actionConsequences;
    }

    public List<ActionConsequence> calcTimeSlotFactoryConsequence() {
        return getInversedPlanningActionList()
                .stream()
                .sorted()
                .map(SchedulingPlayerFactoryAction::calcActionConsequence)
                .flatMap(Collection::stream)
                .toList();
    }

    public boolean boolAffectByAction(@NonNull SchedulingPlayerFactoryAction schedulingPlayerFactoryAction) {
        SchedulingDateTimeSlot actionArrangeDtSlot = schedulingPlayerFactoryAction.getArrangeDateTimeSlot();
        LocalDateTime producingDataTime = schedulingPlayerFactoryAction.getShadowGameProducingDataTime();
        LocalDateTime completeDateTime = schedulingPlayerFactoryAction.getShadowGameCompleteDateTime();
        if (producingDataTime == null || completeDateTime == null) {
            return false;
        }

        Assert.isTrue(
                producingDataTime.isBefore(completeDateTime),
                "producingDataTime should before completeDateTime "
        );

        boolean arrangeIsBeforeThis = actionArrangeDtSlot.getStart().isBefore(getStart());

        boolean producingDataTimeBeforeOrEqualThisStart
                = producingDataTime.isBefore(getStart()) || producingDataTime.isEqual(getStart());
        boolean completeDateTimeBeforeThisStart
                = completeDateTime.isBefore(getStart());

        if (arrangeIsBeforeThis) {
            return !producingDataTimeBeforeOrEqualThisStart || !completeDateTimeBeforeThisStart;
        } else {
            return false;
        }
    }

    public LocalDateTime getStart() {
        return dateTimeSlot.getStart();
    }

    public LocalDateTime getEnd() {
        return dateTimeSlot.getEnd();
    }

    public LocalDateTime getProducingDateTime(SchedulingPlayerFactoryAction schedulingPlayerFactoryAction) {
        return calcProducingDateTime(schedulingPlayerFactoryAction);
    }

    public LocalDateTime calcProducingDateTime(SchedulingPlayerFactoryAction factoryAction) {
        if (!getInversedPlanningActionList().contains(factoryAction)) {
            return null;
        }

        LocalDateTime resultOrStart = getStart();
        if (getProducingStructureType() == ProducingStructureType.SLOT) {
            return resultOrStart;
        }

        ArrayList<SchedulingPlayerFactoryAction> sortedActionList
                = getInversedPlanningActionList().stream()
                .sorted()
                .collect(Collectors.toCollection(ArrayList::new));

        Optional<Duration> optionalDuration = sortedActionList.stream()
                .takeWhile(iteratingAction -> iteratingAction.compareTo(factoryAction) < 0)
                .map(SchedulingPlayerFactoryAction::getActionDuration)
                .reduce(Duration::plus);
        return optionalDuration.map(resultOrStart::plus).orElse(resultOrStart);
    }

    public ProducingStructureType getProducingStructureType() {
        return getFactoryInstance().getProducingStructureType();
    }

}
