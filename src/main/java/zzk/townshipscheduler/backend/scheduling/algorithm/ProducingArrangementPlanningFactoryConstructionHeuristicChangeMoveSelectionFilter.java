package zzk.townshipscheduler.backend.scheduling.algorithm;

import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.SelectorBasedChangeMove;
import ai.timefold.solver.core.impl.score.director.ScoreDirector;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingDateTimeSlot;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

import java.time.Duration;
import java.time.LocalDateTime;

public class ProducingArrangementPlanningFactoryConstructionHeuristicChangeMoveSelectionFilter
        implements SelectionFilter<TownshipSchedulingProblem, SelectorBasedChangeMove<TownshipSchedulingProblem>> {


    @Override
    public boolean accept(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            SelectorBasedChangeMove<TownshipSchedulingProblem> selection
    ) {
        SchedulingProducingArrangement entity = (SchedulingProducingArrangement) selection.getEntity();
        SchedulingDateTimeSlot toPlanningValue = (SchedulingDateTimeSlot) selection.getToPlanningValue();
        if (toPlanningValue == null)
            return false;
        LocalDateTime arrangeDateTime = toPlanningValue.getStart();
        return !(arrangeDateTime.isBefore(entity.calcStaticIdealArrangeDateTime()));
    }

}
