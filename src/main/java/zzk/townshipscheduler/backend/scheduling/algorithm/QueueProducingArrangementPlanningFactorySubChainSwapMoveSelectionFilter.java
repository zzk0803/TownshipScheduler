package zzk.townshipscheduler.backend.scheduling.algorithm;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.move.AbstractMove;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.chained.SubChainReversingSwapMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.chained.SubChainSwapMove;
import ai.timefold.solver.core.impl.heuristic.selector.value.chained.SubChain;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInfo;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryQueueProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingTypeQueueFactoryInstance;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

public class QueueProducingArrangementPlanningFactorySubChainSwapMoveSelectionFilter
        implements SelectionFilter<TownshipSchedulingProblem, AbstractMove<TownshipSchedulingProblem>> {

    @Override
    public boolean accept(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            AbstractMove<TownshipSchedulingProblem> selection
    ) {
        if (selection instanceof SubChainSwapMove<TownshipSchedulingProblem> subChainChangeMove) {
            SubChain leftSubChain = subChainChangeMove.getLeftSubChain();
            var leftSubChainFirstEntity = ((SchedulingFactoryQueueProducingArrangement) leftSubChain.getFirstEntity());
            SchedulingFactoryInfo leftProducingRequiredFactoryInfo = leftSubChainFirstEntity.getRequiredFactoryInfo();
            SchedulingTypeQueueFactoryInstance leftEntityPlanningFactory = leftSubChainFirstEntity.getPlanningAnchorFactory();

            SubChain rightSubChain = subChainChangeMove.getRightSubChain();
            var rightSubChainFirstEntity = ((SchedulingFactoryQueueProducingArrangement) rightSubChain.getFirstEntity());
            SchedulingFactoryInfo rightProducingRequiredFactoryInfo = rightSubChainFirstEntity.getRequiredFactoryInfo();
            SchedulingTypeQueueFactoryInstance rightEntityPlanningFactory = rightSubChainFirstEntity.getPlanningAnchorFactory();

            return leftProducingRequiredFactoryInfo == rightEntityPlanningFactory.getSchedulingFactoryInfo()
                   || rightProducingRequiredFactoryInfo == leftEntityPlanningFactory.getSchedulingFactoryInfo();
        } else if (selection instanceof SubChainReversingSwapMove<TownshipSchedulingProblem> subChainReversingChangeMove) {
            SubChain leftSubChain = subChainReversingChangeMove.getLeftSubChain();
            var leftSubChainFirstEntity = ((SchedulingFactoryQueueProducingArrangement) leftSubChain.getFirstEntity());
            SchedulingFactoryInfo leftProducingRequiredFactoryInfo = leftSubChainFirstEntity.getRequiredFactoryInfo();
            SchedulingTypeQueueFactoryInstance leftEntityPlanningFactory = leftSubChainFirstEntity.getPlanningAnchorFactory();

            SubChain rightSubChain = subChainReversingChangeMove.getRightSubChain();
            var rightSubChainFirstEntity = ((SchedulingFactoryQueueProducingArrangement) rightSubChain.getFirstEntity());
            SchedulingFactoryInfo rightProducingRequiredFactoryInfo = rightSubChainFirstEntity.getRequiredFactoryInfo();
            SchedulingTypeQueueFactoryInstance rightEntityPlanningFactory = rightSubChainFirstEntity.getPlanningAnchorFactory();

            return leftProducingRequiredFactoryInfo == rightEntityPlanningFactory.getSchedulingFactoryInfo()
                   || rightProducingRequiredFactoryInfo == leftEntityPlanningFactory.getSchedulingFactoryInfo();
        } else {
            throw new RuntimeException(selection.toString());
        }
    }

}
