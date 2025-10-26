package zzk.townshipscheduler.backend.scheduling.algorithm;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.move.AbstractMove;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingDateTimeSlot;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInstance;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ArrangementDateTimeSlotCustomMove extends AbstractMove<TownshipSchedulingProblem> {

    private SchedulingDateTimeSlot schedulingDateTimeSlot;

    private SchedulingProducingArrangement schedulingProducingArrangement;

    public ArrangementDateTimeSlotCustomMove(
            SchedulingDateTimeSlot schedulingDateTimeSlot,
            SchedulingProducingArrangement schedulingProducingArrangement
    ) {
        this.schedulingDateTimeSlot = schedulingDateTimeSlot;
        this.schedulingProducingArrangement = schedulingProducingArrangement;
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<TownshipSchedulingProblem> scoreDirector) {
        scoreDirector.beforeVariableChanged(schedulingProducingArrangement,SchedulingProducingArrangement.PLANNING_DATA_TIME_SLOT);
        schedulingProducingArrangement.setPlanningDateTimeSlot(schedulingDateTimeSlot);
        scoreDirector.afterVariableChanged(schedulingProducingArrangement,SchedulingProducingArrangement.PLANNING_DATA_TIME_SLOT);
    }

    @Override
    public boolean isMoveDoable(ScoreDirector<TownshipSchedulingProblem> scoreDirector) {
        SchedulingDateTimeSlot oldDateTimeSlot = schedulingProducingArrangement.getPlanningDateTimeSlot();
        if (oldDateTimeSlot == null) {
            return true;
        }

        return !Objects.equals(oldDateTimeSlot, schedulingDateTimeSlot);
    }

}
