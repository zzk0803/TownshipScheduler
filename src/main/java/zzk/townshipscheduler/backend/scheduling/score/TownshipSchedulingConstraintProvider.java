package zzk.townshipscheduler.backend.scheduling.score;


import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.core.api.score.stream.*;
import zzk.townshipscheduler.backend.scheduling.model.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class TownshipSchedulingConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[]{
                //HARD-BROKEN
                //forbidMismatchFactory(constraintFactory),
                //forbidMismatchExecutionMode(constraintFactory),
                forbidArrangeOutOfFactoryProducingCapacity(constraintFactory),
                forbidArrangeOutOfProductStock(constraintFactory),
                //forbidArrangeBrokenPrerequisite(constraintFactory),
                forbidExceedOrderDueDateTime(constraintFactory),
                //HARD-REVISE
                //considerNextSchedulingForUncertainExecutionMode(constraintFactory),
                //SOFT-ASSIGN
                shouldEveryActionHasAssignedFactory(constraintFactory),
                shouldEveryActionNotInSameSequence(constraintFactory),
                //SOFT-MAKESPAN
                preferMinimizeMakeSpan(constraintFactory),
                //SOFT-BATTER
                preferSameProductItsActionInSameTimeslot(constraintFactory),
                preferArrangeNotInSleepTime(constraintFactory)
                //preferLoadBalanceForFactoryInstance(constraintFactory),
                //preferArrangeInSameTimeGroupingMore(constraintFactory),
        };
    }

    private Constraint forbidArrangeOutOfFactoryProducingCapacity(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(SchedulingFactoryInstance.class)
                .join(
                        SchedulingPlayerFactoryAction.class,
                        Joiners.equal(
                                Function.identity(),
                                factoryAction -> factoryAction.getPlanningTimeSlotFactory().getFactoryInstance()
                        )
                )
                .groupBy(
                        (factory, action) -> factory,
                        ConstraintCollectors.toList((factory, action) -> action)
                )
                .groupBy(
                        (factory, action) -> factory,
                        (factory, actions)
                                -> actions.stream()
                                .sorted()
                                .map(SchedulingPlayerFactoryAction::calcActionConsequence)
                                .flatMap(Collection::stream)
                                .filter(consequence -> consequence.getResource() instanceof ActionConsequence.FactoryWaitQueue)
                                .sorted()
                                .toList()
                )
                .filter((factoryInstance, actionConsequences) -> {
                    int remain = factoryInstance.getProducingLength();
                    for (ActionConsequence consequence : actionConsequences) {
                        ActionConsequence.SchedulingResource resource = consequence.getResource();
                        if (resource instanceof ActionConsequence.FactoryWaitQueue) {
                            remain = consequence.getResourceChange().apply(remain);
                            if (remain < 0) {
                                return true;
                            }
                        }
                    }
                    return false;
                })
                .penalize(BendableScore.ofHard(
                        TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                        TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                        TownshipSchedulingProblem.HARD_BROKEN,
                        10000
                ))
                .asConstraint("forbidArrangeOutOfFactoryProducingCapacity");
    }


//    private Constraint forbidMismatchFactory(ConstraintFactory constraintFactory) {
//        return constraintFactory
//                .forEach(SchedulingPlayerFactoryAction.class)
//                .filter(SchedulingPlayerFactoryAction::boolFactoryMismatch)
//                .penalize(
//                        BendableScore.ofHard(
//                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
//                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
//                                TownshipSchedulingProblem.HARD_BROKEN,
//                                10000
//                        ))
//                .indictWith(List::of)
//                .justifyWith(ForbidMismatchFactoryJustification::new)
//                .asConstraint("forbidMismatchFactory");
//    }

