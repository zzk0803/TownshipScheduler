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
        TownshipSchedulingProblem townshipSchedulingProblem = scoreDirector.getWorkingSolution();
        var entity = (SchedulingProducingArrangement) selection.getEntity();
        var toPlanningValue = (SchedulingDateTimeSlot) selection.getToPlanningValue();
        LocalDateTime startDateTime = townshipSchedulingProblem.getSchedulingWorkCalendar().getStartDateTime();
        Duration producingDuration = entity.getStaticDeepProducingDuration();
        LocalDateTime atLeastFeasibleArrangeDateTime = startDateTime.plus(producingDuration);
        return toPlanningValue != null
               && !(toPlanningValue.getStart().isBefore(atLeastFeasibleArrangeDateTime));
    }

}
