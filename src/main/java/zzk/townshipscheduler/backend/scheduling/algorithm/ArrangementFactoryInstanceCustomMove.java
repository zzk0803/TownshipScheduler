package zzk.townshipscheduler.backend.scheduling.algorithm;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.move.AbstractMove;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInstance;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

import java.util.List;

public class ArrangementFactoryInstanceCustomMove extends AbstractMove<TownshipSchedulingProblem> {

    private SchedulingFactoryInstance schedulingFactoryInstance;

    public ArrangementFactoryInstanceCustomMove(SchedulingFactoryInstance schedulingFactoryInstance) {
        this.schedulingFactoryInstance = schedulingFactoryInstance;
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<TownshipSchedulingProblem> scoreDirector) {
        SchedulingFactoryInstance factoryInstance = scoreDirector.lookUpWorkingObjectOrReturnNull(schedulingFactoryInstance);
        if (factoryInstance == null) {
            throw new IllegalStateException("factoryInstance is null");
        }

        List<SchedulingProducingArrangement> planningProducingArrangements = factoryInstance.getPlanningProducingArrangements();
        scoreDirector.beforeListVariableElementUnassigned(
                factoryInstance,
                SchedulingFactoryInstance.PLANNING_PRODUCING_ARRANGEMENTS,
                planningProducingArrangements
        );
        factoryInstance.setPlanningProducingArrangements(null);
        scoreDirector.afterListVariableElementUnassigned(
                factoryInstance,
                SchedulingFactoryInstance.PLANNING_PRODUCING_ARRANGEMENTS,
                planningProducingArrangements
        );

        scoreDirector.beforeListVariableElementAssigned(
                factoryInstance,
                SchedulingFactoryInstance.PLANNING_PRODUCING_ARRANGEMENTS,
                planningProducingArrangements
        );
        planningProducingArrangements.sort(SchedulingProducingArrangement::compareTo);
        factoryInstance.setPlanningProducingArrangements(planningProducingArrangements);
        scoreDirector.afterListVariableElementAssigned(
                factoryInstance,
                SchedulingFactoryInstance.PLANNING_PRODUCING_ARRANGEMENTS,
                planningProducingArrangements
        );
    }

    @Override
    public boolean isMoveDoable(ScoreDirector<TownshipSchedulingProblem> scoreDirector) {
        return true;
    }

}
