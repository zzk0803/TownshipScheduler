package zzk.townshipscheduler.backend.scheduling.model.utility;

import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import zzk.townshipscheduler.backend.ProducingStructureType;
import zzk.townshipscheduler.backend.scheduling.model.BasePlanningChainSupportFactoryOrAction;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInstance;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingPlayerFactoryAction;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
public class FactoryActionShadowGameProducingDataTimeVariableListener
        implements VariableListener<TownshipSchedulingProblem, SchedulingPlayerFactoryAction> {

    @Override
    public void beforeVariableChanged(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull SchedulingPlayerFactoryAction factoryOrAction
    ) {

    }

    @Override
    public void afterVariableChanged(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull SchedulingPlayerFactoryAction factoryOrAction
    ) {
        doUpdateShadowVariable(scoreDirector, factoryOrAction);
    }

    private void doUpdateShadowVariable(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull SchedulingPlayerFactoryAction schedulingPlayerFactoryAction
    ) {
        SchedulingPlayerFactoryAction currentAction = schedulingPlayerFactoryAction;
        while (currentAction != null) {
            SchedulingPlayerFactoryAction planningNext = currentAction.getPlanningNext();

            scoreDirector.beforeVariableChanged(currentAction, "shadowGameProducingDataTime");
            currentAction.setShadowGameProducingDataTime(
                    calcShadowGameProducingDataTime(
                            currentAction.getPlanningFactory(),
                            currentAction.getPlanningPrevious(),
                            currentAction.getPlanningPlayerArrangeDateTime()
                    )
            );
            scoreDirector.afterVariableChanged(currentAction, "shadowGameProducingDataTime");


            currentAction = planningNext;
        }

    }

    private LocalDateTime calcShadowGameProducingDataTime(
            SchedulingFactoryInstance planningFactory,
            BasePlanningChainSupportFactoryOrAction planningPrevious,
            LocalDateTime currentPlanningArrangeDateTime
    ) {
        if (planningFactory == null) {
            return null;
        }

        if (currentPlanningArrangeDateTime == null) {
            return null;
        }

        if (planningFactory.getProducingStructureType() == ProducingStructureType.SLOT) {
            return currentPlanningArrangeDateTime;
        } else {
            Duration finishDurationFromFactory
                    = planningFactory.nextAvailableAsDuration(currentPlanningArrangeDateTime);
            Duration finishDurationFromPreviousCompleted
                    = planningPrevious.nextAvailableAsDuration(currentPlanningArrangeDateTime);
            return (finishDurationFromFactory == null || finishDurationFromPreviousCompleted == null)
                    ? null
                    : currentPlanningArrangeDateTime.plus(
                            finishDurationFromFactory.compareTo(finishDurationFromPreviousCompleted) >= 0
                                    ? finishDurationFromFactory
                                    : finishDurationFromPreviousCompleted
                    );
        }
    }


    @Override
    public void beforeEntityAdded(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull SchedulingPlayerFactoryAction factoryOrAction
    ) {

    }

    @Override
    public void afterEntityAdded(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull SchedulingPlayerFactoryAction factoryOrAction
    ) {
        doUpdateShadowVariable(scoreDirector, factoryOrAction);
    }

    @Override
    public void beforeEntityRemoved(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull SchedulingPlayerFactoryAction factoryOrAction
    ) {

    }

    @Override
    public void afterEntityRemoved(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull SchedulingPlayerFactoryAction factoryOrAction
    ) {

    }

}
