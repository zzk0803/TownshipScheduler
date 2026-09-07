package zzk.townshipscheduler.backend.scheduling.algorithm;

import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.score.director.ScoreDirector;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

public class SchedulingProducingArrangementFactoryValueRangesFilter
        implements SelectionFilter<TownshipSchedulingProblem, SchedulingProducingArrangement> {

    @Override
    public boolean accept(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            SchedulingProducingArrangement selection
    ) {
        return selection.getRequiredFactoryInfo().getFactoryInstances().size() > 1;
    }

}
