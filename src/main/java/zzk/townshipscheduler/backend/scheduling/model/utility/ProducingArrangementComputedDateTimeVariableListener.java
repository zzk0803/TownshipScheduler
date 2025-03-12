package zzk.townshipscheduler.backend.scheduling.model.utility;

import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import org.jspecify.annotations.NonNull;
import zzk.townshipscheduler.backend.ProducingStructureType;
import zzk.townshipscheduler.backend.scheduling.model.*;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Stream;

public class ProducingArrangementComputedDateTimeVariableListener
        implements VariableListener<TownshipSchedulingProblem, IActionSensitive> {

    @Override
    public void beforeEntityAdded(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull IActionSensitive actionOrFactory
    ) {

    }

    @Override
    public void afterEntityAdded(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull IActionSensitive actionOrFactory
    ) {
        if (!(actionOrFactory instanceof SchedulingPlayerProducingArrangement factoryAction)) {
            return;
        }
        newDoUpdate(scoreDirector, factoryAction);
    }

    @Override
    public void beforeEntityRemoved(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull IActionSensitive actionOrFactory
    ) {

    }

    @Override
    public void afterEntityRemoved(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull IActionSensitive actionOrFactory
    ) {
        if (!(actionOrFactory instanceof SchedulingPlayerProducingArrangement factoryAction)) {
            return;
        }
        newDoUpdate(scoreDirector, factoryAction);
    }

    private void newDoUpdate(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            SchedulingPlayerProducingArrangement factoryAction
    ) {

        SchedulingFactoryInstanceSingle planningFactory
                = factoryAction.getFactory();

        List<SchedulingPlayerProducingArrangement> sortedFilteredActions
                = streamActionWithSort(
                fetchSameFactoryActionFromScoreDirector(
                        scoreDirector, planningFactory
                )
        ).toList();

        if (sortedFilteredActions.isEmpty()) {
            LocalDateTime computedProducingDateTime = null;
            SchedulingDateTimeSlot planningDateTimeSlot = factoryAction.getPlanningDateTimeSlot();
            if (planningDateTimeSlot != null) {
                computedProducingDateTime = planningDateTimeSlot.getStart();
            }
            updateShadowGameProducingDataTime(
                    scoreDirector,
                    factoryAction,
                    computedProducingDateTime
            );
        } else {
            ListIterator<SchedulingPlayerProducingArrangement> arrangementsIterator = sortedFilteredActions.listIterator();
            SchedulingPlayerProducingArrangement iteratingArrangement = null;
            SchedulingPlayerProducingArrangement previousArrangement = null;
            while (arrangementsIterator.hasNext()) {
                iteratingArrangement = arrangementsIterator.next();
                if (arrangementsIterator.hasPrevious()) {
                    previousArrangement = arrangementsIterator.previous();
                }
                LocalDateTime mayNullIteratingProducingDateTime = iteratingArrangement.getShadowGameProducingDataTime();
                LocalDateTime computedProducingDateTime = computeProducingDateTime(
                        iteratingArrangement,
                        previousArrangement,
                        planningFactory
                );
                if (mayNullIteratingProducingDateTime != null
                    && !computedProducingDateTime.isEqual(mayNullIteratingProducingDateTime
                )) {
                    updateShadowGameProducingDataTime(scoreDirector, iteratingArrangement, computedProducingDateTime);
                }
            }
        }
    }

    Stream<SchedulingPlayerProducingArrangement> streamActionWithSort(List<SchedulingPlayerProducingArrangement> schedulingPlayerProducingArrangements) {
        return schedulingPlayerProducingArrangements
                .stream()
                .filter(producingArrangement -> producingArrangement.getPlanningSequence() != null)
                .filter(producingArrangement -> producingArrangement.getPlanningDateTimeSlot() != null)
                .sorted(
                        Comparator.comparing(AbstractPlayerProducingArrangement::getPlanningDateTimeSlotStartAsLocalDateTime)
                                .thenComparingInt(AbstractPlayerProducingArrangement::getPlanningSequence)
                );
    }

    private List<SchedulingPlayerProducingArrangement> fetchSameFactoryActionFromScoreDirector(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull SchedulingFactoryInstanceSingle factoryInstance
    ) {
        return scoreDirector.getWorkingSolution().getSchedulingPlayerProducingArrangement(factoryInstance);
    }

    private LocalDateTime computeProducingDateTime(
            SchedulingPlayerProducingArrangement iteratingArrangement,
            SchedulingPlayerProducingArrangement previousArrangement,
            SchedulingFactoryInstanceSingle planningFactory
    ) {
        SchedulingDateTimeSlot planningDateTimeSlot = iteratingArrangement.getPlanningDateTimeSlot();
        LocalDateTime computedProducingDateTime = planningDateTimeSlot.getStart();
        if (previousArrangement != null) {
            LocalDateTime previousCompleteDateTime = previousArrangement.getShadowGameCompleteDateTime();
            if (previousCompleteDateTime != null) {
                computedProducingDateTime = previousCompleteDateTime.isBefore(computedProducingDateTime)
                        ? computedProducingDateTime
                        : previousCompleteDateTime;
            }
        }
        if (planningFactory.getProducingStructureType() == ProducingStructureType.SLOT) {
            computedProducingDateTime = planningDateTimeSlot.getStart();
        }
        return computedProducingDateTime;
    }

    private void updateShadowGameProducingDataTime(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            SchedulingPlayerProducingArrangement factoryAction,
            LocalDateTime localDateTime
    ) {
        scoreDirector.beforeVariableChanged(
                factoryAction,
                AbstractPlayerProducingArrangement.SHADOW_PRODUCING_DATE_TIME
        );
        factoryAction.setShadowGameProducingDataTime(localDateTime);
        scoreDirector.afterVariableChanged(
                factoryAction,
                AbstractPlayerProducingArrangement.SHADOW_PRODUCING_DATE_TIME
        );
    }

    Stream<SchedulingPlayerProducingArrangement> streamActionBeforeArgument(
            List<SchedulingPlayerProducingArrangement> factoryActions,
            SchedulingPlayerProducingArrangement factoryAction
    ) {
        return streamActionWithSort(factoryActions)
                .takeWhile(iteratingAction -> {
                            LocalDateTime iteratingDateTime = iteratingAction.getPlanningDateTimeSlotStartAsLocalDateTime();
                            LocalDateTime factoryActionDateTime = factoryAction.getPlanningDateTimeSlotStartAsLocalDateTime();
                            return !iteratingDateTime.isAfter(factoryActionDateTime);
                        }
                )
                .sorted(Comparator.comparing(SchedulingPlayerProducingArrangement::getPlanningSequence))
//                .sorted(Comparator.comparing(SchedulingPlayerFactoryAction::getPlanningSequence))
                .takeWhile(iteratingAction -> {
                    Integer iteratingActionPlanningSequence = iteratingAction.getPlanningSequence();
                    Integer factoryActionPlanningSequence = factoryAction.getPlanningSequence();
                    return iteratingActionPlanningSequence.compareTo(factoryActionPlanningSequence) < 0;
                });
    }

    Stream<SchedulingPlayerProducingArrangement> streamActionAfterArgument(
            List<SchedulingPlayerProducingArrangement> factoryActions,
            SchedulingPlayerProducingArrangement factoryAction
    ) {
        return streamActionWithSort(factoryActions)
                .dropWhile(iteratingAction ->
                        {
                            LocalDateTime iteratingDateTime = iteratingAction.getPlanningDateTimeSlotStartAsLocalDateTime();
                            LocalDateTime factoryActionDateTime = factoryAction.getPlanningDateTimeSlotStartAsLocalDateTime();
                            return !iteratingDateTime.isAfter(factoryActionDateTime);
                        }
                )
//                .sorted(Comparator.comparing(SchedulingPlayerFactoryAction::getPlanningSequence))
                .sorted(Comparator.comparing(SchedulingPlayerProducingArrangement::getPlanningSequence))
                .takeWhile(iteratingAction ->
                        iteratingAction.getPlanningSequence()
                                .compareTo(factoryAction.getPlanningSequence()) <= 0);
    }

    @Override
    public void beforeVariableChanged(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull IActionSensitive actionOrFactory
    ) {

    }

    @Override
    public void afterVariableChanged(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull IActionSensitive actionOrFactory
    ) {
        if (!(actionOrFactory instanceof SchedulingPlayerProducingArrangement factoryAction)) {
            return;
        }
        newDoUpdate(scoreDirector, factoryAction);
    }

}
