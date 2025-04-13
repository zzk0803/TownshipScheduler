package zzk.townshipscheduler.backend.scheduling.algorithm;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.move.AbstractMove;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.chained.SubChainReversingSwapMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.chained.SubChainSwapMove;
import ai.timefold.solver.core.impl.heuristic.selector.value.chained.SubChain;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInfo;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInstanceTypeQueue;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangementFactoryTypeQueue;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

public class ArrangeDateTimePhaseQueueProducingArrangementPlanningFactorySubChainSwapMoveSelectionFilter
        implements SelectionFilter<TownshipSchedulingProblem, AbstractMove<TownshipSchedulingProblem>> {

    @Override
    public boolean accept(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            AbstractMove<TownshipSchedulingProblem> selection
    ) {
        if (selection instanceof SubChainSwapMove<TownshipSchedulingProblem> subChainChangeMove) {
            SubChain leftSubChain = subChainChangeMove.getLeftSubChain();
            var leftSubChainFirstEntity = (SchedulingProducingArrangementFactoryTypeQueue) leftSubChain.getFirstEntity();
            var leftSubChainLastEntity = (SchedulingProducingArrangementFactoryTypeQueue) leftSubChain.getLastEntity();
            SchedulingFactoryInfo leftProducingRequiredFactoryInfo = leftSubChainFirstEntity.getRequiredFactoryInfo();
            SchedulingFactoryInstanceTypeQueue leftEntityPlanningFactory = leftSubChainFirstEntity.getPlanningAnchorFactory();

            SubChain rightSubChain = subChainChangeMove.getRightSubChain();
            var rightSubChainFirstEntity = ((SchedulingProducingArrangementFactoryTypeQueue) rightSubChain.getFirstEntity());
            var rightSubChainLastEntity = ((SchedulingProducingArrangementFactoryTypeQueue) rightSubChain.getLastEntity());
            SchedulingFactoryInfo rightProducingRequiredFactoryInfo = rightSubChainFirstEntity.getRequiredFactoryInfo();
            SchedulingFactoryInstanceTypeQueue rightEntityPlanningFactory = rightSubChainFirstEntity.getPlanningAnchorFactory();

            boolean sameAnchor
                    = leftProducingRequiredFactoryInfo.typeEqual(rightEntityPlanningFactory.getSchedulingFactoryInfo())
                      || rightProducingRequiredFactoryInfo.typeEqual(leftEntityPlanningFactory.getSchedulingFactoryInfo());

            boolean arrangeDateTimeChainSort
                    = rightSubChainFirstEntity.getArrangeDateTime()
                              .isBefore(leftSubChainLastEntity.getArrangeDateTime())
                      && rightSubChainLastEntity.getArrangeDateTime()
                              .isBefore(leftSubChainFirstEntity.getArrangeDateTime());

            return sameAnchor && arrangeDateTimeChainSort;

        } else if (selection instanceof SubChainReversingSwapMove<TownshipSchedulingProblem> subChainReversingChangeMove) {
            SubChain leftSubChain = subChainReversingChangeMove.getLeftSubChain();
            var leftSubChainFirstEntity = ((SchedulingProducingArrangementFactoryTypeQueue) leftSubChain.getFirstEntity());
            var leftSubChainLastEntity = ((SchedulingProducingArrangementFactoryTypeQueue) leftSubChain.getLastEntity());
            SchedulingFactoryInfo leftProducingRequiredFactoryInfo = leftSubChainFirstEntity.getRequiredFactoryInfo();
            SchedulingFactoryInstanceTypeQueue leftEntityPlanningFactory = leftSubChainFirstEntity.getPlanningAnchorFactory();

            SubChain rightSubChain = subChainReversingChangeMove.getRightSubChain();
            var rightSubChainFirstEntity = ((SchedulingProducingArrangementFactoryTypeQueue) rightSubChain.getFirstEntity());
            var rightSubChainLastEntity = ((SchedulingProducingArrangementFactoryTypeQueue) rightSubChain.getLastEntity());
            SchedulingFactoryInfo rightProducingRequiredFactoryInfo = rightSubChainFirstEntity.getRequiredFactoryInfo();
            SchedulingFactoryInstanceTypeQueue rightEntityPlanningFactory = rightSubChainFirstEntity.getPlanningAnchorFactory();

            boolean sameAnchor
                    = leftProducingRequiredFactoryInfo.typeEqual(rightEntityPlanningFactory.getSchedulingFactoryInfo())
                      || rightProducingRequiredFactoryInfo.typeEqual(leftEntityPlanningFactory.getSchedulingFactoryInfo());

            boolean arrangeDateTimeChainSort
                    = rightSubChainFirstEntity.getArrangeDateTime()
                              .isBefore(leftSubChainLastEntity.getArrangeDateTime())
                      && rightSubChainLastEntity.getArrangeDateTime()
                              .isBefore(leftSubChainFirstEntity.getArrangeDateTime());

            return sameAnchor && arrangeDateTimeChainSort;

        } else {
            throw new RuntimeException(selection.toString());
        }
    }

}
