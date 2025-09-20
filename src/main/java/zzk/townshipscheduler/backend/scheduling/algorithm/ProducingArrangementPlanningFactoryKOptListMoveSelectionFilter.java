package zzk.townshipscheduler.backend.scheduling.algorithm;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListSwapMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt.KOptListMove;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

public class ProducingArrangementPlanningFactoryKOptListMoveSelectionFilter
        implements SelectionFilter<TownshipSchedulingProblem, KOptListMove<TownshipSchedulingProblem>> {

    @Override
    public boolean accept(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            KOptListMove<TownshipSchedulingProblem> selection
    ) {

        return false;
    }

}
