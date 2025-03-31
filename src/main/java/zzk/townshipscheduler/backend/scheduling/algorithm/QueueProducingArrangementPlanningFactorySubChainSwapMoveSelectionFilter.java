package zzk.townshipscheduler.backend.scheduling.algorithm;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.chained.SubChainChangeMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.chained.SubChainSwapMove;
import ai.timefold.solver.core.impl.heuristic.selector.value.chained.SubChain;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInfo;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactorySlotProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingTypeSlotFactoryInstance;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

public class QueueProducingArrangementPlanningFactorySubChainSwapMoveSelectionFilter
        implements SelectionFilter<TownshipSchedulingProblem, SubChainSwapMove<TownshipSchedulingProblem>> {

    @Override
    public boolean accept(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            SubChainSwapMove<TownshipSchedulingProblem> selection
    ) {
        SubChain leftSubChain = selection.getLeftSubChain();
        var leftSubChainFirstEntity = ((SchedulingFactorySlotProducingArrangement) leftSubChain.getFirstEntity());
        SchedulingFactoryInfo leftProducingRequiredFactoryInfo = leftSubChainFirstEntity.getRequiredFactoryInfo();
        SchedulingTypeSlotFactoryInstance leftEntityPlanningFactory = leftSubChainFirstEntity.getPlanningFactory();

        SubChain rightSubChain = selection.getRightSubChain();
        var rightSubChainFirstEntity = ((SchedulingFactorySlotProducingArrangement) rightSubChain.getFirstEntity());
        SchedulingFactoryInfo rightProducingRequiredFactoryInfo = rightSubChainFirstEntity.getRequiredFactoryInfo();
        SchedulingTypeSlotFactoryInstance rightEntityPlanningFactory = rightSubChainFirstEntity.getPlanningFactory();

        return leftProducingRequiredFactoryInfo == rightEntityPlanningFactory.getSchedulingFactoryInfo()
               || rightProducingRequiredFactoryInfo == leftEntityPlanningFactory.getSchedulingFactoryInfo();
    }

}
