//package zzk.townshipscheduler.backend.scheduling.model.utility;
//
//import ai.timefold.solver.core.api.domain.variable.VariableListener;
//import ai.timefold.solver.core.api.score.director.ScoreDirector;
//import org.jspecify.annotations.NonNull;
//import zzk.townshipscheduler.backend.scheduling.model.SchedulingPlayerFactoryAction;
//import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;
//
//import java.time.LocalDateTime;
//import java.util.ArrayDeque;
//import java.util.List;
//import java.util.Objects;
//import java.util.Queue;
//
//public class FactoryActionMaterialDoneDateTimeVariableListener
//        implements VariableListener<TownshipSchedulingProblem, SchedulingPlayerFactoryAction> {
//
//    @Override
//    public void beforeVariableChanged(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull SchedulingPlayerFactoryAction factoryAction
//    ) {
//
//    }
//
//    @Override
//    public void afterVariableChanged(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull SchedulingPlayerFactoryAction factoryAction
//    ) {
//        doUpdateAction(scoreDirector, factoryAction);
//    }
//
//    private void doUpdateAction(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull SchedulingPlayerFactoryAction factoryAction
//    ) {
//        Queue<SchedulingPlayerFactoryAction> uncheckedSuccessorQueue = new ArrayDeque<>(factoryAction.getSucceedingActions());
//        while (!uncheckedSuccessorQueue.isEmpty()) {
//            SchedulingPlayerFactoryAction successorOfAction = uncheckedSuccessorQueue.remove();
//            boolean updated = updatePredecessorsDoneDate(scoreDirector, successorOfAction);
//            if (updated) {
//                uncheckedSuccessorQueue.addAll(successorOfAction.getSucceedingActions());
//            }
//        }
//    }
//
//    private boolean updatePredecessorsDoneDate(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            SchedulingPlayerFactoryAction successorOfAction
//    ) {
//        // For the source the doneDate must be 0.
//        LocalDateTime computedDoneDate = successorOfAction.getWorkTimeLimit().getStartDateTime();
//        List<SchedulingPlayerFactoryAction> predecessorAllocations = successorOfAction.getMaterialActions();
//        for (SchedulingPlayerFactoryAction predecessorOfAllocation : predecessorAllocations) {
//            LocalDateTime completeDateTime = predecessorOfAllocation.getShadowGameCompleteDateTime();
//            computedDoneDate = completeDateTime == null
//                    ? computedDoneDate
//                    : completeDateTime.isAfter(computedDoneDate) ? completeDateTime : computedDoneDate;
//        }
//
//        LocalDateTime materialDoneDateTime = successorOfAction.getMaterialDoneDateTime();
//        if (Objects.equals(computedDoneDate, materialDoneDateTime)) {
//            return false;
//        }
//
//        scoreDirector.beforeVariableChanged(successorOfAction, "materialDoneDateTime");
//        successorOfAction.setMaterialDoneDateTime(computedDoneDate);
//        scoreDirector.afterVariableChanged(successorOfAction, "materialDoneDateTime");
//
//        return true;
//    }
//
//    @Override
//    public void beforeEntityAdded(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull SchedulingPlayerFactoryAction factoryAction
//    ) {
//
//    }
//
//    @Override
//    public void afterEntityAdded(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull SchedulingPlayerFactoryAction factoryAction
//    ) {
//        doUpdateAction(scoreDirector, factoryAction);
//    }
//
//    @Override
//    public void beforeEntityRemoved(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull SchedulingPlayerFactoryAction factoryAction
//    ) {
//
//    }
//
//    @Override
//    public void afterEntityRemoved(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull SchedulingPlayerFactoryAction factoryAction
//    ) {
//
//    }
//
//}