//    private Constraint forbidMismatchExecutionMode(ConstraintFactory constraintFactory) {
//        return constraintFactory.forEach(SchedulingPlayerFactoryAction.class)
//                .filter(SchedulingPlayerFactoryAction::boolExecutionModeMismatch)
//                .penalize(
//                        BendableScore.ofHard(
//                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
//                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
//                                TownshipSchedulingProblem.HARD_BROKEN,
//                                10000
//                        )
//                ).asConstraint("forbidMismatchExecutionMode");
//    }

    private Constraint forbidArrangeOutOfProductStock(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingPlayerFactoryAction.class)
                .groupBy(
                        SchedulingPlayerFactoryAction::getSchedulingWarehouse,
                        ConstraintCollectors.toList()
                )
                .groupBy(
                        (schedulingWarehouse, factoryActions) -> schedulingWarehouse,
                        (schedulingWarehouse, factoryActions) ->
                                factoryActions.stream()
                                        .sorted()
                                        .map(SchedulingPlayerFactoryAction::calcActionConsequence)
                                        .flatMap(Collection::stream)
                                        .filter(consequence -> consequence.getResource() instanceof ActionConsequence.ProductStock)
                                        .sorted()
                                        .toList()
                )
                .filter((schedulingWarehouse, consequences) -> {
                    Map<SchedulingProduct, Integer> productStockMap = new LinkedHashMap<>(schedulingWarehouse.getProductAmountMap());
                    for (ActionConsequence consequence : consequences) {
                        ActionConsequence.ProductStock productStock = (ActionConsequence.ProductStock) consequence.getResource();
                        ActionConsequence.SchedulingResourceChange resourceChange = consequence.getResourceChange();
                        SchedulingProduct schedulingProduct = productStock.getSchedulingProduct();
                        Integer stock = productStockMap.getOrDefault(schedulingProduct, 0);
                        stock = resourceChange.apply(stock);
                        productStockMap.put(schedulingProduct, stock);
                        if (stock < 0) {
                            return true;
                        }
                    }
                    return false;
                })
                .penalize(BendableScore.ofHard(
                        TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                        TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                        TownshipSchedulingProblem.HARD_BROKEN,
                        10000

                ))
                .asConstraint("forbidArrangeOutOfProductStock");
    }

    private Constraint forbidExceedOrderDueDateTime(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(SchedulingOrder.class)
                .filter(schedulingOrder -> schedulingOrder.optionalDeadline().isPresent())
                .join(
                        SchedulingPlayerFactoryAction.class,
                        Joiners.equal(
                                SchedulingOrder::longIdentity,
                                action -> action.getTargetActionObject().longIdentity()
                        )
                )
                .filter((schedulingOrder, schedulingPlayerFactoryAction) -> {
                            LocalDateTime shadowGameCompleteDateTime = schedulingPlayerFactoryAction.getShadowGameCompleteDateTime();
                            return shadowGameCompleteDateTime != null
                                   && shadowGameCompleteDateTime.isAfter(schedulingOrder.getDeadline());
                        }
                )
                .penalize(
                        BendableScore.ofHard(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.HARD_BROKEN,
                                100

                        ),
                        (schedulingOrder, action) -> {
                            LocalDateTime deadline = schedulingOrder.getDeadline();
                            LocalDateTime completeDateTime = action.getShadowGameCompleteDateTime();
                            return Math.toIntExact(Duration.between(deadline, completeDateTime).toMinutes());
                        }
                )
                .asConstraint("forbidExceedOrderDueDateTime");
    }

    private Constraint shouldEveryActionHasAssignedFactory(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachIncludingUnassigned(SchedulingPlayerFactoryAction.class)
                .filter(action -> action.getPlanningTimeSlotFactory() == null)
                .penalize(
                        BendableScore.ofSoft(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.SOFT_ASSIGNED,
                                1
                        )
                )
                .asConstraint("shouldEveryActionHasAssignedFactory");
    }

    private Constraint shouldEveryActionNotInSameSequence(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(
                        SchedulingPlayerFactoryAction.class,
                        Joiners.equal(SchedulingPlayerFactoryAction::getSequence),
                        Joiners.greaterThan(SchedulingPlayerFactoryAction::getActionId)
                )
                .penalize(
                        BendableScore.ofSoft(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.SOFT_ASSIGNED,
                                1
                        )
                )
                .asConstraint("shouldEveryActionNotInSameSequence");
    }

    private Constraint preferMinimizeMakeSpan(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(SchedulingPlayerFactoryAction.class)
                .penalize(
                        BendableScore.ofSoft(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.SOFT_MAKE_SPAN,
                                1
                        ), (factoryAction) -> {
                            LocalDateTime startDateTime = factoryAction.getWorkTimeLimit().getStartDateTime();
                            LocalDateTime completeDateTime = factoryAction.getShadowGameCompleteDateTime();
                            return Math.toIntExact(Duration.between(startDateTime, completeDateTime).toMinutes());
                        }
                )
                .asConstraint("preferMinimizeMakeSpan");
    }

//    private Constraint forbidArrangeBrokenPrerequisite(ConstraintFactory constraintFactory) {
//        return constraintFactory
//                .forEach(SchedulingPlayerFactoryAction.class)
//                .filter(SchedulingPlayerFactoryAction::boolArrangeBeforePrerequisiteDone)
//                .penalize(BendableScore.ofHard(
//                        TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
//                        TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
//                        TownshipSchedulingProblem.HARD_BROKEN,
//                        5000
//
//                ))
//                .asConstraint("forbidArrangeBrokenPrerequisite");
//    }

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

    private Constraint preferSameProductItsActionInSameTimeslot(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingPlayerFactoryAction.class)
                .groupBy(
                        SchedulingPlayerFactoryAction::getSchedulingProduct,
                        ConstraintCollectors.toList(action -> action.getPlanningTimeSlotFactory().getDateTimeSlot())
                )
                .penalize(
                        BendableScore.ofSoft(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.SOFT_BATTER,
                                1
                        ), (product, dateTimeSlots) -> dateTimeSlots.size()
                )
                .asConstraint("preferSameProductItsActionInSameTimeslot");
    }

    private Constraint preferArrangeNotInSleepTime(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingPlayerFactoryAction.class)
                .map(SchedulingPlayerFactoryAction::getArrangeDateTimeSlot)
                .filter(timeSlot -> {
                    LocalTime localTime = timeSlot.getStart().toLocalTime();
                    LocalTime sleepStart = LocalTime.MIDNIGHT.minusHours(1);
                    LocalTime sleepEnd = LocalTime.MIDNIGHT.plusHours(7);
                    return localTime.isAfter(sleepStart) && localTime.isBefore(sleepEnd);
                })
                .penalize(BendableScore.ofSoft(
                        TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                        TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                        TownshipSchedulingProblem.SOFT_BATTER,
                        10000
                ))
                .asConstraint("preferArrangeNotInSleepTime");
    }

}
