package zzk.townshipscheduler.backend.scheduling.algorithm;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.chained.SubChainChangeMove;
import ai.timefold.solver.core.impl.heuristic.selector.value.chained.SubChain;
import zzk.townshipscheduler.backend.scheduling.model.ISchedulingFactoryOrFactoryArrangement;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactorySlotProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

public class QueueProducingArrangementPlanningFactorySubChainChangeMoveSelectionFilter
        implements SelectionFilter<TownshipSchedulingProblem, SubChainChangeMove<TownshipSchedulingProblem>> {

    @Override
    public boolean accept(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            SubChainChangeMove<TownshipSchedulingProblem> selection
    ) {
        SubChain subChain = selection.getSubChain();
        var headOfSlotProducingSubChain
                = ((SchedulingFactorySlotProducingArrangement) subChain.getEntityList().getFirst());
        var toPlanningValue = ((ISchedulingFactoryOrFactoryArrangement) selection.getToPlanningValue());
        return headOfSlotProducingSubChain.getRequiredFactoryInfo() == toPlanningValue.getFactoryInfo();
    }

}
