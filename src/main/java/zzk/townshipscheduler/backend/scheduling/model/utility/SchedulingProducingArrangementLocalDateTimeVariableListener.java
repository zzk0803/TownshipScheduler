package zzk.townshipscheduler.backend.scheduling.model.utility;

import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import org.javatuples.Pair;
import org.jspecify.annotations.NonNull;
import zzk.townshipscheduler.backend.scheduling.model.FactoryProcessSequence;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInstance;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.SortedMap;
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
        FactoryProcessSequence shadowFactoryProcessSequence
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

        SortedMap<FactoryProcessSequence, Pair<LocalDateTime, LocalDateTime>> preparedProducingAndCompletedMap
                = planningFactoryInstance.prepareProducingAndCompletedMap();
        doUpdateDateTime(scoreDirector, schedulingProducingArrangement, preparedProducingAndCompletedMap);

        scoreDirector.getWorkingSolution()
                .getSchedulingProducingArrangementList()
                .stream()
                .filter(streamIterating -> streamIterating.getPlanningFactoryInstance() == planningFactoryInstance)
                .filter(streamIterating -> streamIterating != schedulingProducingArrangement)
                .filter(streamIterating -> Objects.nonNull(streamIterating.getShadowFactoryProcessSequence()))
                .forEach(streamIterating -> {
                    doUpdateDateTime(scoreDirector, streamIterating, preparedProducingAndCompletedMap);
                });
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
            SortedMap<FactoryProcessSequence, Pair<LocalDateTime, LocalDateTime>> preparedProducingAndCompletedMap
    ) {
        FactoryProcessSequence factoryProcessSequence = schedulingProducingArrangement.getShadowFactoryProcessSequence();
        LocalDateTime oldProducingDateTime = schedulingProducingArrangement.getComputedShadowProducingDateTime();
        LocalDateTime oldCompletedDateTime = schedulingProducingArrangement.getComputedShadowCompletedDateTime();
        Pair<LocalDateTime, LocalDateTime> localDateTimePair
                = preparedProducingAndCompletedMap.get(factoryProcessSequence);
        if (localDateTimePair == null) {
            doShadowVariableUpdate(
                    scoreDirector,
                    schedulingProducingArrangement,
                    null,
                    SchedulingProducingArrangement::setComputedShadowProducingDateTime,
                    SchedulingProducingArrangement.SHADOW_PRODUCING_DATE_TIME
            );
            doShadowVariableUpdate(
                    scoreDirector,
                    schedulingProducingArrangement,
                    null,
                    SchedulingProducingArrangement::setComputedShadowCompletedDateTime,
                    SchedulingProducingArrangement.SHADOW_COMPLETED_DATE_TIME
            );
            return;
        }

        LocalDateTime producingDateTime = localDateTimePair.getValue0();
        LocalDateTime completedDateTime = localDateTimePair.getValue1();
        if (!(
                Objects.equals(oldProducingDateTime, producingDateTime)
                && Objects.equals(oldCompletedDateTime, completedDateTime)
        )) {
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
    }

    private void doUpdateDateTime(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            SchedulingProducingArrangement schedulingProducingArrangement,
            SchedulingFactoryInstance planningFactoryInstance
    ) {
        LocalDateTime oldProducingDateTime = schedulingProducingArrangement.getComputedShadowProducingDateTime();
        LocalDateTime oldCompletedDateTime = schedulingProducingArrangement.getComputedShadowCompletedDateTime();
        Pair<LocalDateTime, LocalDateTime> localDateTimePair
                = planningFactoryInstance.queryProducingAndCompletedPair(schedulingProducingArrangement);
        LocalDateTime producingDateTime = localDateTimePair.getValue0();
        LocalDateTime completedDateTime = localDateTimePair.getValue1();
        if (!(
                Objects.equals(oldProducingDateTime, producingDateTime)
                && Objects.equals(oldCompletedDateTime, completedDateTime)
        )) {
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
