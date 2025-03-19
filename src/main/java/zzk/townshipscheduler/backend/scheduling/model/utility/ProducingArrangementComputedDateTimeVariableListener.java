package zzk.townshipscheduler.backend.scheduling.model.utility;

import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import org.jspecify.annotations.NonNull;
import zzk.townshipscheduler.backend.ProducingStructureType;
import zzk.townshipscheduler.backend.scheduling.model.*;

import java.time.Duration;
import java.util.List;

public class ProducingArrangementComputedDateTimeVariableListener
        implements VariableListener<TownshipSchedulingProblem, AbstractPlayerProducingArrangement> {

    @Override
    public void beforeEntityAdded(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull AbstractPlayerProducingArrangement producingArrangement
    ) {

    }

    @Override
    public void afterEntityAdded(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull AbstractPlayerProducingArrangement producingArrangement
    ) {

    }

    @Override
    public void beforeEntityRemoved(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull AbstractPlayerProducingArrangement producingArrangement
    ) {

    }

    @Override
    public void afterEntityRemoved(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull AbstractPlayerProducingArrangement producingArrangement
    ) {
    }

    @Override
    public void beforeVariableChanged(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull AbstractPlayerProducingArrangement producingArrangement
    ) {

    }

    @Override
    public void afterVariableChanged(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull AbstractPlayerProducingArrangement producingArrangement
    ) {
        if (!(producingArrangement instanceof SchedulingPlayerProducingArrangement factoryAction)) {
            return;
        }
        newDoUpdate(scoreDirector, factoryAction);
    }

    private void newDoUpdate(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            SchedulingPlayerProducingArrangement producingArrangement
    ) {
        List<AbstractPlayerProducingArrangement> shadowPreviousArrangements
                = producingArrangement.getShadowPreviousArrangements();
        Duration computedDuration
                = shadowPreviousArrangements.stream()
                .reduce(
                        Duration.ZERO,
                        (duration, arrangement) -> duration.plus(arrangement.getProducingDuration()),
                        Duration::plus
                );
        updateShadowDelayFromPlanningSlotDuration(scoreDirector, producingArrangement, computedDuration);

    }

    private void updateShadowDelayFromPlanningSlotDuration(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            SchedulingPlayerProducingArrangement factoryAction,
            Duration duration
    ) {
        scoreDirector.beforeVariableChanged(
                factoryAction,
                AbstractPlayerProducingArrangement.SHADOW_DELAY_DURATION
        );
        factoryAction.setShadowDelayFromPlanningSlotDuration(duration);
        scoreDirector.afterVariableChanged(
                factoryAction,
                AbstractPlayerProducingArrangement.SHADOW_DELAY_DURATION
        );
    }

}
