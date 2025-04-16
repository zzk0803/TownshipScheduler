package zzk.townshipscheduler.backend.scheduling.algorithm;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.api.solver.phase.PhaseCommand;
import lombok.extern.slf4j.Slf4j;
import zzk.townshipscheduler.backend.scheduling.model.*;
import zzk.townshipscheduler.backend.scheduling.model.utility.SchedulingProducingArrangementDifficultyComparator;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
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
        List<BaseSchedulingProducingArrangement> producingArrangements
                = workingSolution.getBaseProducingArrangements();
        List<SchedulingFactoryInstanceTypeSlot> slotFactoryInstanceValueRange
                = workingSolution.getSchedulingFactoryInstanceTypeSlotList();
        List<SchedulingFactoryInstanceTypeQueue> queueFactoryInstanceValueRange
                = workingSolution.getSchedulingFactoryInstanceTypeQueueList();
        LocalDateTime startDateTime = workingSolution.getSchedulingWorkTimeLimit().getStartDateTime();

        var queueArrangeFactoryAnchorMap = new LinkedHashMap<SchedulingFactoryInstanceTypeQueue, SchedulingProducingArrangementFactoryTypeQueue>();

        List<SchedulingDateTimeSlot> sortedDataTimeSlotValueRange = dateTimeSlotSetValueRange.stream().sorted().toList();
        List<BaseSchedulingProducingArrangement> difficultySortedProducingArrangements
                = producingArrangements.stream()
                .sorted(new SchedulingProducingArrangementDifficultyComparator())
                .toList();
        ArrayDeque<BaseSchedulingProducingArrangement> initiatingDeque
                = new ArrayDeque<>(difficultySortedProducingArrangements);

        while (!initiatingDeque.isEmpty()) {
            BaseSchedulingProducingArrangement arrangement = initiatingDeque.removeFirst();
            if (shouldInitiating(arrangement)) {
                if (arrangement instanceof SchedulingProducingArrangementFactoryTypeSlot slotProducingArrangement) {
                    setupSlotArrangement(
                            scoreDirector,
                            slotProducingArrangement,
                            sortedDataTimeSlotValueRange,
                            slotFactoryInstanceValueRange,
                            startDateTime
                    );
                } else if (arrangement instanceof SchedulingProducingArrangementFactoryTypeQueue queueProducingArrangement) {
                    setupQueueArrangement(
                            scoreDirector,
                            queueProducingArrangement,
                            sortedDataTimeSlotValueRange,
                            queueFactoryInstanceValueRange,
                            startDateTime,
                            queueArrangeFactoryAnchorMap
                    );
                } else {
                    throw new IllegalStateException();
                }

            }
        }

    }

    private boolean shouldInitiating(BaseSchedulingProducingArrangement arrangement) {
        SchedulingDateTimeSlot planningDateTimeSlot = arrangement.getPlanningDateTimeSlot();
        LocalDateTime arrangeDateTime = arrangement.getArrangeDateTime();
        BaseSchedulingFactoryInstance planningFactoryInstance = arrangement.getPlanningFactoryInstance();
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

    private void setupSlotArrangement(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            SchedulingProducingArrangementFactoryTypeSlot slotProducingArrangement,
            List<SchedulingDateTimeSlot> dateTimeSlotSet,
            List<SchedulingFactoryInstanceTypeSlot> slotFactoryInstanceList,
            LocalDateTime startDateTime
    ) {
        SchedulingDateTimeSlot computedDataTimeSlot
                = calcApproximateArrangeDateTimeSlot(
                slotProducingArrangement,
                dateTimeSlotSet,
                startDateTime
        );

        scoreDirector.beforeVariableChanged(
                slotProducingArrangement,
                BaseSchedulingProducingArrangement.PLANNING_DATA_TIME_SLOT
        );
        slotProducingArrangement.setPlanningDateTimeSlot(computedDataTimeSlot);
        scoreDirector.afterVariableChanged(
                slotProducingArrangement,
                BaseSchedulingProducingArrangement.PLANNING_DATA_TIME_SLOT
        );
        scoreDirector.triggerVariableListeners();

        SchedulingFactoryInfo requireFactory
                = slotProducingArrangement.getSchedulingProduct().getRequireFactory();
        slotFactoryInstanceList.stream()
                .filter(slotFactoryInstance -> requireFactory.typeEqual(slotFactoryInstance.getSchedulingFactoryInfo()))
                .findAny()
                .ifPresentOrElse(
                        schedulingFactoryInstanceTypeSlot -> {
                            scoreDirector.beforeVariableChanged(
                                    slotProducingArrangement,
                                    SchedulingProducingArrangementFactoryTypeSlot.PLANNING_FACTORY
                            );
                            slotProducingArrangement.setPlanningFactory(schedulingFactoryInstanceTypeSlot);
                            scoreDirector.afterVariableChanged(
                                    slotProducingArrangement,
                                    SchedulingProducingArrangementFactoryTypeSlot.PLANNING_FACTORY
                            );
                            scoreDirector.triggerVariableListeners();
                        }, () -> {
                        }
                );

    }

    private void setupQueueArrangement(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            SchedulingProducingArrangementFactoryTypeQueue queueProducingArrangement,
            List<SchedulingDateTimeSlot> dateTimeSlotSet,
            List<SchedulingFactoryInstanceTypeQueue> queueFactoryInstanceList,
            LocalDateTime startDateTime,
            Map<SchedulingFactoryInstanceTypeQueue, SchedulingProducingArrangementFactoryTypeQueue> queueArrangeFactoryAnchorMap
    ) {
        SchedulingDateTimeSlot computedDataTimeSlot
                = calcApproximateArrangeDateTimeSlot(
                queueProducingArrangement,
                dateTimeSlotSet,
                startDateTime
        );

        scoreDirector.beforeVariableChanged(
                queueProducingArrangement,
                BaseSchedulingProducingArrangement.PLANNING_DATA_TIME_SLOT
        );
        queueProducingArrangement.setPlanningDateTimeSlot(computedDataTimeSlot);
        scoreDirector.afterVariableChanged(
                queueProducingArrangement,
                BaseSchedulingProducingArrangement.PLANNING_DATA_TIME_SLOT
        );
        scoreDirector.triggerVariableListeners();

        SchedulingFactoryInfo requireFactory
                = queueProducingArrangement.getSchedulingProduct().getRequireFactory();
        queueFactoryInstanceList.stream()
                .filter(schedulingFactoryInstanceTypeQueue -> schedulingFactoryInstanceTypeQueue.getSchedulingFactoryInfo()
                        .typeEqual(requireFactory))
                .findAny()
                .ifPresentOrElse(
                        schedulingFactoryInstanceTypeQueue -> {

                            SchedulingProducingArrangementFactoryTypeQueue mayNullFactoryToProducing
                                    = queueArrangeFactoryAnchorMap.get(schedulingFactoryInstanceTypeQueue);
                            if (mayNullFactoryToProducing != null) {
                                scoreDirector.beforeVariableChanged(
                                        queueProducingArrangement,
                                        SchedulingProducingArrangementFactoryTypeQueue.PLANNING_PREVIOUS
                                );
                                queueProducingArrangement.setPlanningPreviousProducingArrangementOrFactory(
                                        mayNullFactoryToProducing
                                );
                                scoreDirector.afterVariableChanged(
                                        queueProducingArrangement,
                                        SchedulingProducingArrangementFactoryTypeQueue.PLANNING_PREVIOUS
                                );

                                queueArrangeFactoryAnchorMap.put(
                                        schedulingFactoryInstanceTypeQueue,
                                        queueProducingArrangement
                                );
                            } else {
                                scoreDirector.beforeVariableChanged(
                                        queueProducingArrangement,
                                        SchedulingProducingArrangementFactoryTypeQueue.PLANNING_PREVIOUS
                                );
                                queueProducingArrangement.setPlanningPreviousProducingArrangementOrFactory(
                                        schedulingFactoryInstanceTypeQueue
                                );
                                scoreDirector.afterVariableChanged(
                                        queueProducingArrangement,
                                        SchedulingProducingArrangementFactoryTypeQueue.PLANNING_PREVIOUS
                                );
                                queueArrangeFactoryAnchorMap.put(
                                        schedulingFactoryInstanceTypeQueue,
                                        queueProducingArrangement
                                );
                            }
                            scoreDirector.triggerVariableListeners();
                        }, () -> {
                        }
                );

        scoreDirector.triggerVariableListeners();

    }

    private SchedulingDateTimeSlot calcApproximateArrangeDateTimeSlot(
            BaseSchedulingProducingArrangement producingArrangement,
            List<SchedulingDateTimeSlot> dateTimeSlotSet,
            LocalDateTime startDateTime
    ) {
        SchedulingDateTimeSlot result = dateTimeSlotSet.getFirst();

        List<BaseSchedulingProducingArrangement> prerequisiteProducingArrangements
                = producingArrangement.getDeepPrerequisiteProducingArrangements();

        Duration approximateDelay = Duration.ZERO;

        if (!prerequisiteProducingArrangements.isEmpty()) {
            SchedulingDateTimeSlot schedulingDateTimeSlot_A = prerequisiteProducingArrangements.stream()
                    .filter(iterating -> iterating.getPlanningDateTimeSlot() != null)
                    .map(BaseSchedulingProducingArrangement::getPlanningDateTimeSlot)
                    .max(SchedulingDateTimeSlot::compareTo)
                    .orElse(result);
            schedulingDateTimeSlot_A = SchedulingDateTimeSlot.fromRangeJumpCeil(
                    dateTimeSlotSet, prerequisiteProducingArrangements.stream()
                            .filter(iterating -> iterating.getPlanningDateTimeSlot() != null)
                            .map(BaseSchedulingProducingArrangement::getPlanningDateTimeSlot)
                            .max(SchedulingDateTimeSlot::compareTo)
                            .orElse(result).getStart()
            ).orElse(schedulingDateTimeSlot_A);

            SchedulingDateTimeSlot schedulingDateTimeSlot_B = SchedulingDateTimeSlot.fromRangeJumpCeil(
                    dateTimeSlotSet,
                    startDateTime.plus(prerequisiteProducingArrangements.stream()
                            .map(BaseSchedulingProducingArrangement::getProducingDuration)
                            .distinct()
                            .reduce(approximateDelay, Duration::plus))
            ).orElse(dateTimeSlotSet.getLast());

            result = schedulingDateTimeSlot_A.compareTo(schedulingDateTimeSlot_B) > 0
                    ? schedulingDateTimeSlot_A
                    : schedulingDateTimeSlot_B;
        }

        return result;
    }

}
