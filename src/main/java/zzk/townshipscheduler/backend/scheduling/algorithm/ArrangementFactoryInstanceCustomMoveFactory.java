package zzk.townshipscheduler.backend.scheduling.algorithm;

import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.move.factory.MoveListFactory;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

import java.util.List;

public class ArrangementFactoryInstanceCustomMoveFactory implements MoveListFactory<TownshipSchedulingProblem> {

    @Override
    public List<? extends Move<TownshipSchedulingProblem>> createMoveList(TownshipSchedulingProblem townshipSchedulingProblem) {
        return List.of(new ArrangementFactoryInstanceCustomMove());
    }

}
