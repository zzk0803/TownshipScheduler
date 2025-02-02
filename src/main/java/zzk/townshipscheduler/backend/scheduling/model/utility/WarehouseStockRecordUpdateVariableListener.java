package zzk.townshipscheduler.backend.scheduling.model.utility;

import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import org.jspecify.annotations.NonNull;
import zzk.townshipscheduler.backend.scheduling.model.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class WarehouseStockRecordUpdateVariableListener implements VariableListener<TownshipSchedulingProblem, SchedulingWarehouse> {

    @Override
    public void beforeVariableChanged(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull SchedulingWarehouse schedulingWarehouse
    ) {

    }

    @Override
    public void afterVariableChanged(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull SchedulingWarehouse schedulingWarehouse
    ) {
        doUpdate(scoreDirector, schedulingWarehouse);
    }

    @Override
    public void beforeEntityAdded(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull SchedulingWarehouse schedulingWarehouse
    ) {

    }

    @Override
    public void afterEntityAdded(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull SchedulingWarehouse schedulingWarehouse
    ) {
        doUpdate(scoreDirector, schedulingWarehouse);
    }

    private void doUpdate(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull SchedulingWarehouse schedulingWarehouse
    ) {

        List<SchedulingWarehouse.Record> warehouseRecords = new LinkedList<>();
        SchedulingPlayerWarehouseAction warehousePlanningNext = schedulingWarehouse.getPlanningNext();
        while (warehousePlanningNext != null) {
            List<SchedulingWarehouse.Record> actionConsequence = warehousePlanningNext.toWarehouseConsequence();
            warehouseRecords.addAll(actionConsequence);
            warehousePlanningNext = warehousePlanningNext.getPlanningNext();
        }

        TownshipSchedulingProblem workingSolution = scoreDirector.getWorkingSolution();
        Set<SchedulingFactoryInstance> factoryInstanceSet = workingSolution.getSchedulingFactoryInstanceSet();
        for (SchedulingFactoryInstance schedulingFactoryInstance : factoryInstanceSet) {
            SchedulingPlayerFactoryAction factoryInstancePlanningNext = schedulingFactoryInstance.getPlanningNext();
            while (factoryInstancePlanningNext != null) {
                List<SchedulingWarehouse.Record> actionConsequence = factoryInstancePlanningNext.toWarehouseConsequence();
                warehouseRecords.addAll(actionConsequence);
                factoryInstancePlanningNext = factoryInstancePlanningNext.getPlanningNext();
            }

        }

        scoreDirector.beforeVariableChanged(schedulingWarehouse, "records");
        schedulingWarehouse.renew(warehouseRecords);
        scoreDirector.afterVariableChanged(schedulingWarehouse, "records");
    }

    @Override
    public void beforeEntityRemoved(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull SchedulingWarehouse schedulingWarehouse
    ) {

    }

    @Override
    public void afterEntityRemoved(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull SchedulingWarehouse schedulingWarehouse
    ) {

    }

}
