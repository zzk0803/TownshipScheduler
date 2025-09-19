package zzk.townshipscheduler.backend.scheduling.algorithm;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.api.solver.phase.PhaseCommand;
import lombok.extern.slf4j.Slf4j;
import zzk.townshipscheduler.backend.scheduling.model.*;
import zzk.townshipscheduler.backend.scheduling.model.utility.SchedulingProducingArrangementDifficultyComparator;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

@Slf4j
public class TownshipInitiateCustomPhase implements PhaseCommand<TownshipSchedulingProblem> {

    @Override
    public void changeWorkingSolution(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            BooleanSupplier isPhaseTerminated
    ) {
        if (isPhaseTerminated.getAsBoolean()) {
            return;
        }

        TownshipSchedulingProblem workingSolution
                = scoreDirector.getWorkingSolution();
        List<SchedulingDateTimeSlot> dateTimeSlotSetValueRange
                = workingSolution.getSchedulingDateTimeSlots();
        List<SchedulingProducingArrangement> producingArrangements
                = workingSolution.getSchedulingProducingArrangementList();
        List<SchedulingFactoryInstance> queueFactoryInstanceValueRange
                = workingSolution.getSchedulingFactoryInstanceList();

        List<SchedulingDateTimeSlot> sortedDataTimeSlotValueRange = dateTimeSlotSetValueRange.stream()
                .sorted()
                .toList();
        List<SchedulingProducingArrangement> difficultySortedProducingArrangements
                = producingArrangements.stream()
                .sorted(new SchedulingProducingArrangementDifficultyComparator())
                .toList();
        ArrayDeque<SchedulingProducingArrangement> initiatingDeque
                = new ArrayDeque<>(difficultySortedProducingArrangements);

        while (!initiatingDeque.isEmpty()) {
            SchedulingProducingArrangement arrangement = initiatingDeque.removeFirst();
            if (shouldInitiating(arrangement)) {
                setupArrangement(
                        scoreDirector,
                        arrangement,
                        sortedDataTimeSlotValueRange,
                        queueFactoryInstanceValueRange
                );
            }
        }

    }

    private boolean shouldInitiating(SchedulingProducingArrangement arrangement) {
        SchedulingDateTimeSlot planningDateTimeSlot = arrangement.getPlanningDateTimeSlot();
        LocalDateTime arrangeDateTime = arrangement.getArrangeDateTime();
        SchedulingFactoryInstance planningFactoryInstance = arrangement.getPlanningFactoryInstance();
        LocalDateTime producingDateTime = arrangement.getProducingDateTime();
        LocalDateTime completedDateTime = arrangement.getCompletedDateTime();

        return Stream.of(
                planningDateTimeSlot,
                arrangeDateTime,
                planningFactoryInstance,
                producingDateTime,
                completedDateTime
        ).anyMatch(Objects::isNull);
    }

    private void setupArrangement(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            SchedulingProducingArrangement schedulingProducingArrangement,
            List<SchedulingDateTimeSlot> dateTimeSlotSet,
            List<SchedulingFactoryInstance> factoryInstanceList
    ) {
        SchedulingFactoryInstance schedulingFactoryInstance
                = factoryInstanceList.stream()
                .filter(slotFactoryInstance -> schedulingProducingArrangement.getRequiredFactoryInfo()
                        .typeEqual(slotFactoryInstance.getSchedulingFactoryInfo()))
                .findAny()
                .get();
        SchedulingDateTimeSlot computedDataTimeSlot
                = calcApproximateArrangeDateTimeSlot(
                schedulingProducingArrangement,
                dateTimeSlotSet
        );

        scoreDirector.beforeVariableChanged(
                schedulingProducingArrangement,
                SchedulingProducingArrangement.PLANNING_DATA_TIME_SLOT
        );
        schedulingProducingArrangement.setPlanningDateTimeSlot(computedDataTimeSlot);
        scoreDirector.afterVariableChanged(
                schedulingProducingArrangement,
                SchedulingProducingArrangement.PLANNING_DATA_TIME_SLOT
        );
        scoreDirector.triggerVariableListeners();

        int size = schedulingFactoryInstance.getPlanningProducingArrangements().size();
        scoreDirector.beforeListVariableElementAssigned(
                schedulingFactoryInstance,
                SchedulingFactoryInstance.PLANNING_PRODUCING_ARRANGEMENTS,
                schedulingProducingArrangement
        );
        scoreDirector.beforeListVariableChanged(
                schedulingFactoryInstance,
                SchedulingFactoryInstance.PLANNING_PRODUCING_ARRANGEMENTS,
                size,
                size+1
        );
        schedulingProducingArrangement.setPlanningFactoryInstance(schedulingFactoryInstance);
        schedulingFactoryInstance.getPlanningProducingArrangements().add(schedulingProducingArrangement);
        scoreDirector.afterListVariableChanged(
                schedulingFactoryInstance,
                SchedulingFactoryInstance.PLANNING_PRODUCING_ARRANGEMENTS,
                size,
                size+1
        );
        scoreDirector.afterListVariableElementAssigned(
                schedulingFactoryInstance,
                SchedulingFactoryInstance.PLANNING_PRODUCING_ARRANGEMENTS,
                schedulingProducingArrangement
        );

        scoreDirector.triggerVariableListeners();
    }

    private SchedulingDateTimeSlot calcApproximateArrangeDateTimeSlot(
            SchedulingProducingArrangement producingArrangement,
            List<SchedulingDateTimeSlot> dateTimeSlotSet
    ) {

        SchedulingDateTimeSlot result = dateTimeSlotSet.getFirst();
        Duration calcStaticProducingDuration = producingArrangement.calcStaticProducingDuration();

        if (!producingArrangement.getDeepPrerequisiteProducingArrangements().isEmpty()) {
             result =  SchedulingDateTimeSlot.fromRangeJumpCeil(
                    dateTimeSlotSet,
                    result.getStart().plus(calcStaticProducingDuration)
            ).orElse(dateTimeSlotSet.getLast());

        }

        return result;
    }


}
