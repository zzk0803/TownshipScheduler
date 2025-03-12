package zzk.townshipscheduler.backend.scheduling.model.utility;

import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import org.jspecify.annotations.NonNull;
import zzk.townshipscheduler.backend.ProducingStructureType;
import zzk.townshipscheduler.backend.scheduling.model.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

public class ProducingFactoryArrangementComputedDateTimeVariableListener
        implements VariableListener<TownshipSchedulingProblem, IActionSensitive> {

    @Override
    public void beforeVariableChanged(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull IActionSensitive objectOrAction
    ) {

    }

    @Override
    public void afterVariableChanged(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull IActionSensitive objectOrAction
    ) {
        doUpdate(scoreDirector, objectOrAction);
    }

    private void doUpdate(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            IActionSensitive objectOrAction
    ) {
        if (!(objectOrAction instanceof SchedulingPlayerFactoryProducingArrangement factoryAction)) {
            return;
        }
        SchedulingDateTimeSlot planningDateTimeSlot = factoryAction.getPlanningDateTimeSlot();
        Integer planningSequence = factoryAction.getPlanningSequence();
        SchedulingFactoryInstanceMultiple planningFactory = factoryAction.getFactory();

        if (
                Objects.isNull(planningFactory)
                || Objects.isNull(planningDateTimeSlot)
                || Objects.isNull(planningSequence)
        ) {
            resetShadowVariables(scoreDirector, factoryAction);
            return;
        }

        LocalDateTime planningDateTimeSlotStart
                = planningDateTimeSlot.getStart();

        List<SchedulingPlayerFactoryProducingArrangement> sortedFactoryActions
                = streamActionWithSort(planningFactory)
                .toList();

        if (sortedFactoryActions.isEmpty()) {
            updateFirstAction(scoreDirector, factoryAction, planningDateTimeSlotStart);
        } else {
            updateSubsequentActions(scoreDirector, factoryAction, planningFactory, planningDateTimeSlotStart);
        }
    }

    private void resetShadowVariables(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            SchedulingPlayerFactoryProducingArrangement factoryAction
    ) {
        if (Objects.nonNull(factoryAction.getShadowGameProducingDataTime())) {
            updateShadowVariable(
                    scoreDirector,
                    factoryAction,
                    SchedulingPlayerProducingArrangement.SHADOW_PRODUCING_DATE_TIME,
                    null
            );
        }
    }

    private Stream<SchedulingPlayerFactoryProducingArrangement> streamActionWithSort(SchedulingFactoryInstanceMultiple schedulingFactoryInstanceMultiple) {
        return schedulingFactoryInstanceMultiple.getPlanningFactoryActionList()
                .stream()
                .filter(producingArrangement -> producingArrangement.getPlanningDateTimeSlot() != null)
                .filter(producingArrangement -> producingArrangement.getPlanningSequence() != null)
                .sorted(Comparator.comparing(AbstractPlayerProducingArrangement::getPlanningDateTimeSlotStartAsLocalDateTime));
    }

    private void updateFirstAction(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            SchedulingPlayerFactoryProducingArrangement factoryAction,
            LocalDateTime planningDateTimeSlotStart
    ) {
        updateShadowVariable(
                scoreDirector,
                factoryAction,
                SchedulingPlayerProducingArrangement.SHADOW_PRODUCING_DATE_TIME,
                planningDateTimeSlotStart
        );
    }

    private void updateSubsequentActions(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            SchedulingPlayerFactoryProducingArrangement factoryAction,
            SchedulingFactoryInstanceMultiple planningFactory,
            LocalDateTime planningDateTimeSlotStart
    ) {
        ProducingStructureType factoryProducingStructureType
                = planningFactory.getProducingStructureType();

        Optional<LocalDateTime> lastActionCompletedDateTime
                = getLastActionCompletedDateTime(
                planningFactory,
                factoryAction
        );

        LocalDateTime computedFactoryActionProducingDateTime
                = lastActionCompletedDateTime.orElse(planningDateTimeSlotStart);

        if (
                factoryProducingStructureType == ProducingStructureType.QUEUE
                && computedFactoryActionProducingDateTime.isBefore(planningDateTimeSlotStart)
        ) {
            computedFactoryActionProducingDateTime = planningDateTimeSlotStart;
        }

        updateShadowVariable(
                scoreDirector,
                factoryAction,
                SchedulingPlayerProducingArrangement.SHADOW_PRODUCING_DATE_TIME,
                computedFactoryActionProducingDateTime
        );

        updateFollowingActions(scoreDirector, planningFactory, factoryAction);
    }

    private void updateShadowVariable(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            SchedulingPlayerFactoryProducingArrangement factoryAction,
            String variableName,
            LocalDateTime value
    ) {
        scoreDirector.beforeVariableChanged(factoryAction, variableName);
        factoryAction.setShadowGameProducingDataTime(value);
        scoreDirector.afterVariableChanged(factoryAction, variableName);
    }

    private Optional<LocalDateTime> getLastActionCompletedDateTime(
            SchedulingFactoryInstanceMultiple planningFactory,
            SchedulingPlayerFactoryProducingArrangement factoryAction
    ) {
        return streamActionBeforeArgument(planningFactory, factoryAction)
                .max(Comparator.comparing(SchedulingPlayerFactoryProducingArrangement::getPlanningSequence))
                .map(SchedulingPlayerFactoryProducingArrangement::getShadowGameCompleteDateTime);
    }

    private void updateFollowingActions(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            SchedulingFactoryInstanceMultiple planningFactory,
            SchedulingPlayerFactoryProducingArrangement factoryAction
    ) {
        List<SchedulingPlayerFactoryProducingArrangement> followingActionList
                = streamActionAfterArgument(
                planningFactory,
                factoryAction
        ).toList();
        Iterator<SchedulingPlayerFactoryProducingArrangement> followingIterator = followingActionList.iterator();

        LocalDateTime previousCompletedDateTime
                = factoryAction.getShadowGameCompleteDateTime();

        while (followingIterator.hasNext()) {
            SchedulingPlayerFactoryProducingArrangement iteratingAction = followingIterator.next();
            LocalDateTime iteratingActionPlanningDateTime
                    = iteratingAction.getPlanningDateTimeSlotStartAsLocalDateTime();

            LocalDateTime producingDateTime
                    = previousCompletedDateTime.isAfter(iteratingActionPlanningDateTime)
                    ? previousCompletedDateTime
                    : iteratingActionPlanningDateTime;

            updateShadowVariable(
                    scoreDirector,
                    iteratingAction,
                    SchedulingPlayerProducingArrangement.SHADOW_PRODUCING_DATE_TIME,
                    producingDateTime
            );

            previousCompletedDateTime = iteratingAction.getShadowGameCompleteDateTime();
        }
    }

    Stream<SchedulingPlayerFactoryProducingArrangement> streamActionBeforeArgument(
            SchedulingFactoryInstanceMultiple schedulingFactoryInstance,
            SchedulingPlayerFactoryProducingArrangement factoryAction
    ) {
        return streamActionWithSort(schedulingFactoryInstance)
                .takeWhile(iteratingAction -> {
                            LocalDateTime iteratingDateTime = iteratingAction.getPlanningDateTimeSlotStartAsLocalDateTime();
                            LocalDateTime factoryActionDateTime = factoryAction.getPlanningDateTimeSlotStartAsLocalDateTime();
                            return !iteratingDateTime.isAfter(factoryActionDateTime);
                        }
                )
                .sorted(Comparator.comparing(SchedulingPlayerFactoryProducingArrangement::getActionId))
                .takeWhile(iteratingAction ->
                        iteratingAction.getActionId()
                                .compareTo(factoryAction.getActionId()) < 0);
    }

    Stream<SchedulingPlayerFactoryProducingArrangement> streamActionAfterArgument(
            SchedulingFactoryInstanceMultiple schedulingFactoryInstance,
            SchedulingPlayerFactoryProducingArrangement factoryAction
    ) {
        return streamActionWithSort(schedulingFactoryInstance)
                .dropWhile(iteratingAction ->
                        {
                            LocalDateTime iteratingDateTime = iteratingAction.getPlanningDateTimeSlotStartAsLocalDateTime();
                            LocalDateTime factoryActionDateTime = factoryAction.getPlanningDateTimeSlotStartAsLocalDateTime();
                            return !iteratingDateTime.isAfter(factoryActionDateTime);
                        }
                )
                .sorted(Comparator.comparing(SchedulingPlayerFactoryProducingArrangement::getActionId))
                .takeWhile(iteratingAction ->
                        iteratingAction.getActionId()
                                .compareTo(factoryAction.getActionId()) <= 0);
    }

    @Override
    public void beforeEntityAdded(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull IActionSensitive objectOrAction
    ) {

    }

    @Override
    public void afterEntityAdded(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull IActionSensitive objectOrAction
    ) {
        doUpdate(scoreDirector, objectOrAction);
    }

    @Override
    public void beforeEntityRemoved(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull IActionSensitive objectOrAction
    ) {

    }

    @Override
    public void afterEntityRemoved(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull IActionSensitive objectOrAction
    ) {

    }

}
