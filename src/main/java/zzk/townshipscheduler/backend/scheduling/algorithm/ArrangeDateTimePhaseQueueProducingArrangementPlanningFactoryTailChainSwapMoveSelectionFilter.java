package zzk.townshipscheduler.backend.scheduling.algorithm;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.chained.TailChainSwapMove;
import zzk.townshipscheduler.backend.scheduling.model.*;

public class ArrangeDateTimePhaseQueueProducingArrangementPlanningFactoryTailChainSwapMoveSelectionFilter
        implements SelectionFilter<TownshipSchedulingProblem, TailChainSwapMove<TownshipSchedulingProblem>> {

    @Override
    public boolean accept(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            TailChainSwapMove<TownshipSchedulingProblem> selection
    ) {
        var leftEntity = ((SchedulingProducingArrangementFactoryTypeQueue) selection.getLeftEntity());
        var rightValue = ((ISchedulingFactoryOrFactoryArrangement) selection.getRightValue());
        SchedulingFactoryInfo leftEntityRequiredFactoryInfo = leftEntity.getRequiredFactoryInfo();
        SchedulingFactoryInfo rightValueFactoryInfo = rightValue.getFactoryInfo();
        return leftEntityRequiredFactoryInfo.typeEqual(rightValueFactoryInfo) && rightValue instanceof SchedulingFactoryInstanceTypeQueue;
    }

}
