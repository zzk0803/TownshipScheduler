package zzk.townshipscheduler.backend.scheduling.model.utility;

import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import org.javatuples.Pair;
import org.jspecify.annotations.NonNull;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingDateTimeSlot;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInstance;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.SortedSet;
import java.util.function.BiConsumer;

public class SchedulingProducingArrangementLocalDateTimeVariableListener
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
        SchedulingDateTimeSlot.FactoryProcessSequence shadowFactoryProcessSequence
                = schedulingProducingArrangement.getShadowFactoryProcessSequence();
        if (Objects.isNull(planningFactoryInstance) || Objects.isNull(shadowFactoryProcessSequence)) {
            doShadowVariableUpdate(
                    scoreDirector,
                    schedulingProducingArrangement,
                    null,
                    SchedulingProducingArrangement::setShadowFactoryProcessSequence,
                    SchedulingProducingArrangement.SHADOW_PRODUCING_DATE_TIME
            );
            doShadowVariableUpdate(
                    scoreDirector,
                    schedulingProducingArrangement,
                    null,
                    SchedulingProducingArrangement::setShadowFactoryProcessSequence,
                    SchedulingProducingArrangement.SHADOW_PRODUCING_DATE_TIME
            );
            return;
        }

        Pair<LocalDateTime, LocalDateTime> localDateTimePair
                = planningFactoryInstance.queryProducingAndCompletedPair(schedulingProducingArrangement);
        LocalDateTime producingDateTime = localDateTimePair.getValue0();
        LocalDateTime completedDateTime = localDateTimePair.getValue1();
        doShadowVariableUpdate(
                scoreDirector,
                schedulingProducingArrangement,
                producingDateTime,
                SchedulingProducingArrangement::setComputedShadowProducingDateTime,
                SchedulingProducingArrangement.SHADOW_PRODUCING_DATE_TIME
        );
        doShadowVariableUpdate(
                scoreDirector,
                schedulingProducingArrangement,
                completedDateTime,
                SchedulingProducingArrangement::setComputedShadowCompletedDateTime,
                SchedulingProducingArrangement.SHADOW_COMPLETED_DATE_TIME
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
