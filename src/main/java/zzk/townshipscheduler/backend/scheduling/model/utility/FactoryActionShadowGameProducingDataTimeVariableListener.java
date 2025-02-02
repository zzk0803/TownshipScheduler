package zzk.townshipscheduler.backend.scheduling.model.utility;

import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import zzk.townshipscheduler.backend.ProducingStructureType;
import zzk.townshipscheduler.backend.scheduling.model.*;

import java.time.LocalDateTime;

@Slf4j
public class FactoryActionShadowGameProducingDataTimeVariableListener implements VariableListener<TownshipSchedulingProblem, SchedulingPlayerFactoryAction> {

    @Override
    public void beforeVariableChanged(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull SchedulingPlayerFactoryAction schedulingPlayerFactoryAction
    ) {

    }

    @Override
    public void afterVariableChanged(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull SchedulingPlayerFactoryAction schedulingPlayerFactoryAction
    ) {
        doComputeShadowInGameProducingDateTime(scoreDirector, schedulingPlayerFactoryAction);
    }

    @Override
    public void beforeEntityAdded(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull SchedulingPlayerFactoryAction schedulingPlayerFactoryAction
    ) {

    }

    @Override
    public void afterEntityAdded(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull SchedulingPlayerFactoryAction schedulingPlayerFactoryAction
    ) {
        doComputeShadowInGameProducingDateTime(scoreDirector, schedulingPlayerFactoryAction);
    }

    private void doComputeShadowInGameProducingDateTime(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull SchedulingPlayerFactoryAction schedulingPlayerFactoryAction
    ) {
        SchedulingFactoryInstance factoryInstance = schedulingPlayerFactoryAction.getPlanningFactory();
        if (factoryInstance == null) {
            schedulingPlayerFactoryAction.clearShadowVariable();
            return;
        }

        ProducingStructureType factoryProducingType
                = factoryInstance.getSchedulingFactoryInfo().getProducingStructureType();
        LocalDateTime planningPlayerDoItDateTime
                = schedulingPlayerFactoryAction.getPlanningPlayerDoItDateTime();
        SchedulingGameActionExecutionMode planningProducingExecutionMode
                = schedulingPlayerFactoryAction.getPlanningProducingExecutionMode();
        SchedulingPlayerFactoryAction planningPrevious
                = (SchedulingPlayerFactoryAction) schedulingPlayerFactoryAction.getPlanningPrevious();

        if (planningPrevious == null) {
            scoreDirector.beforeVariableChanged(schedulingPlayerFactoryAction,"shadowGameProducingDataTime");
            schedulingPlayerFactoryAction.setShadowGameProducingDataTime(planningPlayerDoItDateTime);
            scoreDirector.afterVariableChanged(schedulingPlayerFactoryAction,"shadowGameProducingDataTime");

            LocalDateTime shadowGameCompleteDateTime
                    = planningPlayerDoItDateTime.plus(planningProducingExecutionMode.getExecuteDuration());
            scoreDirector.beforeVariableChanged(schedulingPlayerFactoryAction,"shadowGameCompleteDateTime");
            schedulingPlayerFactoryAction.setShadowGameCompleteDateTime(shadowGameCompleteDateTime);
            scoreDirector.afterVariableChanged(schedulingPlayerFactoryAction,"shadowGameCompleteDateTime");
        }else {
            if (factoryProducingType == ProducingStructureType.QUEUE) {

                LocalDateTime previousShadowGameCompleteDateTime = planningPrevious.getShadowGameCompleteDateTime();
                scoreDirector.beforeVariableChanged(planningPrevious,"shadowGameProducingDataTime");
                planningPrevious.setShadowGameProducingDataTime(
                        planningPlayerDoItDateTime.isAfter(previousShadowGameCompleteDateTime)
                                ? planningPlayerDoItDateTime
                                : previousShadowGameCompleteDateTime
                );
                scoreDirector.afterVariableChanged(planningPrevious,"shadowGameProducingDataTime");

                LocalDateTime shadowGameCompleteDateTime = planningPlayerDoItDateTime.plus(
                        planningProducingExecutionMode.getExecuteDuration()
                );
                scoreDirector.beforeVariableChanged(planningPrevious,"shadowGameCompleteDateTime");
                planningPrevious.setShadowGameCompleteDateTime(shadowGameCompleteDateTime);
                scoreDirector.afterVariableChanged(planningPrevious,"shadowGameCompleteDateTime");

            } else if (factoryProducingType == ProducingStructureType.SLOT) {

                scoreDirector.beforeVariableChanged(schedulingPlayerFactoryAction,"shadowGameProducingDataTime");
                schedulingPlayerFactoryAction.setShadowGameProducingDataTime(planningPlayerDoItDateTime);
                scoreDirector.afterVariableChanged(schedulingPlayerFactoryAction,"shadowGameProducingDataTime");

                LocalDateTime shadowGameCompleteDateTime
                        = planningPlayerDoItDateTime.plus(planningProducingExecutionMode.getExecuteDuration());
                scoreDirector.beforeVariableChanged(schedulingPlayerFactoryAction,"shadowGameCompleteDateTime");
                schedulingPlayerFactoryAction.setShadowGameCompleteDateTime(shadowGameCompleteDateTime);
                scoreDirector.afterVariableChanged(schedulingPlayerFactoryAction,"shadowGameCompleteDateTime");

            }else {
                log.warn("Run To Here");
            }
        }


    }

    @Override
    public void beforeEntityRemoved(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull SchedulingPlayerFactoryAction schedulingPlayerFactoryAction
    ) {

    }

    @Override
    public void afterEntityRemoved(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull SchedulingPlayerFactoryAction schedulingPlayerFactoryAction
    ) {

    }
}
