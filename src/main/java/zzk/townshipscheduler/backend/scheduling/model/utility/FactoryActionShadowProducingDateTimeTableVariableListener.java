//package zzk.townshipscheduler.backend.scheduling.model.utility;
//
//import ai.timefold.solver.core.api.domain.variable.VariableListener;
//import ai.timefold.solver.core.api.score.director.ScoreDirector;
//import org.jspecify.annotations.NonNull;
//import zzk.townshipscheduler.backend.ProducingStructureType;
//import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryTimeSlotInstance;
//import zzk.townshipscheduler.backend.scheduling.model.SchedulingPlayerFactoryAction;
//import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;
//
//import java.time.Duration;
//import java.time.LocalDateTime;
//import java.util.*;
//import java.util.stream.Collectors;
//
//public class FactoryActionShadowProducingDateTimeTableVariableListener
//        implements VariableListener<TownshipSchedulingProblem, SchedulingFactoryTimeSlotInstance> {
//
//    @Override
//    public void beforeVariableChanged(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull SchedulingFactoryTimeSlotInstance timeslotFactory
//    ) {
//
//    }
//
//    @Override
//    public void afterVariableChanged(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull SchedulingFactoryTimeSlotInstance timeslotFactory
//    ) {
//        doUpdate(scoreDirector, timeslotFactory);
//    }
//
//    private void doUpdate(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull SchedulingFactoryTimeSlotInstance timeslotFactory
//    ) {
//        Map<SchedulingPlayerFactoryAction, LocalDateTime> shadowProducingDateTimeTable = new LinkedHashMap<>();
//        List<SchedulingPlayerFactoryAction> inversedPlanningActionList =
//                timeslotFactory.getInversedPlanningActionList()
//                        .stream()
//                        .sorted()
//                        .collect(Collectors.toCollection(ArrayList::new));
//
//        for (SchedulingPlayerFactoryAction action : inversedPlanningActionList) {
//            LocalDateTime producingDateTime = timeslotFactory.calcProducingDateTime(action);
//            shadowProducingDateTimeTable.put(action, producingDateTime);
//        }
//
//        scoreDirector.beforeVariableChanged(timeslotFactory, "shadowProducingDateTimeTable");
//        timeslotFactory.setShadowProducingDateTimeTable(shadowProducingDateTimeTable);
//        scoreDirector.afterVariableChanged(timeslotFactory, "shadowProducingDateTimeTable");
//    }
//
////    private LocalDateTime calcProducingDataTime(
////            SchedulingFactoryTimeSlotInstance planningPeriodFactory,
////            SchedulingPlayerFactoryAction previousAction
////    ) {
////        if (planningPeriodFactory == null) {
////            return null;
////        }
////
////        ProducingStructureType producingStructureType = planningPeriodFactory.getProducingStructureType();
////        LocalDateTime periodFactoryStart = planningPeriodFactory.getDateTimeSlot().getStart();
////        if (producingStructureType == ProducingStructureType.SLOT) {
////            return periodFactoryStart;
////        } else if (producingStructureType == ProducingStructureType.QUEUE) {
////            Duration finishDurationFromPreviousCompleted
////                    = previousAction.nextAvailableAsDuration(periodFactoryStart);
////            return finishDurationFromPreviousCompleted == null
////                    ? null
////                    : periodFactoryStart.plus(finishDurationFromPreviousCompleted);
////        } else {
////            return periodFactoryStart;
////        }
////    }
//
//    @Override
//    public void beforeEntityAdded(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull SchedulingFactoryTimeSlotInstance timeslotFactory
//    ) {
//
//    }
//
//    @Override
//    public void afterEntityAdded(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull SchedulingFactoryTimeSlotInstance timeslotFactory
//    ) {
//        doUpdate(scoreDirector, timeslotFactory);
//    }
//
//    @Override
//    public void beforeEntityRemoved(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull SchedulingFactoryTimeSlotInstance timeslotFactory
//    ) {
//
//    }
//
//    @Override
//    public void afterEntityRemoved(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull SchedulingFactoryTimeSlotInstance timeslotFactory
//    ) {
//
//    }
//
//}
