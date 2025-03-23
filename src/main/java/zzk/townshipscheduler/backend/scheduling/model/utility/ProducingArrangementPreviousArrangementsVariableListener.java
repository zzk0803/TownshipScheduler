//package zzk.townshipscheduler.backend.scheduling.model.utility;
//
//import ai.timefold.solver.core.api.domain.variable.VariableListener;
//import ai.timefold.solver.core.api.score.director.ScoreDirector;
//import org.jspecify.annotations.NonNull;
//import org.springframework.util.Assert;
//import zzk.townshipscheduler.backend.scheduling.model.*;
//
//import java.util.Comparator;
//import java.util.List;
//import java.util.Objects;
//
//public class ProducingArrangementPreviousArrangementsVariableListener
//        implements VariableListener<TownshipSchedulingProblem, AbstractPlayerProducingArrangement> {
//
//    @Override
//    public void beforeVariableChanged(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull AbstractPlayerProducingArrangement producingArrangement
//    ) {
//
//    }
//
//    @Override
//    public void afterVariableChanged(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull AbstractPlayerProducingArrangement producingArrangement
//    ) {
//        doUpdate(scoreDirector, producingArrangement);
//    }
//
//    private void doUpdate(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull AbstractPlayerProducingArrangement producingArrangement
//    ) {
//        AbstractFactoryInstance factoryInstance = producingArrangement.getFactory();
//        SchedulingFactoryInfo.ArrangeSequence planningSequence = producingArrangement.getPlanningSequence();
//        SchedulingDateTimeSlot planningDateTimeSlot = producingArrangement.getPlanningDateTimeSlot();
//        if (Objects.isNull(factoryInstance) || Objects.isNull(planningSequence) || Objects.isNull(planningDateTimeSlot)) {
//            return;
//        }
//
//        TownshipSchedulingProblem townshipSchedulingProblem = scoreDirector.getWorkingSolution();
//        if (producingArrangement instanceof SchedulingPlayerProducingArrangement schedulingPlayerProducingArrangement) {
//            List<SchedulingPlayerProducingArrangement> sameFactoryProducingArrangements
//                    = townshipSchedulingProblem.getSchedulingPlayerProducingArrangement(factoryInstance);
//            Assert.isTrue(
//                    sameFactoryProducingArrangements.contains(schedulingPlayerProducingArrangement),
//                    "sameFactoryProducingArrangements.contains(schedulingPlayerProducingArrangement) is false !?"
//            );
//
//            List<SchedulingPlayerProducingArrangement> computedPreviousArrangements
//                    = sameFactoryProducingArrangements.stream()
//                    .filter(arrangement -> arrangement.getFactory() != null && arrangement.getPlanningSequence() != null)
//                    .sorted(Comparator.comparing(AbstractPlayerProducingArrangement::getPlanningDateTimeSlot))
//                    .takeWhile(arrangement -> arrangement.getPlanningDateTimeSlot().compareTo(planningDateTimeSlot) <= 0)
//                    .sorted(Comparator.comparing(AbstractPlayerProducingArrangement::getPlanningSequence))
//                    .takeWhile(arrangement -> arrangement.getPlanningSequence().compareTo(planningSequence)<0)
//                    .toList();
//
//            scoreDirector.beforeVariableChanged(
//                    schedulingPlayerProducingArrangement,
//                    AbstractPlayerProducingArrangement.SHADOW_PREVIOUS
//            );
//            schedulingPlayerProducingArrangement.setupPreviousArrangements(computedPreviousArrangements);
//            scoreDirector.afterVariableChanged(
//                    schedulingPlayerProducingArrangement,
//                    AbstractPlayerProducingArrangement.SHADOW_PREVIOUS
//            );
//
//        } else if (producingArrangement instanceof SchedulingPlayerFactoryProducingArrangement schedulingPlayerFactoryProducingArrangement) {
//            List<SchedulingPlayerFactoryProducingArrangement> sameFactoryProducingArrangements
//                    = townshipSchedulingProblem.getSchedulingPlayerFactoryProducingArrangement(factoryInstance);
//            Assert.isTrue(
//                    sameFactoryProducingArrangements.contains(schedulingPlayerFactoryProducingArrangement),
//                    "sameFactoryProducingArrangements.contains(schedulingPlayerFactoryProducingArrangement) is false !?"
//            );
//
//            List<SchedulingPlayerFactoryProducingArrangement> computedPreviousArrangements
//                    = sameFactoryProducingArrangements.stream()
//                    .filter(arrangement -> arrangement.getFactory() != null && arrangement.getPlanningSequence() != null)
//                    .sorted(Comparator.comparing(AbstractPlayerProducingArrangement::getPlanningDateTimeSlot))
//                    .takeWhile(arrangement -> arrangement.getPlanningDateTimeSlot().compareTo(planningDateTimeSlot) <= 0)
//                    .sorted(Comparator.comparing(AbstractPlayerProducingArrangement::getPlanningSequence))
//                    .takeWhile(arrangement -> arrangement.getPlanningSequence().compareTo(planningSequence)<0)
//                    .toList();
//
//            scoreDirector.beforeVariableChanged(
//                    schedulingPlayerFactoryProducingArrangement,
//                    AbstractPlayerProducingArrangement.SHADOW_PREVIOUS
//            );
//            schedulingPlayerFactoryProducingArrangement.setupPreviousArrangements(computedPreviousArrangements);
//            scoreDirector.afterVariableChanged(
//                    schedulingPlayerFactoryProducingArrangement,
//                    AbstractPlayerProducingArrangement.SHADOW_PREVIOUS
//            );
//        }
//    }
//
//    @Override
//    public void beforeEntityAdded(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull AbstractPlayerProducingArrangement producingArrangement
//    ) {
//
//    }
//
//    @Override
//    public void afterEntityAdded(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull AbstractPlayerProducingArrangement producingArrangement
//    ) {
//        doUpdate(scoreDirector, producingArrangement);
//    }
//
//    @Override
//    public void beforeEntityRemoved(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull AbstractPlayerProducingArrangement producingArrangement
//    ) {
//
//    }
//
//    @Override
//    public void afterEntityRemoved(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull AbstractPlayerProducingArrangement producingArrangement
//    ) {
//    }
//
//}
