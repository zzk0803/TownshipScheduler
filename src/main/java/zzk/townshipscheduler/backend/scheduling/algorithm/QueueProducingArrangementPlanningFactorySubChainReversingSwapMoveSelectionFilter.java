package zzk.townshipscheduler.backend.scheduling.algorithm;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.chained.SubChainReversingSwapMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.chained.SubChainSwapMove;
import ai.timefold.solver.core.impl.heuristic.selector.value.chained.SubChain;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInfo;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryQueueProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingTypeQueueFactoryInstance;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

public class QueueProducingArrangementPlanningFactorySubChainReversingSwapMoveSelectionFilter
        implements SelectionFilter<TownshipSchedulingProblem, SubChainReversingSwapMove<TownshipSchedulingProblem>> {

    @Override
    public boolean accept(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            SubChainReversingSwapMove<TownshipSchedulingProblem> selection
    ) {
        SubChain leftSubChain = selection.getLeftSubChain();
        var leftSubChainFirstEntity = ((SchedulingFactoryQueueProducingArrangement) leftSubChain.getFirstEntity());
        SchedulingFactoryInfo leftProducingRequiredFactoryInfo = leftSubChainFirstEntity.getRequiredFactoryInfo();
        SchedulingTypeQueueFactoryInstance leftEntityPlanningFactory = leftSubChainFirstEntity.getPlanningAnchorFactory();

        SubChain rightSubChain = selection.getRightSubChain();
        var rightSubChainFirstEntity = ((SchedulingFactoryQueueProducingArrangement) rightSubChain.getFirstEntity());
        SchedulingFactoryInfo rightProducingRequiredFactoryInfo = rightSubChainFirstEntity.getRequiredFactoryInfo();
        SchedulingTypeQueueFactoryInstance rightEntityPlanningFactory = rightSubChainFirstEntity.getPlanningAnchorFactory();

        return leftProducingRequiredFactoryInfo == rightEntityPlanningFactory.getSchedulingFactoryInfo()
               || rightProducingRequiredFactoryInfo == leftEntityPlanningFactory.getSchedulingFactoryInfo();
    }

}
