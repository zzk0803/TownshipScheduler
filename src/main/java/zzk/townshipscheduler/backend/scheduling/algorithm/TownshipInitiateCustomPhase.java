package zzk.townshipscheduler.backend.scheduling.algorithm;

import ai.timefold.solver.core.api.solver.phase.PhaseCommand;
import ai.timefold.solver.core.api.solver.phase.PhaseCommandContext;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningEntityMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import lombok.extern.slf4j.Slf4j;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingDateTimeSlot;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInstance;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

import java.time.LocalDateTime;
import java.util.*;
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
        NavigableSet<SchedulingDateTimeSlot> dateTimeSlotSetValueRange
                = workingSolution.getSchedulingDateTimeSlots();
        NavigableSet<SchedulingProducingArrangement> producingArrangements
                = workingSolution.getSchedulingProducingArrangements();
        List<SchedulingFactoryInstance> queueFactoryInstanceValueRange
                = workingSolution.getSchedulingFactoryInstanceList();

        log.info(
                "difficultySortedProducingArrangements:{} ",
                producingArrangements
        );
        ArrayDeque<SchedulingProducingArrangement> initiatingDeque
                = new ArrayDeque<>(producingArrangements);

        while (!initiatingDeque.isEmpty()) {
            SchedulingProducingArrangement arrangement = initiatingDeque.removeFirst();
            if (shouldInitiating(arrangement)) {
                setupArrangement(
                        phaseCommandContext,
                        arrangement,
                        dateTimeSlotSetValueRange,
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
            PhaseCommandContext<TownshipSchedulingProblem> phaseCommandContext,
            SchedulingProducingArrangement schedulingProducingArrangement,
            NavigableSet<SchedulingDateTimeSlot> schedulingDateTimeSlots,
            List<SchedulingFactoryInstance> factoryInstanceList
    ) {
        SchedulingFactoryInstance schedulingFactoryInstance
                = factoryInstanceList.stream()
                .filter(slotFactoryInstance -> schedulingProducingArrangement.getRequiredFactoryInfo()
                        .typeEqual(slotFactoryInstance.getSchedulingFactoryInfo()))
                .findAny()
                .get()
                ;
        SchedulingDateTimeSlot computedDataTimeSlot
                = calcApproximateArrangeDateTimeSlot(
                schedulingProducingArrangement,
                schedulingDateTimeSlots
        );

        PlanningSolutionMetaModel<TownshipSchedulingProblem> solutionMetaModel = phaseCommandContext.getSolutionMetaModel();
        PlanningEntityMetaModel<TownshipSchedulingProblem, SchedulingProducingArrangement> schedulingProducingArrangementPlanningEntityMetaModel
                = solutionMetaModel.entity(SchedulingProducingArrangement.class);

        PlanningVariableMetaModel<TownshipSchedulingProblem, SchedulingProducingArrangement, SchedulingDateTimeSlot>
                dateTimeSlotPlanningVariableMetaModel
                = (PlanningVariableMetaModel<TownshipSchedulingProblem, SchedulingProducingArrangement, SchedulingDateTimeSlot>)
                schedulingProducingArrangementPlanningEntityMetaModel.<SchedulingDateTimeSlot>variable(SchedulingProducingArrangement.PLANNING_DATE_TIME_SLOT);

        PlanningVariableMetaModel<TownshipSchedulingProblem, SchedulingProducingArrangement, SchedulingFactoryInstance>
                factoryInstancePlanningVariableMetaModel
                = (PlanningVariableMetaModel<TownshipSchedulingProblem, SchedulingProducingArrangement, SchedulingFactoryInstance>)
                schedulingProducingArrangementPlanningEntityMetaModel.<SchedulingFactoryInstance>variable(SchedulingProducingArrangement.PLANNING_FACTORY_INSTANCE);

        phaseCommandContext.execute(
                Moves.compose(
                        Moves.change(
                                factoryInstancePlanningVariableMetaModel,
                                schedulingProducingArrangement,
                                schedulingFactoryInstance
                        ),
                        Moves.change(
                                dateTimeSlotPlanningVariableMetaModel,
                                schedulingProducingArrangement,
                                computedDataTimeSlot
                        )
                )
        );

    }

    private SchedulingDateTimeSlot calcApproximateArrangeDateTimeSlot(
            SchedulingProducingArrangement producingArrangement,
            NavigableSet<SchedulingDateTimeSlot> dateTimeSlotSet
    ) {
        SchedulingDateTimeSlot dateTimeSlotSetFirst = dateTimeSlotSet.getFirst();
        return SchedulingDateTimeSlot.ceilingDateTimeFromValueRange(
                dateTimeSlotSet,
                dateTimeSlotSetFirst.getStart()
        );
    }


}
