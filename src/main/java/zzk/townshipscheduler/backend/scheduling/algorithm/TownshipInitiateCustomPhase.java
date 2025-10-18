package zzk.townshipscheduler.backend.scheduling.algorithm;

import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.api.solver.phase.PhaseCommand;
import lombok.extern.slf4j.Slf4j;
import zzk.townshipscheduler.backend.scheduling.model.*;
import zzk.townshipscheduler.backend.scheduling.model.utility.SchedulingProducingArrangementDifficultyComparator;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
        CountableValueRange<LocalDateTime> dateTimeValueRange
                = workingSolution.valueRangeForQueuedDateTime();
        List<SchedulingProducingArrangement> producingArrangements
                = workingSolution.getSchedulingProducingArrangementList();
        List<SchedulingFactoryInstance> queueFactoryInstanceValueRange
                = workingSolution.getSchedulingFactoryInstanceList();


        Spliterator<LocalDateTime> spliterator =
                Spliterators.spliteratorUnknownSize(dateTimeValueRange.createOriginalIterator(), Spliterator.ORDERED);
        StreamSupport.stream(spliterator, false);
        List<LocalDateTime> sortedDataTimeSlotValueRange = StreamSupport.stream(spliterator, false)
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
        LocalDateTime planningDateTime = arrangement.getArrangeDateTime();
        LocalDateTime arrangeDateTime = arrangement.getArrangeDateTime();
        SchedulingFactoryInstance planningFactoryInstance = arrangement.getPlanningFactoryInstance();
        LocalDateTime producingDateTime = arrangement.getProducingDateTime();
        LocalDateTime completedDateTime = arrangement.getCompletedDateTime();

        return Stream.of(
                planningDateTime,
                arrangeDateTime,
                planningFactoryInstance,
                producingDateTime,
                completedDateTime
        ).anyMatch(Objects::isNull);
    }

    private void setupArrangement(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            SchedulingProducingArrangement schedulingProducingArrangement,
            List<LocalDateTime> dateTimeSlotList,
            List<SchedulingFactoryInstance> factoryInstanceList
    ) {
        SchedulingFactoryInstance schedulingFactoryInstance
                = factoryInstanceList.stream()
                .filter(slotFactoryInstance -> schedulingProducingArrangement.getRequiredFactoryInfo()
                        .typeEqual(slotFactoryInstance.getSchedulingFactoryInfo()))
                .findAny()
                .get();
        LocalDateTime computedDataTime
                = calcApproximateArrangeDateTimeSlot(
                schedulingProducingArrangement,
                dateTimeSlotList
        );

        scoreDirector.beforeVariableChanged(
                schedulingProducingArrangement,
                SchedulingProducingArrangement.PLANNING_QUEUED_DATA_TIME
        );
        schedulingProducingArrangement.setPlanningQueuedDateTime(computedDataTime);
        scoreDirector.afterVariableChanged(
                schedulingProducingArrangement,
                SchedulingProducingArrangement.PLANNING_QUEUED_DATA_TIME
        );
        scoreDirector.triggerVariableListeners();

        scoreDirector.beforeVariableChanged(
                schedulingProducingArrangement,
                SchedulingProducingArrangement.PLANNING_FACTORY_INSTANCE
        );
        schedulingProducingArrangement.setPlanningFactoryInstance(schedulingFactoryInstance);
        scoreDirector.afterVariableChanged(
                schedulingProducingArrangement,
                SchedulingProducingArrangement.PLANNING_FACTORY_INSTANCE
        );
        scoreDirector.triggerVariableListeners();
    }

    private LocalDateTime calcApproximateArrangeDateTimeSlot(
            SchedulingProducingArrangement producingArrangement,
            List<LocalDateTime> localDateTimeList
    ) {
        LocalDateTime result = localDateTimeList.getFirst();

        if (!producingArrangement.getDeepPrerequisiteProducingArrangements().isEmpty()) {
            result = producingArrangement.calcStaticCompleteDateTime(result);

        }

        return result;
    }


}
