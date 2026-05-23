package zzk.townshipscheduler.backend.scheduling.algorithm;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.ChangeMove;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingDateTimeSlot;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

import java.time.Duration;
import java.time.LocalDateTime;

public class ProducingArrangementPlanningFactoryConstructionHeuristicChangeMoveSelectionFilter implements SelectionFilter<TownshipSchedulingProblem, ChangeMove<TownshipSchedulingProblem>> {

    @Override
    public boolean accept(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            ChangeMove<TownshipSchedulingProblem> selection
    ) {
        var entity = (SchedulingProducingArrangement) selection.getEntity();
        var toPlanningValue = (SchedulingDateTimeSlot) selection.getToPlanningValue();
        LocalDateTime startDateTime = entity.getSchedulingWorkCalendar().getStartDateTime();
        Duration producingDuration = entity.getStaticDeepProducingDuration();
        LocalDateTime atLeastFeasibleArrangeDateTime = startDateTime.plus(producingDuration);
        return toPlanningValue.getStart().isAfter(atLeastFeasibleArrangeDateTime)
                || toPlanningValue.getStart().isEqual(atLeastFeasibleArrangeDateTime);
    }

}
