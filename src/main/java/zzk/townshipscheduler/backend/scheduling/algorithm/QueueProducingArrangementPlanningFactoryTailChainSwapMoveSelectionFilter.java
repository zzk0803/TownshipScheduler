package zzk.townshipscheduler.backend.scheduling.algorithm;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.chained.TailChainSwapMove;
import zzk.townshipscheduler.backend.scheduling.model.ISchedulingFactoryOrFactoryArrangement;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInfo;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryQueueProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

public class QueueProducingArrangementPlanningFactoryTailChainSwapMoveSelectionFilter
        implements SelectionFilter<TownshipSchedulingProblem, TailChainSwapMove<TownshipSchedulingProblem>> {

    @Override
    public boolean accept(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            TailChainSwapMove<TownshipSchedulingProblem> selection
    ) {
        var leftEntity = ((SchedulingFactoryQueueProducingArrangement) selection.getLeftEntity());
        var rightValue = ((ISchedulingFactoryOrFactoryArrangement) selection.getRightValue());
        SchedulingFactoryInfo leftEntityRequiredFactoryInfo = leftEntity.getRequiredFactoryInfo();
        SchedulingFactoryInfo rightValueFactoryInfo = rightValue.getFactoryInfo();
        return leftEntityRequiredFactoryInfo==rightValueFactoryInfo;
    }

}
