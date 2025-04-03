package zzk.townshipscheduler.backend.scheduling.algorithm;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.move.AbstractMove;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.chained.SubChainChangeMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.chained.SubChainReversingChangeMove;
import ai.timefold.solver.core.impl.heuristic.selector.value.chained.SubChain;
import zzk.townshipscheduler.backend.scheduling.model.ISchedulingFactoryOrFactoryArrangement;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryQueueProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

public class QueueProducingArrangementPlanningFactorySubChainChangeMoveSelectionFilter
        implements SelectionFilter<TownshipSchedulingProblem, AbstractMove<TownshipSchedulingProblem>> {

    @Override
    public boolean accept(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            AbstractMove<TownshipSchedulingProblem> selection
    ) {
        if (selection instanceof SubChainChangeMove<TownshipSchedulingProblem> subChainChangeMove) {
            SubChain subChain = subChainChangeMove.getSubChain();
            var headOfSlotProducingSubChain
                    = ((SchedulingFactoryQueueProducingArrangement) subChain.getEntityList().getFirst());
            var toPlanningValue = ((ISchedulingFactoryOrFactoryArrangement) subChainChangeMove.getToPlanningValue());
            return headOfSlotProducingSubChain.getRequiredFactoryInfo() == toPlanningValue.getFactoryInfo();
        } else if (selection instanceof SubChainReversingChangeMove<TownshipSchedulingProblem> subChainReversingChangeMove) {
            SubChain subChain = subChainReversingChangeMove.getSubChain();
            var headOfSlotProducingSubChain
                    = ((SchedulingFactoryQueueProducingArrangement) subChain.getEntityList().getFirst());
            var toPlanningValue = ((ISchedulingFactoryOrFactoryArrangement) subChainReversingChangeMove.getToPlanningValue());
            return headOfSlotProducingSubChain.getRequiredFactoryInfo() == toPlanningValue.getFactoryInfo();
        } else {
            throw new RuntimeException(selection.toString());
        }
    }

}
