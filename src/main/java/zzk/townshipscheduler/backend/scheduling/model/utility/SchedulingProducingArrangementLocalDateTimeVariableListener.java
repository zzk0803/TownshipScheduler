//package zzk.townshipscheduler.backend.scheduling.model.utility;
//
//import ai.timefold.solver.core.api.domain.variable.VariableListener;
//import ai.timefold.solver.core.api.score.director.ScoreDirector;
//import org.jspecify.annotations.NonNull;
//import zzk.townshipscheduler.backend.scheduling.model.*;
//
//import java.time.LocalDateTime;
//import java.util.Objects;
//import java.util.SortedMap;
//import java.util.function.BiConsumer;
//
//public class SchedulingProducingArrangementLocalDateTimeVariableListener
//        implements VariableListener<TownshipSchedulingProblem, SchedulingProducingArrangement> {
//
//    @Override
//    public void beforeEntityAdded(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull SchedulingProducingArrangement schedulingProducingArrangement
//    ) {
//
//    }
//
//    @Override
//    public void afterEntityAdded(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull SchedulingProducingArrangement schedulingProducingArrangement
//    ) {
//        process(scoreDirector, schedulingProducingArrangement);
//    }
//
//    @Override
//    public void beforeEntityRemoved(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull SchedulingProducingArrangement schedulingProducingArrangement
//    ) {
//
//    }
//
//    @Override
//    public void afterEntityRemoved(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull SchedulingProducingArrangement schedulingProducingArrangement
//    ) {
//
//    }
//
//    private void process(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull SchedulingProducingArrangement schedulingProducingArrangement
//    ) {
//        SchedulingFactoryInstance planningFactoryInstance
//                = schedulingProducingArrangement.getPlanningFactoryInstance();
//        if (planningFactoryInstance == null) {
//            setDateTimeNull(scoreDirector, schedulingProducingArrangement);
//            return;
//        }
//
//        SortedMap<FactoryProcessSequence, FactoryComputedDataTimePair> preparedProducingAndCompletedMap
//                = planningFactoryInstance.prepareProducingAndCompletedMap();
//
//        scoreDirector.getWorkingSolution()
//                .getSchedulingProducingArrangementList()
//                .stream()
//                .filter(streamIterating -> streamIterating.getPlanningFactoryInstance() == planningFactoryInstance)
//                .forEach(streamIterating -> {
//                    doUpdateDateTime(scoreDirector, streamIterating, preparedProducingAndCompletedMap);
//                });
//    }
//
//    private void setDateTimeNull(
//            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            SchedulingProducingArrangement schedulingProducingArrangement
//    ) {
//        doShadowVariableUpdate(
//                scoreDirector,
//                schedulingProducingArrangement,
//                null,
//                SchedulingProducingArrangement::setComputedShadowProducingDateTime,
//                SchedulingProducingArrangement.SHADOW_PRODUCING_DATE_TIME
//        );
//        doShadowVariableUpdate(
//                scoreDirector,
//                schedulingProducingArrangement,
//                null,
//                SchedulingProducingArrangement::setComputedShadowCompletedDateTime,
//                SchedulingProducingArrangement.SHADOW_COMPLETED_DATE_TIME
//        );
//    }
//
//    private void doUpdateDateTime(
//            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            SchedulingProducingArrangement schedulingProducingArrangement,
//            SortedMap<FactoryProcessSequence, FactoryComputedDataTimePair> preparedProducingAndCompletedMap
//    ) {
//        FactoryProcessSequence factoryProcessSequence = schedulingProducingArrangement.getShadowFactoryProcessSequence();
//        LocalDateTime oldProducingDateTime = schedulingProducingArrangement.getComputedShadowProducingDateTime();
//        LocalDateTime oldCompletedDateTime = schedulingProducingArrangement.getComputedShadowCompletedDateTime();
//        if (factoryProcessSequence == null) {
//            setDateTimeNull(scoreDirector, schedulingProducingArrangement);
//            return;
//        }
//
//        FactoryComputedDataTimePair localDateTimePair
//                = preparedProducingAndCompletedMap.get(factoryProcessSequence);
//        if (localDateTimePair == null) {
//            return;
//        }
//
//        LocalDateTime producingDateTime = localDateTimePair.producingDateTime();
//        LocalDateTime completedDateTime = localDateTimePair.completedDateTime();
//        if (!(
//                Objects.equals(oldProducingDateTime, producingDateTime)
//                && Objects.equals(oldCompletedDateTime, completedDateTime)
//        )) {
//            doShadowVariableUpdate(
//                    scoreDirector,
//                    schedulingProducingArrangement,
//                    producingDateTime,
//                    SchedulingProducingArrangement::setComputedShadowProducingDateTime,
//                    SchedulingProducingArrangement.SHADOW_PRODUCING_DATE_TIME
//            );
//            doShadowVariableUpdate(
//                    scoreDirector,
//                    schedulingProducingArrangement,
//                    completedDateTime,
//                    SchedulingProducingArrangement::setComputedShadowCompletedDateTime,
//                    SchedulingProducingArrangement.SHADOW_COMPLETED_DATE_TIME
//            );
//        }
//    }
//
//    private <E, V> void doShadowVariableUpdate(
//            final ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            final E entity,
//            final V value,
//            final BiConsumer<E, V> setterFunction,
//            final String shadowVariableName
//    ) {
//        scoreDirector.beforeVariableChanged(entity, shadowVariableName);
//        setterFunction.accept(entity, value);
//        scoreDirector.afterVariableChanged(entity, shadowVariableName);
//    }
//
//    @Override
//    public boolean requiresUniqueEntityEvents() {
//        return true;
//    }
//
//    @Override
//    public void beforeVariableChanged(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull SchedulingProducingArrangement schedulingProducingArrangement
//    ) {
//
//    }
//
//    @Override
//    public void afterVariableChanged(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull SchedulingProducingArrangement schedulingProducingArrangement
//    ) {
//        process(scoreDirector, schedulingProducingArrangement);
//    }
//
//}
