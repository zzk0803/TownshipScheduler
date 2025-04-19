package zzk.townshipscheduler.backend.scheduling.algorithm;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.SwapMove;
import zzk.townshipscheduler.backend.scheduling.model.*;

public class ProducingArrangementPlanningFactorySwapMoveSelectionFilter
        implements SelectionFilter<TownshipSchedulingProblem, SwapMove<TownshipSchedulingProblem>> {

    @Override
    public boolean accept(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            SwapMove<TownshipSchedulingProblem> selection
    ) {
        SchedulingProducingArrangement leftEntity
                = (SchedulingProducingArrangement) selection.getLeftEntity();
        SchedulingFactoryInfo leftProducingRequiredFactoryInfo = leftEntity.getRequiredFactoryInfo();
        SchedulingFactoryInstance leftEntityPlanningFactory = leftEntity.getPlanningFactoryInstance();

        SchedulingProducingArrangement rightEntity
                = (SchedulingProducingArrangement) selection.getRightEntity();
        SchedulingFactoryInfo rightProducingRequiredFactoryInfo = rightEntity.getRequiredFactoryInfo();
        SchedulingFactoryInstance rightEntityPlanningFactory = rightEntity.getPlanningFactoryInstance();
        return leftProducingRequiredFactoryInfo .typeEqual(rightEntityPlanningFactory.getSchedulingFactoryInfo())
                || rightProducingRequiredFactoryInfo .typeEqual(leftEntityPlanningFactory.getSchedulingFactoryInfo());
    }

}
