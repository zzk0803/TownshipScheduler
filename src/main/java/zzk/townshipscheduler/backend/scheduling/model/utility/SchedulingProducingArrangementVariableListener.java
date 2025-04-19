package zzk.townshipscheduler.backend.scheduling.model.utility;

import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import org.jspecify.annotations.NonNull;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingDateTimeSlot;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInstance;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

import java.util.Objects;
import java.util.SortedSet;
import java.util.function.BiConsumer;

public class SchedulingProducingArrangementVariableListener
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
        SchedulingDateTimeSlot.FactoryProcessSequence oldFactoryProcessSequence
                = schedulingProducingArrangement.getShadowFactoryProcessSequence();
        if (Objects.isNull(planningFactoryInstance) || Objects.isNull(planningDateTimeSlot)) {
            schedulingProducingArrangement.invalidComputedShadowProperty();
            doShadowVariableUpdate(
                    scoreDirector,
                    schedulingProducingArrangement,
                    null,
                    SchedulingProducingArrangement::setShadowFactoryProcessSequence,
                    SchedulingProducingArrangement.SHADOW_DATE_TIME_SEQUENCE
            );
            return;
        }

        SortedSet<SchedulingDateTimeSlot.FactoryProcessSequence> shadowFactorySequenceSet
                = planningFactoryInstance.getShadowFactorySequenceSet();
        SchedulingDateTimeSlot.FactoryProcessSequence newFactoryProcessSequence
                = new SchedulingDateTimeSlot.FactoryProcessSequence(schedulingProducingArrangement);
        if (Objects.nonNull(oldFactoryProcessSequence)) {
            shadowFactorySequenceSet.remove(oldFactoryProcessSequence);
        }

        shadowFactorySequenceSet.add(newFactoryProcessSequence);
        schedulingProducingArrangement.invalidComputedShadowProperty();
        doShadowVariableUpdate(
                scoreDirector,
                schedulingProducingArrangement,
                newFactoryProcessSequence,
                SchedulingProducingArrangement::setShadowFactoryProcessSequence,
                SchedulingProducingArrangement.SHADOW_DATE_TIME_SEQUENCE
        );

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
