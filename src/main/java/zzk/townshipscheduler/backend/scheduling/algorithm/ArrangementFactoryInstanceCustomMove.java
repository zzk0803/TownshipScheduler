package zzk.townshipscheduler.backend.scheduling.algorithm;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.move.AbstractMove;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInstance;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

import java.util.List;

public class ArrangementFactoryInstanceCustomMove extends AbstractMove<TownshipSchedulingProblem> {


    public ArrangementFactoryInstanceCustomMove() {
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<TownshipSchedulingProblem> scoreDirector) {
        scoreDirector.getWorkingSolution()
                .getSchedulingFactoryInstanceList()
                .forEach(schedulingFactoryInstance -> {
                    List<SchedulingProducingArrangement> planningProducingArrangements = schedulingFactoryInstance.getPlanningProducingArrangements();
                    scoreDirector.beforeListVariableElementAssigned(
                            schedulingFactoryInstance,
                            SchedulingFactoryInstance.PLANNING_PRODUCING_ARRANGEMENTS,
                            planningProducingArrangements
                    );
                    planningProducingArrangements.sort(SchedulingProducingArrangement::compareTo);
                    scoreDirector.afterListVariableElementAssigned(
                            schedulingFactoryInstance,
                            SchedulingFactoryInstance.PLANNING_PRODUCING_ARRANGEMENTS,
                            planningProducingArrangements
                    );
                });
    }

    @Override
    public boolean isMoveDoable(ScoreDirector<TownshipSchedulingProblem> scoreDirector) {
        return true;
    }

}
