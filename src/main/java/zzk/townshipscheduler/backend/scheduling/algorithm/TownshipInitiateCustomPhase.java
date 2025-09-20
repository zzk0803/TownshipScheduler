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
import java.util.Set;
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
        List<SchedulingFactoryInstanceDateTimeSlot> schedulingFactoryInstanceDateTimeSlotList
                = workingSolution.getSchedulingFactoryInstanceDateTimeSlotList();
        LocalDateTime startDateTime = workingSolution.getSchedulingWorkCalendar().getStartDateTime();


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
                        queueFactoryInstanceValueRange,
                        schedulingFactoryInstanceDateTimeSlotList,
                        startDateTime
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
            List<SchedulingFactoryInstance> factoryInstanceList,
            List<SchedulingFactoryInstanceDateTimeSlot> schedulingFactoryInstanceDateTimeSlotList,
            LocalDateTime startDateTime
    ) {
        SchedulingDateTimeSlot computedDataTimeSlot
                = calcApproximateArrangeDateTimeSlot(
                schedulingProducingArrangement,
                dateTimeSlotSet,
                startDateTime
        );
        SchedulingFactoryInfo requireFactory
                = schedulingProducingArrangement.getSchedulingProduct().getRequireFactory();
        SchedulingFactoryInstance schedulingFactoryInstance
                = factoryInstanceList.stream()
                .filter(slotFactoryInstance -> requireFactory.typeEqual(slotFactoryInstance.getSchedulingFactoryInfo()))
                .findAny()
                .get();

        schedulingFactoryInstanceDateTimeSlotList.stream()
                .filter(schedulingFactoryInstanceDateTimeSlot -> schedulingFactoryInstanceDateTimeSlot.getFactoryInstance() == schedulingFactoryInstance
                                                                 && schedulingFactoryInstanceDateTimeSlot.getDateTimeSlot() == computedDataTimeSlot)
                .findFirst()
                .ifPresent(schedulingFactoryInstanceDateTimeSlot -> {
                    int size = schedulingFactoryInstanceDateTimeSlot.getPlanningSchedulingProducingArrangements()
                            .size();
                    scoreDirector.beforeListVariableChanged(
                            schedulingFactoryInstanceDateTimeSlot,
                            SchedulingFactoryInstanceDateTimeSlot.PLANNING_SCHEDULING_PRODUCING_ARRANGEMENTS,
                            size,
                            size+1
                    );
                    scoreDirector.beforeListVariableElementAssigned(
                            schedulingFactoryInstanceDateTimeSlot,
                            SchedulingFactoryInstanceDateTimeSlot.PLANNING_SCHEDULING_PRODUCING_ARRANGEMENTS,
                            schedulingProducingArrangement
                    );
                    schedulingFactoryInstanceDateTimeSlot.getPlanningSchedulingProducingArrangements()
                            .add(schedulingProducingArrangement);
                    scoreDirector.afterListVariableElementAssigned(
                            schedulingFactoryInstanceDateTimeSlot,
                            SchedulingFactoryInstanceDateTimeSlot.PLANNING_SCHEDULING_PRODUCING_ARRANGEMENTS,
                            schedulingProducingArrangement
                    );
                    scoreDirector.afterListVariableChanged(
                            schedulingFactoryInstanceDateTimeSlot,
                            SchedulingFactoryInstanceDateTimeSlot.PLANNING_SCHEDULING_PRODUCING_ARRANGEMENTS,
                            size,
                            size+1
                    );
                    scoreDirector.triggerVariableListeners();
                });


    }

    private SchedulingDateTimeSlot calcApproximateArrangeDateTimeSlot(
            SchedulingProducingArrangement producingArrangement,
            List<SchedulingDateTimeSlot> dateTimeSlotSet,
            LocalDateTime startDateTime
    ) {
        SchedulingDateTimeSlot result = dateTimeSlotSet.getFirst();

        Set<SchedulingProducingArrangement> prerequisiteProducingArrangements
                = producingArrangement.getDeepPrerequisiteProducingArrangements();

        Duration approximateDelay = Duration.ZERO;

        if (!prerequisiteProducingArrangements.isEmpty()) {
            SchedulingDateTimeSlot schedulingDateTimeSlot_A
                    = prerequisiteProducingArrangements.stream()
                    .filter(iterating -> iterating.getPlanningDateTimeSlot() != null)
                    .map(SchedulingProducingArrangement::getPlanningDateTimeSlot)
                    .max(SchedulingDateTimeSlot::compareTo)
                    .orElse(result);
            schedulingDateTimeSlot_A = SchedulingDateTimeSlot.fromRangeJumpCeil(
                    dateTimeSlotSet,
                    prerequisiteProducingArrangements.stream()
                            .filter(iterating -> iterating.getPlanningDateTimeSlot() != null)
                            .map(SchedulingProducingArrangement::getPlanningDateTimeSlot)
                            .max(SchedulingDateTimeSlot::compareTo)
                            .orElse(result).getStart()
            ).orElse(schedulingDateTimeSlot_A);

            SchedulingDateTimeSlot schedulingDateTimeSlot_B
                    = SchedulingDateTimeSlot.fromRangeJumpCeil(
                    dateTimeSlotSet,
                    startDateTime.plus(
                            prerequisiteProducingArrangements.stream()
                                    .map(SchedulingProducingArrangement::getProducingDuration)
                                    .distinct()
                                    .reduce(approximateDelay, Duration::plus)
                    )
            ).orElse(dateTimeSlotSet.getLast());

            result = schedulingDateTimeSlot_A.compareTo(schedulingDateTimeSlot_B) > 0
                    ? schedulingDateTimeSlot_A
                    : schedulingDateTimeSlot_B;
        }

        return result;
    }


}
