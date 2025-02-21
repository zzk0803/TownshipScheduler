package zzk.townshipscheduler.backend.scheduling.score;


import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.core.api.score.stream.*;
import zzk.townshipscheduler.backend.scheduling.model.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
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
                //SOFT-MAKESPAN
                preferMinimizeMakeSpan(constraintFactory),
                //SOFT-BATTER
                //preferLoadBalanceForFactoryInstance(constraintFactory),
                //preferArrangeInSameTimeGroupingMore(constraintFactory),
                //preferArrangeNotInSleepTime(constraintFactory)
        };
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
                .expand((factoryInstance, factoryActions) -> {
                    return factoryActions.stream()
                            .sorted()
                            .map(SchedulingPlayerFactoryAction::calcAccumulatedConsequence)
                            .flatMap(Collection::stream)
                            .filter(consequence -> consequence.getResource() instanceof ActionConsequence.FactoryWaitQueue)
                            .sorted()
                            .toList();
                })
                .filter((factoryInstance, factoryActions, actionConsequences) -> {
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
                        5000
                ))
                .asConstraint("forbidArrangeOutOfFactoryProducingCapacity");
    }

    private Constraint forbidArrangeOutOfProductStock(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingPlayerFactoryAction.class)
                .groupBy(
                        SchedulingPlayerFactoryAction::getSchedulingWarehouse,
                        ConstraintCollectors.toList()
                )
                .filter((schedulingWarehouse, factoryActions) -> {
                    Map<SchedulingProduct, Integer> productStockMap = new LinkedHashMap<>(schedulingWarehouse.getProductAmountMap());
                    List<ActionConsequence> consequences = factoryActions.stream()
                            .sorted()
                            .map(SchedulingPlayerFactoryAction::calcAccumulatedConsequence)
                            .flatMap(Collection::stream)
                            .filter(consequence -> consequence.getResource() instanceof ActionConsequence.ProductStock)
                            .sorted()
                            .toList();
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

    private Constraint preferMinimizeMakeSpan(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(SchedulingPlayerFactoryAction.class)
                .map(SchedulingPlayerFactoryAction::getShadowGameCompleteDateTime)
                .join(SchedulingWorkTimeLimit.class)
                .penalize(
                        BendableScore.ofSoft(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.SOFT_MAKE_SPAN,
                                1
                        ), (localDateTime, workTimeLimit) -> {
                            LocalDateTime startDateTime = workTimeLimit.getStartDateTime();
                            LocalDateTime completeDateTime = localDateTime;
                            return Math.toIntExact(Duration.between(startDateTime, completeDateTime).toMinutes());
                        }
                )
                .asConstraint("preferMinimizeMakeSpan");
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

}
