package zzk.townshipscheduler.backend.scheduling.score;


import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.core.api.score.stream.*;
import zzk.townshipscheduler.backend.scheduling.model.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.function.Function;

public class TownshipSchedulingConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[]{
                //HARD-BROKEN
                forbidMismatchFactory(constraintFactory),
                forbidMismatchExecutionMode(constraintFactory),
                forbidArrangeOutOfFactoryProducingCapacity(constraintFactory),
                forbidArrangeBrokenPrerequisite(constraintFactory),
                forbidExceedOrderDueDateTime(constraintFactory),
                //HARD-REVISE
//                considerNextSchedulingForUncertainExecutionMode(constraintFactory),
                //SOFT-MAKESPAN
                preferMinimizeMakeSpan(constraintFactory),
                //SOFT-BATTER
                //preferLoadBalanceForFactoryInstance(constraintFactory),
                //preferArrangeInSameTimeGroupingMore(constraintFactory),
                //preferArrangeNotInSleepTime(constraintFactory)
        };
    }

    private Constraint forbidMismatchFactory(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingPlayerFactoryAction.class)
                .filter(SchedulingPlayerFactoryAction::boolFactoryMismatch).
                penalize(
                        BendableScore.ofHard(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.HARD_BROKEN,
                                10000
                        )
                ).asConstraint("forbidMismatchFactory");
    }

    private Constraint forbidMismatchExecutionMode(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingPlayerFactoryAction.class)
                .filter(SchedulingPlayerFactoryAction::boolExecutionModeMismatch)
                .penalize(
                        BendableScore.ofHard(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.HARD_BROKEN,
                                10000
                        )
                ).asConstraint("forbidMismatchExecutionMode");
    }

    private Constraint forbidArrangeOutOfFactoryProducingCapacity(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(SchedulingPlayerFactoryAction.class)
                .filter(SchedulingPlayerFactoryAction::boolArrangeOutOfFactoryProducingCapacity)
                .penalize(BendableScore.ofHard(
                        TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                        TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                        TownshipSchedulingProblem.HARD_BROKEN,
                        1

                )).asConstraint("forbidArrangeOutOfFactoryProducingCapacity");
    }

    private Constraint forbidArrangeBrokenPrerequisite(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingPlayerFactoryAction.class)
                .filter(schedulingPlayerFactoryAction -> !schedulingPlayerFactoryAction.boolArrangeValidForPrerequisite())
                .penalize(BendableScore.ofHard(
                        TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                        TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                        TownshipSchedulingProblem.HARD_BROKEN,
                        1

                )).asConstraint("forbidArrangeBrokenPrerequisite");

    }


    private Constraint forbidExceedOrderDueDateTime(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingOrder.class)
                .filter(schedulingOrder -> schedulingOrder.optionalDeadline().isPresent())
                .join(
                        SchedulingPlayerFactoryAction.class,
                        Joiners.equal(
                                SchedulingOrder::readable,
                                action -> action.getCurrentActionObject().readable()
                        )
                ).filter((schedulingOrder, schedulingPlayerFactoryAction) -> {
                    LocalDateTime shadowGameCompleteDateTime = schedulingPlayerFactoryAction.getShadowGameCompleteDateTime();
                    return shadowGameCompleteDateTime != null
                           && shadowGameCompleteDateTime.isAfter(schedulingOrder.getDeadline());
                }).penalize(BendableScore.ofHard(
                        TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                        TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                        TownshipSchedulingProblem.HARD_BROKEN,
                        1

                )).asConstraint("forbidExceedOrderDueDateTime");
    }

//    private Constraint considerNextSchedulingForUncertainExecutionMode(ConstraintFactory constraintFactory) {
//        return constraintFactory.forEach(SchedulingProduct.class)
//                .filter(schedulingProduct -> schedulingProduct.getExecutionModeSet().size() > 1)
//                .ifNotExists(
//                        SchedulingPlayerFactoryAction.class,
//                        Joiners.equal(
//                                SchedulingProduct::readable,
//                                action -> action.getCurrentActionObject().readable()
//                        )
//                ).penalize(BendableScore.ofHard(
//                        TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
//                        TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
//                        TownshipSchedulingProblem.HARD_NEED_REVISE,
//                        1
//
//                )).asConstraint("considerNextSchedulingForUncertainExecutionMode");
//    }

    private Constraint preferMinimizeMakeSpan(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingPlayerFactoryAction.class)
                .filter(schedulingPlayerFactoryAction -> {
                    return schedulingPlayerFactoryAction.getPlanningFactory() != null
                           && schedulingPlayerFactoryAction.getPlanningNext() == null
                           && schedulingPlayerFactoryAction.getShadowGameCompleteDateTime() != null
                            ;
                })
                .join(SchedulingWorkTimeLimit.class)
                .penalize(
                        BendableScore.ofSoft(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.SOFT_MAKESPAN,
                                1
                        ), (action, workTimeLimit) -> {
                            LocalDateTime startDateTime = workTimeLimit.getStartDateTime();
                            LocalDateTime completeDateTime = action.getShadowGameCompleteDateTime();
                            return Math.toIntExact(Duration.between(startDateTime, completeDateTime).toMinutes());
                        }
                ).asConstraint("preferMinimizeMakeSpan");
    }

}
