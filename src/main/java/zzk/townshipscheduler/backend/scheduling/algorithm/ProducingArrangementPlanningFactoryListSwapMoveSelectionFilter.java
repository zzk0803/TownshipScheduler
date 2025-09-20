package zzk.townshipscheduler.backend.scheduling.algorithm;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListSwapMove;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInstanceDateTimeSlot;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

public class ProducingArrangementPlanningFactoryListSwapMoveSelectionFilter
        implements SelectionFilter<TownshipSchedulingProblem, ListSwapMove<TownshipSchedulingProblem>> {

    @Override
    public boolean accept(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            ListSwapMove<TownshipSchedulingProblem> selection
    ) {
        var leftEntity = (SchedulingFactoryInstanceDateTimeSlot) selection.getLeftEntity();
        var rightEntity = (SchedulingFactoryInstanceDateTimeSlot) selection.getRightEntity();
        var boolSameFactoryInfo = rightEntity.getSchedulingFactoryInfo()
                .typeEqual(leftEntity.getSchedulingFactoryInfo());

        var leftValue = (SchedulingProducingArrangement) selection.getLeftValue();
        var leftValueFactoryInfoMatch = leftValue.getRequiredFactoryInfo()
                .typeEqual(rightEntity.getSchedulingFactoryInfo());

        var rightValue = (SchedulingProducingArrangement) selection.getRightValue();
        var rightValueFactoryInfoMatch = rightValue.getRequiredFactoryInfo()
                .typeEqual(rightEntity.getSchedulingFactoryInfo());

        return boolSameFactoryInfo && leftValueFactoryInfoMatch && rightValueFactoryInfoMatch;
    }

}
