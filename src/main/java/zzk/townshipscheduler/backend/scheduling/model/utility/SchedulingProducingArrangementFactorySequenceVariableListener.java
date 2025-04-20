package zzk.townshipscheduler.backend.scheduling.model.utility;

import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import org.jspecify.annotations.NonNull;
import zzk.townshipscheduler.backend.scheduling.model.*;

import java.util.Objects;
import java.util.function.BiConsumer;

public class SchedulingProducingArrangementFactorySequenceVariableListener
        implements VariableListener<TownshipSchedulingProblem, SchedulingProducingArrangement> {

    @Override
    public void beforeVariableChanged(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull SchedulingProducingArrangement schedulingProducingArrangement
    ) {

    }

    @Override
    public void afterVariableChanged(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull SchedulingProducingArrangement schedulingProducingArrangement
    ) {
        process(scoreDirector, schedulingProducingArrangement);
    }

    private void process(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull SchedulingProducingArrangement schedulingProducingArrangement
    ) {
        SchedulingFactoryInstance planningFactoryInstance
                = schedulingProducingArrangement.getPlanningFactoryInstance();
        SchedulingDateTimeSlot planningDateTimeSlot
                = schedulingProducingArrangement.getPlanningDateTimeSlot();
        FactoryProcessSequence oldFactoryProcessSequence
                = schedulingProducingArrangement.getShadowFactoryProcessSequence();
        if (Objects.isNull(planningFactoryInstance) || Objects.isNull(planningDateTimeSlot)) {
            doShadowVariableUpdate(
                    scoreDirector,
                    schedulingProducingArrangement,
                    null,
                    SchedulingProducingArrangement::setShadowFactoryProcessSequence,
                    SchedulingProducingArrangement.SHADOW_FACTORY_PROCESS_SEQUENCE
            );
            return;
        }

        FactoryProcessSequence newFactoryProcessSequence
                = new FactoryProcessSequence(schedulingProducingArrangement);
        if (!Objects.equals(oldFactoryProcessSequence, newFactoryProcessSequence)) {
            if (Objects.nonNull(oldFactoryProcessSequence)) {
                planningFactoryInstance.removeFactoryProcessSequence(oldFactoryProcessSequence);
            }
            planningFactoryInstance.addFactoryProcessSequence(newFactoryProcessSequence);
            doShadowVariableUpdate(
                    scoreDirector,
                    schedulingProducingArrangement,
                    newFactoryProcessSequence,
                    SchedulingProducingArrangement::setShadowFactoryProcessSequence,
                    SchedulingProducingArrangement.SHADOW_FACTORY_PROCESS_SEQUENCE
            );
        }

    }

    private <E, V> void doShadowVariableUpdate(
            final ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            final E entity,
            final V value,
            final BiConsumer<E, V> setterFunction,
            final String shadowVariableName
    ) {
        scoreDirector.beforeVariableChanged(entity, shadowVariableName);
        setterFunction.accept(entity, value);
        scoreDirector.afterVariableChanged(entity, shadowVariableName);
    }

    @Override
    public void beforeEntityAdded(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull SchedulingProducingArrangement schedulingProducingArrangement
    ) {

    }

    @Override
    public void afterEntityAdded(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull SchedulingProducingArrangement schedulingProducingArrangement
    ) {
        process(scoreDirector, schedulingProducingArrangement);
    }

    @Override
    public void beforeEntityRemoved(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull SchedulingProducingArrangement schedulingProducingArrangement
    ) {

    }

    @Override
    public void afterEntityRemoved(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull SchedulingProducingArrangement schedulingProducingArrangement
    ) {

    }

}
