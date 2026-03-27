package zzk.townshipscheduler.backend.scheduling.algorithm;

import ai.timefold.solver.core.api.solver.phase.PhaseCommand;
import ai.timefold.solver.core.api.solver.phase.PhaseCommandContext;
import ai.timefold.solver.core.preview.api.domain.metamodel.*;
import lombok.extern.slf4j.Slf4j;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingDateTimeSlot;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInstance;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;
import zzk.townshipscheduler.backend.scheduling.model.utility.SchedulingProducingArrangementDifficultyComparator;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Stream;

@Slf4j
public class TownshipInitiateCustomPhase
        implements PhaseCommand<TownshipSchedulingProblem> {

    @Override
    public void changeWorkingSolution(PhaseCommandContext<TownshipSchedulingProblem> phaseCommandContext) {

        if (phaseCommandContext.isPhaseTerminated()) {
            return;
        }

        TownshipSchedulingProblem workingSolution
                = phaseCommandContext.getWorkingSolution();
        TreeSet<SchedulingDateTimeSlot> dateTimeSlotSetValueRange
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
                        phaseCommandContext,
                        arrangement,
                        sortedDataTimeSlotValueRange,
                        queueFactoryInstanceValueRange
                );
            }
        }


    }

    private boolean shouldInitiating(SchedulingProducingArrangement arrangement) {
        SchedulingDateTimeSlot planningDateTimeSlot = arrangement.getShadowDateTimeSlot();
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
            PhaseCommandContext<TownshipSchedulingProblem> phaseCommandContext,
            SchedulingProducingArrangement schedulingProducingArrangement,
            List<SchedulingDateTimeSlot> dateTimeSlotList,
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
                dateTimeSlotList
        );

        PlanningSolutionMetaModel<TownshipSchedulingProblem> solutionMetaModel = phaseCommandContext.getSolutionMetaModel();
        PlanningEntityMetaModel<TownshipSchedulingProblem, SchedulingProducingArrangement> arrangementPlanningEntityMetaModel
                = solutionMetaModel.entity(SchedulingProducingArrangement.class);
        PlanningEntityMetaModel<TownshipSchedulingProblem, SchedulingFactoryInstance> factoryInstancePlanningEntityMetaModel
                = solutionMetaModel.entity(SchedulingFactoryInstance.class);

        VariableMetaModel<TownshipSchedulingProblem, SchedulingProducingArrangement, Integer> arrangementIntegerVariableMetaModel
                = arrangementPlanningEntityMetaModel.variable(SchedulingProducingArrangement.PLANNING_DELAY_SLOT);
        PlanningVariableMetaModel<TownshipSchedulingProblem, SchedulingProducingArrangement, Integer> arrangementPlanningDelaySlotVariableMetaModel
                = (PlanningVariableMetaModel<TownshipSchedulingProblem, SchedulingProducingArrangement, Integer>) arrangementIntegerVariableMetaModel;

        VariableMetaModel<TownshipSchedulingProblem, SchedulingFactoryInstance, SchedulingProducingArrangement> arrangementVariableMetaModel
                = factoryInstancePlanningEntityMetaModel.variable(SchedulingFactoryInstance.PLANNING_FACTORY_INSTANCE_PRODUCING_ARRANGEMENTS);
        PlanningListVariableMetaModel<TownshipSchedulingProblem, SchedulingFactoryInstance, SchedulingProducingArrangement> planningArrangementVariableMetaModel
                = (PlanningListVariableMetaModel<TownshipSchedulingProblem, SchedulingFactoryInstance, SchedulingProducingArrangement>) arrangementVariableMetaModel;

        phaseCommandContext.execute(
                mutableSolutionView -> {
                    mutableSolutionView.changeVariable(arrangementPlanningDelaySlotVariableMetaModel, schedulingProducingArrangement, 0);
                    mutableSolutionView.assignValueAndAdd(
                            planningArrangementVariableMetaModel,
                            schedulingProducingArrangement,
                            ElementPosition.of(
                                    schedulingFactoryInstance,
                                    schedulingFactoryInstance.getPlanningFactoryInstanceProducingArrangements().size() + 1
                            )
                    );
                }
        );

    }

    private SchedulingDateTimeSlot calcApproximateArrangeDateTimeSlot(
            SchedulingProducingArrangement producingArrangement,
            List<SchedulingDateTimeSlot> dateTimeSlotSet
    ) {
        SchedulingDateTimeSlot result = dateTimeSlotSet.getFirst();

        if (!producingArrangement.getDeepPrerequisiteProducingArrangements().isEmpty()) {
            result = SchedulingDateTimeSlot.fromRangeJumpCeil(
                    dateTimeSlotSet,
                    producingArrangement.calcStaticCompleteDateTime(result.getStart())
            ).orElse(dateTimeSlotSet.getLast());

        }

        return result;
    }


}
