package zzk.townshipscheduler.backend.scheduling.algorithm;

import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionProbabilityWeightFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.chained.TailChainSwapMove;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

public class QueueProducingArrangementPlanningFactoryTailChainSwapMoveSelectionProbabilityWeightFactory
        implements SelectionProbabilityWeightFactory<TownshipSchedulingProblem, TailChainSwapMove<TownshipSchedulingProblem>> {

    public static final double DEFAULT_PROBABILITY = 1;

    @Override
    public double createProbabilityWeight(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            TailChainSwapMove<TownshipSchedulingProblem> selection
    ) {
        BendableScore score = scoreDirector.getWorkingSolution().getScore();
        return DEFAULT_PROBABILITY;
    }

}
