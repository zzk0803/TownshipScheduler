package zzk.townshipscheduler.backend.scheduling.algorithm;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.chained.ChainedChangeMove;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryQueueProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingTypeQueueFactoryInstance;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

public class QueueProducingArrangementPlanningFactoryChainChangeMoveSelectionFilter
        implements SelectionFilter<TownshipSchedulingProblem, ChainedChangeMove<TownshipSchedulingProblem>> {

    @Override
    public boolean accept(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            ChainedChangeMove<TownshipSchedulingProblem> selection
    ) {
        var entity = (SchedulingFactoryQueueProducingArrangement) selection.getEntity();
        var toPlanningValue = (SchedulingTypeQueueFactoryInstance) selection.getToPlanningValue();
        return entity.getRequiredFactoryInfo() == toPlanningValue.getFactoryInfo();
    }

}
