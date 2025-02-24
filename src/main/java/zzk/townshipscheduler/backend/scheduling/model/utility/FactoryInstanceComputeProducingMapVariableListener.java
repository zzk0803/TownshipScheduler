//package zzk.townshipscheduler.backend.scheduling.model.utility;
//
//import ai.timefold.solver.core.api.domain.variable.VariableListener;
//import ai.timefold.solver.core.api.score.director.ScoreDirector;
//import org.jspecify.annotations.NonNull;
//import zzk.townshipscheduler.backend.ProducingStructureType;
//import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInstance;
//import zzk.townshipscheduler.backend.scheduling.model.SchedulingPlayerFactoryAction;
//import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;
//
//import java.time.Duration;
//import java.time.LocalDateTime;
//import java.util.*;
//
//public class FactoryInstanceComputeProducingMapVariableListener
//        implements VariableListener<TownshipSchedulingProblem, SchedulingFactoryInstance> {
//
//    @Override
//    public void beforeEntityAdded(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull SchedulingFactoryInstance factoryInstance
//    ) {
//
//    }
//
//    @Override
//    public void afterEntityAdded(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull SchedulingFactoryInstance factoryInstance
//    ) {
//        doUpdate(scoreDirector, factoryInstance);
//    }
//
//    @Override
//    public void beforeEntityRemoved(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull SchedulingFactoryInstance factoryInstance
//    ) {
//
//    }
//
//    @Override
//    public void afterEntityRemoved(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull SchedulingFactoryInstance factoryInstance
//    ) {
//        doUpdate(scoreDirector, factoryInstance);
//    }
//
//    private void doUpdate(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull SchedulingFactoryInstance factoryInstance
//    ) {
//        Result computedResult = listToComputedMap(factoryInstance);
//        scoreDirector.beforeVariableChanged(factoryInstance, "actionProducingMap");
//        factoryInstance.setActionProducingMap(computedResult.actionProducingMap());
//        scoreDirector.afterVariableChanged(factoryInstance, "actionProducingMap");
//
//        scoreDirector.beforeVariableChanged(factoryInstance, "actionCompletedMap");
//        factoryInstance.setActionProducingMap(computedResult.actionCompletedMap());
//        scoreDirector.afterVariableChanged(factoryInstance, "actionCompletedMap");
//
//    }
//
//    public Result listToComputedMap(
//            SchedulingFactoryInstance factoryInstance
//    ) {
//
//        List<SchedulingPlayerFactoryAction> sortedActionList = factoryInstance.getInversedPlanningActionList()
//                .stream()
//                .sorted(Comparator.comparing(SchedulingPlayerFactoryAction::getPlanningArrangeDateTimeSlot))
//                .toList();
//
//        return getResult(factoryInstance, sortedActionList);
//    }
//
//    private Result getResult(
//            SchedulingFactoryInstance factoryInstance,
//            List<SchedulingPlayerFactoryAction> sortedActionList
//    ) {
//        SortedMap<SchedulingPlayerFactoryAction, LocalDateTime> actionProducingMap
//                = new TreeMap<>(
//                Comparator.comparing(SchedulingPlayerFactoryAction::getPlanningArrangeDateTimeSlot)
//                        .thenComparingInt(SchedulingPlayerFactoryAction::getPlanningSequence)
//        );
//
//        SortedMap<SchedulingPlayerFactoryAction, LocalDateTime> actionCompletedMap
//                = new TreeMap<>(
//                Comparator.comparing(SchedulingPlayerFactoryAction::getPlanningArrangeDateTimeSlot)
//                        .thenComparingInt(SchedulingPlayerFactoryAction::getPlanningSequence)
//        );
//        ;
//
//        Iterator<SchedulingPlayerFactoryAction> actionIterator = sortedActionList.iterator();
//        SchedulingPlayerFactoryAction firstAction = null;
//        Duration firstActionDuration = null;
//        SchedulingPlayerFactoryAction previousAction = null;
//        Duration previousActionDuration = null;
//        SchedulingPlayerFactoryAction iteratingAction = null;
//        Duration iteratingActionDuration = null;
//        if (actionIterator.hasNext()) {
//            firstAction = sortedActionList.getFirst();
//            firstActionDuration = firstAction.getProducingDuration();
//        }
//        while (actionIterator.hasNext()) {
//            iteratingAction = actionIterator.next();
//            iteratingActionDuration = iteratingAction.getProducingDuration();
//            if (iteratingAction == firstAction) {
//                LocalDateTime arrangeDateTime = iteratingAction.getPlanningArrangeDateTimeSlot().getStart();
//                actionProducingMap.put(iteratingAction, arrangeDateTime);
//                actionCompletedMap.put(iteratingAction, arrangeDateTime.plus(firstActionDuration));
//            } else {
//                LocalDateTime arrangeDateTime
//                        = iteratingAction.getPlanningArrangeDateTimeSlot().getStart();
//                LocalDateTime previousActionCompletedDateTime
//                        = actionCompletedMap.get(previousAction);
//                LocalDateTime computedProducingDateTime = null;
//                if (factoryInstance.getProducingStructureType() == ProducingStructureType.SLOT) {
//                    computedProducingDateTime = arrangeDateTime;
//                } else {
//                    computedProducingDateTime =
//                            previousActionCompletedDateTime == null ||
//                            previousActionCompletedDateTime.isBefore(arrangeDateTime)
//                                    ? arrangeDateTime
//                                    : previousActionCompletedDateTime;
//                }
//                actionProducingMap.put(iteratingAction, computedProducingDateTime);
//                actionCompletedMap.put(iteratingAction, computedProducingDateTime.plus(iteratingActionDuration));
//            }
//            previousAction = iteratingAction;
//            previousActionDuration = iteratingActionDuration;
//        }
//        return new Result(actionProducingMap, actionCompletedMap);
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
//            @NonNull SchedulingFactoryInstance factoryInstance
//    ) {
//
//    }
//
//    @Override
//    public void afterVariableChanged(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull SchedulingFactoryInstance factoryInstance
//    ) {
//        doUpdate(scoreDirector, factoryInstance);
//    }
//
//    private record Result(
//            SortedMap<SchedulingPlayerFactoryAction, LocalDateTime> actionProducingMap,
//            SortedMap<SchedulingPlayerFactoryAction, LocalDateTime> actionCompletedMap
//    ) {
//
//    }
//
//}
