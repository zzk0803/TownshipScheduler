//package zzk.townshipscheduler.backend.scheduling.model.utility;
//
//import ai.timefold.solver.core.api.domain.variable.VariableListener;
//import ai.timefold.solver.core.api.score.director.ScoreDirector;
//import org.jspecify.annotations.NonNull;
//import zzk.townshipscheduler.backend.scheduling.model.SchedulingDateTimeSlot;
//import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInstance;
//import zzk.townshipscheduler.backend.scheduling.model.SchedulingPlayerFactoryAction;
//import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;
//
//import java.time.Duration;
//import java.time.LocalDateTime;
//import java.util.Comparator;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//public class DataTimeSlotRollingAndPushVariableListener
//        implements VariableListener<TownshipSchedulingProblem, SchedulingDateTimeSlot> {
//
//    @Override
//    public void beforeVariableChanged(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull SchedulingDateTimeSlot dateTimeSlot
//    ) {
//
//    }
//
//    @Override
//    public void afterVariableChanged(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull SchedulingDateTimeSlot dateTimeSlot
//    ) {
//        doUpdate(scoreDirector, dateTimeSlot);
//    }
//
//    @Override
//    public void beforeEntityAdded(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull SchedulingDateTimeSlot dateTimeSlot
//    ) {
//
//    }
//
//    @Override
//    public void afterEntityAdded(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull SchedulingDateTimeSlot dateTimeSlot
//    ) {
//        doUpdate(scoreDirector, dateTimeSlot);
//    }
//
//    private static void doUpdate(
//            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            SchedulingDateTimeSlot dateTimeSlot
//    ) {
//        List<SchedulingPlayerFactoryAction> dateTimeSlotActions
//                = dateTimeSlot.getPlanningFactoryActionList();
//        Map<SchedulingFactoryInstance, List<SchedulingPlayerFactoryAction>> factoryActionsMap
//                = dateTimeSlotActions.stream()
//                .collect(
//                        Collectors.groupingBy(SchedulingPlayerFactoryAction::getPlanningFactory)
//                );
//        factoryActionsMap.forEach((factoryInstance, slotFactoryActions) -> {
//            List<SchedulingPlayerFactoryAction> factoryActions = factoryInstance.getPlanningFactoryActionList();
//            LocalDateTime dateTimeSlotStart = dateTimeSlot.getStart();
//            Duration accumulatedDuration = Duration.ZERO;
//            List<SchedulingPlayerFactoryAction> sortedSlotActions
//                    = slotFactoryActions.stream()
//                    .sorted(Comparator.comparing(SchedulingPlayerFactoryAction::getActionId))
//                    .toList();
//            for (SchedulingPlayerFactoryAction action : sortedSlotActions) {
//                Duration producingDuration = action.getProducingDuration();
//                action.acceptComputedDateTime(
//                        dateTimeSlotStart.plus(accumulatedDuration),
//                        dateTimeSlotStart.plus(accumulatedDuration).plus(producingDuration)
//                );
//                accumulatedDuration.plus(producingDuration);
//            }
//        });
//        scoreDirector.beforeVariableChanged(dateTimeSlot, SchedulingDateTimeSlot.SHADOW_ROLLING_CHANGE);
//        dateTimeSlot.setShadowRollingChange(dateTimeSlot.getShadowRollingChange() + 1);
//        scoreDirector.afterVariableChanged(dateTimeSlot, SchedulingDateTimeSlot.SHADOW_ROLLING_CHANGE);
//    }
//
//    @Override
//    public void beforeEntityRemoved(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull SchedulingDateTimeSlot dateTimeSlot
//    ) {
//
//    }
//
//    @Override
//    public void afterEntityRemoved(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull SchedulingDateTimeSlot dateTimeSlot
//    ) {
//
//    }
//
//}
