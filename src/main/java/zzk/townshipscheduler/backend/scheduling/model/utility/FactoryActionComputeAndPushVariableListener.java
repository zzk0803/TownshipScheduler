package zzk.townshipscheduler.backend.scheduling.model.utility;

import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import org.jspecify.annotations.NonNull;
import zzk.townshipscheduler.backend.ProducingStructureType;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInstance;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingPlayerFactoryAction;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class FactoryActionComputeAndPushVariableListener
        implements VariableListener<TownshipSchedulingProblem, SchedulingPlayerFactoryAction> {

    @Override
    public void beforeEntityAdded(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull SchedulingPlayerFactoryAction factoryAction
    ) {

    }

    @Override
    public void afterEntityAdded(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull SchedulingPlayerFactoryAction factoryAction
    ) {
        doUpdate(scoreDirector, factoryAction);
    }

    @Override
    public void beforeEntityRemoved(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull SchedulingPlayerFactoryAction factoryAction
    ) {

    }

    @Override
    public void afterEntityRemoved(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull SchedulingPlayerFactoryAction factoryAction
    ) {
        doUpdate(scoreDirector, factoryAction);
    }

    private void doUpdate(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull SchedulingPlayerFactoryAction factoryAction
    ) {

        SchedulingFactoryInstance planningFactory = factoryAction.getPlanningFactory();
        LocalDateTime playerArrangeDateTime = factoryAction.getPlanningPlayerArrangeDateTime();
        Integer planningSequence = factoryAction.getPlanningSequence();
        if (
                Objects.isNull(planningFactory) ||
                Objects.isNull(playerArrangeDateTime) ||
                Objects.isNull(planningSequence)
        ) {
            return;
        }

        doComputeAndPush(planningFactory);
        scoreDirector.beforeVariableChanged(factoryAction, "shadowRollingChange");
        factoryAction.setShadowRollingChange(factoryAction.getShadowRollingChange() + 1L);
        scoreDirector.afterVariableChanged(factoryAction, "shadowRollingChange");

    }

    public void doComputeAndPush(
            SchedulingFactoryInstance schedulingFactoryInstance
    ) {
        List<SchedulingPlayerFactoryAction> sortedActionList
                = schedulingFactoryInstance.getPlanningFactoryActionList()
                .stream()
                .sorted(Comparator.comparing(SchedulingPlayerFactoryAction::getPlanningDateTimeSlot))
                .sorted(Comparator.comparing(SchedulingPlayerFactoryAction::getPlanningSequence))
                .toList();

        SortedMap<SchedulingPlayerFactoryAction, LocalDateTime> actionProducingMap
                = new TreeMap<>(
                Comparator.comparing(SchedulingPlayerFactoryAction::getPlanningDateTimeSlot)
                        .thenComparingInt(SchedulingPlayerFactoryAction::getPlanningSequence)
        );

        SortedMap<SchedulingPlayerFactoryAction, LocalDateTime> actionCompletedMap
                = new TreeMap<>(
                Comparator.comparing(SchedulingPlayerFactoryAction::getPlanningDateTimeSlot)
                        .thenComparingInt(SchedulingPlayerFactoryAction::getPlanningSequence)
        );


        Iterator<SchedulingPlayerFactoryAction> actionIterator = sortedActionList.iterator();
        SchedulingPlayerFactoryAction firstAction = null;
        Duration firstActionDuration = null;
        SchedulingPlayerFactoryAction previousAction = null;
        Duration previousActionDuration = null;
        SchedulingPlayerFactoryAction iteratingAction = null;
        Duration iteratingActionDuration = null;
        if (actionIterator.hasNext()) {
            firstAction = sortedActionList.getFirst();
            firstActionDuration = firstAction.getProducingDuration();
        }
        while (actionIterator.hasNext()) {
            iteratingAction = actionIterator.next();
            iteratingActionDuration = iteratingAction.getProducingDuration();
            if (iteratingAction == firstAction) {
                LocalDateTime firstActionProducingDateTime = iteratingAction.getPlanningDateTimeSlot().getStart();
                actionProducingMap.put(iteratingAction, firstActionProducingDateTime);
                LocalDateTime firstActionCompletedDateTime = firstActionProducingDateTime.plus(firstActionDuration);
                actionCompletedMap.put(iteratingAction, firstActionCompletedDateTime);
                iteratingAction.acceptComputedDateTime(
                         firstActionProducingDateTime,
                         firstActionCompletedDateTime
                );
                iteratingAction.getSchedulingWarehouse().acceptActionConsequence(
                        iteratingAction,iteratingAction.calcActionConsequence()
                );
            } else {
                LocalDateTime arrangeDateTime
                        = iteratingAction.getPlanningDateTimeSlot().getStart();
                LocalDateTime previousActionCompletedDateTime
                        = actionCompletedMap.get(previousAction);
                LocalDateTime computedProducingDateTime
                        = getComputedProducingDateTime(
                        schedulingFactoryInstance,
                        arrangeDateTime,
                        previousActionCompletedDateTime
                );
                actionProducingMap.put(iteratingAction, computedProducingDateTime);
                LocalDateTime computedCompletedDateTime = computedProducingDateTime.plus(iteratingActionDuration);
                actionCompletedMap.put(iteratingAction, computedCompletedDateTime);

                iteratingAction.acceptComputedDateTime(
                        computedProducingDateTime,
                        computedCompletedDateTime
                );
                iteratingAction.getSchedulingWarehouse().acceptActionConsequence(
                        iteratingAction,iteratingAction.calcActionConsequence()
                );
            }
            previousAction = iteratingAction;
            previousActionDuration = iteratingActionDuration;
        }
    }

    private static LocalDateTime getComputedProducingDateTime(
            SchedulingFactoryInstance factoryAction,
            LocalDateTime arrangeDateTime,
            LocalDateTime previousActionCompletedDateTime
    ) {
        LocalDateTime computedProducingDateTime;
        if (factoryAction.getProducingStructureType() == ProducingStructureType.SLOT) {
            computedProducingDateTime = arrangeDateTime;
        } else {
            computedProducingDateTime =
                    previousActionCompletedDateTime == null
                    || previousActionCompletedDateTime.isBefore(arrangeDateTime)
                            ? arrangeDateTime
                            : previousActionCompletedDateTime;
        }
        return computedProducingDateTime;
    }

    //    @Override
//    public boolean requiresUniqueEntityEvents() {
//        return true;
//    }

    @Override
    public void beforeVariableChanged(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull SchedulingPlayerFactoryAction factoryAction
    ) {

    }

    @Override
    public void afterVariableChanged(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull SchedulingPlayerFactoryAction factoryAction
    ) {
        doUpdate(scoreDirector, factoryAction);
    }


}
