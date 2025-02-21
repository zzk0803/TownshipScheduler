//package zzk.townshipscheduler.backend.scheduling.model.utility;
//
//import ai.timefold.solver.core.api.domain.variable.VariableListener;
//import ai.timefold.solver.core.api.score.director.ScoreDirector;
//import lombok.extern.slf4j.Slf4j;
//import org.jspecify.annotations.NonNull;
//import zzk.townshipscheduler.backend.ProducingStructureType;
//import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryTimeSlotInstance;
//import zzk.townshipscheduler.backend.scheduling.model.SchedulingPlayerFactoryAction;
//import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;
//
//import java.time.Duration;
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.Comparator;
//import java.util.List;
//import java.util.Objects;
//import java.util.stream.Collectors;
//
//@Slf4j
//public class FactoryActionShadowGameProducingDataTimeVariableListener
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
//        doUpdateShadowVariable(scoreDirector, factoryAction);
//    }
//
//    private void doUpdateShadowVariable(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull SchedulingPlayerFactoryAction schedulingPlayerFactoryAction
//    ) {
//        SchedulingFactoryTimeSlotInstance planningFactory = schedulingPlayerFactoryAction.getSchedulingFactoryTimeSlotInstance();
//        if (Objects.isNull(planningFactory)) {
//            scoreDirector.beforeVariableChanged(schedulingPlayerFactoryAction, "shadowGameProducingDataTime");
//            schedulingPlayerFactoryAction.setShadowGameProducingDataTime(null);
//            scoreDirector.afterVariableChanged(schedulingPlayerFactoryAction, "shadowGameProducingDataTime");
//            return;
//        }
//
//        List<SchedulingPlayerFactoryAction> inversedPlanningActionList
//                = planningFactory.getInversedPlanningActionList()
//                .stream()
//                .sorted(Comparator.comparing(SchedulingPlayerFactoryAction::getSequence))
//                .collect(Collectors.toCollection(ArrayList::new));
//        Integer schedulingPlayerFactoryActionSequence = schedulingPlayerFactoryAction.getSequence();
////        int affectFollowingIndex = Collections.binarySearch(planningActionList, schedulingPlayerFactoryAction);
//
//        if (inversedPlanningActionList.isEmpty()) {
//            scoreDirector.beforeVariableChanged(schedulingPlayerFactoryAction, "shadowGameProducingDataTime");
//            schedulingPlayerFactoryAction.setShadowGameProducingDataTime(schedulingPlayerFactoryAction.getArrangeDateTimeSlot()
//                    .getStart());
//            scoreDirector.afterVariableChanged(schedulingPlayerFactoryAction, "shadowGameProducingDataTime");
//        } else {
//            for (SchedulingPlayerFactoryAction factoryAction : inversedPlanningActionList) {
//                Integer iteratingActionSequence = factoryAction.getSequence();
//                if (iteratingActionSequence < schedulingPlayerFactoryActionSequence) {
//                    continue;
//                }
//
//                LocalDateTime computedProducingDataTime = calcShadowGameProducingDataTime(
//                        planningFactory,
//                        factoryAction
//                );
//
//                LocalDateTime currentProducingDateTimeOfIterating = factoryAction.getShadowGameProducingDataTime();
//                if (currentProducingDateTimeOfIterating.isEqual(computedProducingDataTime)) {
//                    break;
//                } else {
//                    scoreDirector.beforeVariableChanged(factoryAction, "shadowGameProducingDataTime");
//                    factoryAction.setShadowGameProducingDataTime(computedProducingDataTime);
//                    scoreDirector.afterVariableChanged(factoryAction, "shadowGameProducingDataTime");
//                }
//
//            }
//        }
//    }
//
//    private LocalDateTime calcShadowGameProducingDataTime(
//            SchedulingFactoryTimeSlotInstance planningPeriodFactory,
//            SchedulingPlayerFactoryAction planningPrevious
//    ) {
//        if (planningPeriodFactory == null) {
//            return null;
//        }
//
//        ProducingStructureType producingStructureType = planningPeriodFactory.getProducingStructureType();
//        LocalDateTime periodFactoryStart = planningPeriodFactory.getDateTimeSlot().getStart();
//        if (producingStructureType == ProducingStructureType.SLOT) {
//            return periodFactoryStart;
//        } else if (producingStructureType == ProducingStructureType.QUEUE) {
//            Duration finishDurationFromFactory
//                    = planningPeriodFactory.nextAvailableAsDuration(periodFactoryStart);
//            Duration finishDurationFromPreviousCompleted
//                    = planningPrevious.nextAvailableAsDuration(periodFactoryStart);
//            return (finishDurationFromFactory == null || finishDurationFromPreviousCompleted == null)
//                    ? null
//                    : periodFactoryStart.plus(
//                            finishDurationFromFactory.compareTo(finishDurationFromPreviousCompleted) >= 0
//                                    ? finishDurationFromFactory
//                                    : finishDurationFromPreviousCompleted
//                    );
//        } else {
//            return periodFactoryStart;
//        }
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
//        //doUpdateShadowVariable(scoreDirector, factoryAction);
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
//
//}
