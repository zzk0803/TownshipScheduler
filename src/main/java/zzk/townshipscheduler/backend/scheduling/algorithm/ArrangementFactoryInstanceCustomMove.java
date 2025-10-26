package zzk.townshipscheduler.backend.scheduling.algorithm;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.move.AbstractMove;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInstance;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

import java.util.Collections;
import java.util.List;

public class ArrangementFactoryInstanceCustomMove extends AbstractMove<TownshipSchedulingProblem> {

    private SchedulingFactoryInstance schedulingFactoryInstance;

    private SchedulingProducingArrangement schedulingProducingArrangement;

    public ArrangementFactoryInstanceCustomMove(
            SchedulingFactoryInstance schedulingFactoryInstance,
            SchedulingProducingArrangement schedulingProducingArrangement
    ) {
        this.schedulingFactoryInstance = schedulingFactoryInstance;
        this.schedulingProducingArrangement = schedulingProducingArrangement;
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<TownshipSchedulingProblem> scoreDirector) {
        List<SchedulingProducingArrangement> planningProducingArrangements = schedulingFactoryInstance.getPlanningProducingArrangements();
        scoreDirector.beforeListVariableElementAssigned(
                schedulingFactoryInstance,
                SchedulingFactoryInstance.PLANNING_PRODUCING_ARRANGEMENTS,
                planningProducingArrangements
        );
        planningProducingArrangements.add(schedulingProducingArrangement);
        Collections.sort(planningProducingArrangements,SchedulingProducingArrangement::compareTo);
        scoreDirector.afterListVariableElementAssigned(
                schedulingFactoryInstance,
                SchedulingFactoryInstance.PLANNING_PRODUCING_ARRANGEMENTS,
                planningProducingArrangements
        );
    }

    @Override
    public boolean isMoveDoable(ScoreDirector<TownshipSchedulingProblem> scoreDirector) {
        return true;
    }



}
