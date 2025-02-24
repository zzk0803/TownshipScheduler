package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.solution.cloner.DeepPlanningClone;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import zzk.townshipscheduler.backend.ProducingStructureType;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@PlanningEntity
public class SchedulingFactoryInstance {

    @PlanningId
    @EqualsAndHashCode.Include
    private Integer id;

    private SchedulingFactoryInfo schedulingFactoryInfo;

    private int seqNum;

    private int producingLength;

    private int reapWindowSize;

    @PlanningPin
    private boolean pinIfOnlyOneVariable;

    @DeepPlanningClone
    @InverseRelationShadowVariable(sourceVariableName = "planningFactory")
    private List<SchedulingPlayerFactoryAction> inversedPlanningActionList = new CopyOnWriteArrayList<>();

//    @ShadowVariable(
//            sourceVariableName = "inversedPlanningActionList",
//            variableListenerClass = FactoryInstanceComputeProducingMapVariableListener.class
//    )
    private SortedMap<SchedulingPlayerFactoryAction, LocalDateTime> actionProducingMap
            = new TreeMap<>(
            Comparator.comparing(SchedulingPlayerFactoryAction::getPlanningPlayerArrangeDateTime)
                    .thenComparingInt(SchedulingPlayerFactoryAction::getPlanningSequence)
    );
//
//    @PiggybackShadowVariable(shadowVariableName = "actionProducingMap")
    private SortedMap<SchedulingPlayerFactoryAction, LocalDateTime> actionCompletedMap
            = new TreeMap<>(
            Comparator.comparing(SchedulingPlayerFactoryAction::getPlanningPlayerArrangeDateTime)
                    .thenComparingInt(SchedulingPlayerFactoryAction::getPlanningSequence)
    );

    @Override
    public String toString() {
        return this.schedulingFactoryInfo.getCategoryName() + "#" + this.getSeqNum() + ",size=" + this.getProducingLength();
    }

    public int calcRemainProducingQueueSize(LocalDateTime localDateTime) {
        int producingLength = getProducingLength();
        List<ActionConsequence> factoryConsequence = calcFactoryConsequence();
        return factoryConsequence.stream()
                .sorted()
                .filter(consequence -> consequence.getResource() instanceof ActionConsequence.FactoryProducingQueue)
                .filter(consequence -> consequence.getResource().getRoot() == this)
                .takeWhile(consequence -> {
                    boolean before = consequence.getLocalDateTime().isBefore(localDateTime);
                    boolean equal = consequence.getLocalDateTime().isEqual(localDateTime);
                    return before || equal;
                })
                .map(ActionConsequence::getResourceChange)
                .reduce(
                        producingLength,
                        (integer, resourceChange) -> resourceChange.apply(integer),
                        Integer::sum
                );
    }

    public List<ActionConsequence> calcFactoryConsequence() {
        return getInversedPlanningActionList().parallelStream()
                .sorted(Comparator.comparing(SchedulingPlayerFactoryAction::getPlanningPlayerArrangeDateTime)
                        .thenComparing(SchedulingPlayerFactoryAction::getPlanningSequence))
                .map(SchedulingPlayerFactoryAction::calcActionConsequence)
                .flatMap(Collection::stream)
                .filter(consequence -> consequence.getResource().getRoot() == this)
                .toList();
    }

    public LocalDateTime calcProducingDateTime(SchedulingPlayerFactoryAction factoryAction) {
        //        if (!inversedPlanningActionList.contains(factoryAction)) {
        //            return null;
        //        }

        if (getProducingStructureType() == ProducingStructureType.SLOT) {
            return factoryAction.getPlanningPlayerArrangeDateTime();
        }

        if (getActionProducingMap().containsKey(factoryAction)) {
            return getActionProducingMap().get(factoryAction);
        }

        LocalDateTime actionProducingDateTime = factoryAction.getPlanningPlayerArrangeDateTime();
        Duration accumulatedDelayDuration = getInversedPlanningActionList()
                .stream()
                .sorted(
                        Comparator.comparing(SchedulingPlayerFactoryAction::getPlanningPlayerArrangeDateTime)
                )
                .takeWhile(iteratingAction -> {
                    LocalDateTime inversedActionArrangeDateTime = iteratingAction.getPlanningPlayerArrangeDateTime();
                    LocalDateTime argumentActionArrangeDateTime = factoryAction.getPlanningPlayerArrangeDateTime();
                    return inversedActionArrangeDateTime.isBefore(argumentActionArrangeDateTime);
                })
                .sorted(Comparator.comparing(SchedulingPlayerFactoryAction::getPlanningSequence))
                .map(SchedulingPlayerFactoryAction::getProducingDuration)
                .reduce(Duration::plus)
                .orElse(Duration.ZERO);
        return actionProducingDateTime.plus(accumulatedDelayDuration);
    }

    public ProducingStructureType getProducingStructureType() {
        return schedulingFactoryInfo.getProducingStructureType();
    }

    public void setProducingDateTimeForAction(SchedulingPlayerFactoryAction schedulingPlayerFactoryAction) {
        schedulingPlayerFactoryAction.setShadowGameProducingDataTime(getActionProducingMap().get(
                schedulingPlayerFactoryAction));
    }

    public void setCompletedDateTimeForAction(SchedulingPlayerFactoryAction schedulingPlayerFactoryAction) {
        schedulingPlayerFactoryAction.setShadowGameCompleteDateTime(getActionCompletedMap().get(
                schedulingPlayerFactoryAction));
    }

}
