package zzk.townshipscheduler.backend.scheduling.model.utility;

import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import org.jspecify.annotations.NonNull;
import zzk.townshipscheduler.backend.scheduling.model.*;

import java.util.Objects;
import java.util.SortedMap;
import java.util.function.BiConsumer;

public class SchedulingProducingArrangementFactorySequenceVariableListener
        implements VariableListener<TownshipSchedulingProblem, SchedulingProducingArrangement> {

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
        process(scoreDirector, schedulingProducingArrangement);
    }

    private void process(
            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            @NonNull SchedulingProducingArrangement schedulingProducingArrangement
    ) {
        TownshipSchedulingProblem scoreDirectorWorkingSolution = scoreDirector.getWorkingSolution();
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
                scoreDirectorWorkingSolution.lookupFactoryInstance(oldFactoryProcessSequence.getSchedulingFactoryInstanceReadableIdentifier())
                        .ifPresent(schedulingFactoryInstance -> {
                            schedulingFactoryInstance.removeFactoryProcessSequence(
                                    oldFactoryProcessSequence);
                        });
            }
            planningFactoryInstance.addFactoryProcessSequence(newFactoryProcessSequence);
            doShadowVariableUpdate(
                    scoreDirector,
                    schedulingProducingArrangement,
                    newFactoryProcessSequence,
                    SchedulingProducingArrangement::setShadowFactoryProcessSequence,
                    SchedulingProducingArrangement.SHADOW_FACTORY_PROCESS_SEQUENCE
            );

            SortedMap<FactoryProcessSequence, FactoryComputedDataTimePair> preparedProducingAndCompletedMap
                    = planningFactoryInstance.prepareProducingAndCompletedMap();

            scoreDirectorWorkingSolution.lookupProducingArrangements(planningFactoryInstance)
                    .forEach(streamIterating -> {
                        doUpdateDateTime(scoreDirector, streamIterating, preparedProducingAndCompletedMap);
                    });
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

    private void doUpdateDateTime(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            SchedulingProducingArrangement schedulingProducingArrangement,
            SortedMap<FactoryProcessSequence, FactoryComputedDataTimePair> preparedProducingAndCompletedMap
    ) {
        FactoryProcessSequence factoryProcessSequence = schedulingProducingArrangement.getShadowFactoryProcessSequence();
        FactoryComputedDataTimePair oldDateTimePair = schedulingProducingArrangement.getFactoryComputedDataTimePair();
        if (factoryProcessSequence == null) {
            return;
        }

        FactoryComputedDataTimePair newDateTimePair
                = preparedProducingAndCompletedMap.get(factoryProcessSequence);
        if (newDateTimePair == null) {
            return;
        }

        if (!Objects.equals(oldDateTimePair, newDateTimePair)) {
            doShadowVariableUpdate(
                    scoreDirector,
                    schedulingProducingArrangement,
                    newDateTimePair,
                    SchedulingProducingArrangement::setFactoryComputedDataTimePair,
                    SchedulingProducingArrangement.SHADOW_COMPUTED_DATE_TIME_PAIR
            );
        }
    }

    @Override
    public boolean requiresUniqueEntityEvents() {
        return true;
    }

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

}
