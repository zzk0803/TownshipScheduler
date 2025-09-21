package zzk.townshipscheduler.backend.scheduling.algorithm;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListChangeMove;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInstanceDateTimeSlot;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

public class ProducingArrangementPlanningFactoryListChangeMoveSelectionFilter
        implements SelectionFilter<TownshipSchedulingProblem, ListChangeMove<TownshipSchedulingProblem>> {

    @Override
    public boolean accept(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            ListChangeMove<TownshipSchedulingProblem> selection
    ) {
        var movedValue = (SchedulingProducingArrangement) selection.getMovedValue();
        var destinationEntity = (SchedulingFactoryInstanceDateTimeSlot) selection.getDestinationEntity();
        return movedValue.getRequiredFactoryInfo().typeEqual(destinationEntity.getSchedulingFactoryInfo());
    }

}
