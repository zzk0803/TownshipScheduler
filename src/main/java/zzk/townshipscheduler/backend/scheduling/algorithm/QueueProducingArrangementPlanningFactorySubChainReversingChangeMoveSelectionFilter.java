package zzk.townshipscheduler.backend.scheduling.algorithm;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.chained.SubChainReversingChangeMove;
import ai.timefold.solver.core.impl.heuristic.selector.value.chained.SubChain;
import zzk.townshipscheduler.backend.scheduling.model.ISchedulingFactoryOrFactoryArrangement;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryQueueProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

public class QueueProducingArrangementPlanningFactorySubChainReversingChangeMoveSelectionFilter
        implements SelectionFilter<TownshipSchedulingProblem, SubChainReversingChangeMove<TownshipSchedulingProblem>> {

    @Override
    public boolean accept(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            SubChainReversingChangeMove<TownshipSchedulingProblem> selection
    ) {
        SubChain subChain = selection.getSubChain();
        var headOfSlotProducingSubChain
                = ((SchedulingFactoryQueueProducingArrangement) subChain.getEntityList().getFirst());
        var toPlanningValue = ((ISchedulingFactoryOrFactoryArrangement) selection.getToPlanningValue());
        return headOfSlotProducingSubChain.getRequiredFactoryInfo() == toPlanningValue.getFactoryInfo();
    }

}
