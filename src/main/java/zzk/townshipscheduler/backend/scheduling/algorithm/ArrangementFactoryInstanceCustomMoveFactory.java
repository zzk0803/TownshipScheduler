package zzk.townshipscheduler.backend.scheduling.algorithm;

import ai.timefold.solver.core.impl.heuristic.move.CompositeMove;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.move.factory.MoveListFactory;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInstance;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ArrangementFactoryInstanceCustomMoveFactory implements MoveListFactory<TownshipSchedulingProblem> {

    @Override
    public List<? extends Move<TownshipSchedulingProblem>> createMoveList(TownshipSchedulingProblem townshipSchedulingProblem) {
        List<SchedulingFactoryInstance> schedulingFactoryInstanceList = townshipSchedulingProblem.getSchedulingFactoryInstanceList();
        ArrayList<ArrangementFactoryInstanceCustomMove> customMoveArrayList = schedulingFactoryInstanceList.stream()
                .map(ArrangementFactoryInstanceCustomMove::new)
                .collect(Collectors.toCollection(ArrayList::new));
        Move<TownshipSchedulingProblem> sortingCompositeMove = CompositeMove.buildMove(customMoveArrayList);
        return List.of(sortingCompositeMove);
    }

}
