package zzk.townshipscheduler.backend.scheduling.score;


import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.*;
import zzk.townshipscheduler.backend.scheduling.model.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;

public class TownshipSchedulingConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[]{
                //HARD-BROKEN
//                forbidMismatchFactory(constraintFactory),
                //forbidMismatchExecutionMode(constraintFactory),
                shouldEveryActionNotInSameSequence(constraintFactory),
                forbidCompositeProducingBeforeOrEqualMaterialProducing(constraintFactory),
                forbidArrangeOutOfFactoryProducingCapacity(constraintFactory),
//                forbidArrangeOutOfProductStock(constraintFactory),
                //forbidArrangeBrokenPrerequisite(constraintFactory),
                forbidExceedOrderDueDateTime(constraintFactory),
//                forbidSequenceLessThanEarlyTimeSlotMaxSequence(constraintFactory),
                //HARD-REVISE
                //considerNextSchedulingForUncertainExecutionMode(constraintFactory),
                //SOFT-ASSIGN
//                shouldEveryActionHasAssignedFactory(constraintFactory),
                //SOFT-MAKESPAN
                preferEarlyArrange(constraintFactory),
                //SOFT-BATTER
                preferArrangeNotInSleepTime(constraintFactory)
                //preferLoadBalanceForFactoryInstance(constraintFactory),
                //preferArrangeInSameTimeGroupingMore(constraintFactory),
        };
    }

//    private Constraint forbidMismatchFactory(ConstraintFactory constraintFactory) {
//        return constraintFactory.forEach(SchedulingPlayerFactoryAction.class)
//                .filter(SchedulingPlayerFactoryAction::boolFactoryMismatch)
//                .groupBy(ConstraintCollectors.count())
//                .penalize(BendableScore.ofHard(
//                        TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
//                        TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
//                        TownshipSchedulingProblem.HARD_BAD_ASSIGN,
//                        600
//                ),
//                        Math::toIntExact
//                )
//                .asConstraint("forbidMismatchFactory");
//    }

    private Constraint shouldEveryActionNotInSameSequence(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(
                        SchedulingPlayerFactoryAction.class
                )
                .groupBy(
                        SchedulingPlayerFactoryAction::getPlanningSequence,
                        ConstraintCollectors.toList()
                )
                .penalize(
                        HardSoftScore.ofHard(
                                9999
                        )
                        , (integer, factoryActions) -> factoryActions.size() * factoryActions.size()
                )
//                .penalize(
//                        BendableScore.ofHard(
//                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
//                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
//                                TownshipSchedulingProblem.HARD_BAD_ASSIGN,
//                                1
//                        )
//                        , (integer, factoryActions) -> factoryActions.size() * factoryActions.size()
//                )
                .asConstraint("shouldEveryActionNotInSameSequence");
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

    private Constraint forbidArrangeOutOfFactoryProducingCapacity(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingPlayerFactoryAction.class)
                .join(
                        SchedulingFactoryInstance.class,
                        Joiners.equal(SchedulingPlayerFactoryAction::getPlanningFactory, Function.identity())
                )
                .join(
                        SchedulingDateTimeSlot.class,
                        Joiners.equal((a, b) -> a.getPlanningDateTimeSlot(), Function.identity())
                )
                .expand((action, factoryInstance, dateTimeSlot) -> factoryInstance.calcRemainProducingQueueSize(
                        dateTimeSlot.getStart()))
                .filter((action, factoryInstance, dateTimeSlot, remainSize) -> {
                    int tempSize = remainSize;
                    List<ActionConsequence.SchedulingResourceChange> resourceChanges = action.calcActionConsequence()
                            .stream()
                            .filter(consequence -> consequence.getResource().getRoot() == factoryInstance)
                            .filter(consequence -> consequence.getResource() instanceof ActionConsequence.FactoryProducingQueue)
                            .filter(consequence -> consequence.getLocalDateTime().isEqual(dateTimeSlot.getStart()))
                            .map(ActionConsequence::getResourceChange)
                            .toList();
                    for (ActionConsequence.SchedulingResourceChange resourceChange : resourceChanges) {
                        if ((tempSize = resourceChange.apply(tempSize)) < 0) {
                            return true;
                        }
                    }
                    return false;
                })
                .groupBy(ConstraintCollectors.toList((factoryInstance, dateTimeSlot, remainSize, action) -> action))
                .penalize(
                        HardSoftScore.ofHard(
                                800
                        )
                )
//                .penalize(
//                        BendableScore.ofHard(
//                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
//                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
//                                TownshipSchedulingProblem.HARD_BROKEN_QUEUE,
//                                600
//                        ),
//                        (actions) -> actions.size() * actions.size()
//                )
                .asConstraint("forbidArrangeOutOfFactoryProducingCapacity");
    }

//    private Constraint forbidArrangeOutOfProductStock(ConstraintFactory constraintFactory) {
//        return constraintFactory.forEach(SchedulingPlayerFactoryAction.class)
//                .groupBy(
//                        SchedulingPlayerFactoryAction::getSchedulingWarehouse,
//                        ConstraintCollectors.toList()
//                )
//                .filter((schedulingWarehouse, factoryActions) -> {
//                    List<ActionConsequence> productStockConsequence
//                            = schedulingWarehouse.mapToActionProductStockConsequences(factoryActions);
//
//                    Map<SchedulingProduct, Integer> productStockMap = new LinkedHashMap<>(schedulingWarehouse.getProductAmountMap());
//                    for (ActionConsequence consequence : productStockConsequence) {
//                        ActionConsequence.ProductStock productStock = (ActionConsequence.ProductStock) consequence.getResource();
//                        ActionConsequence.SchedulingResourceChange resourceChange = consequence.getResourceChange();
//                        SchedulingProduct schedulingProduct = productStock.getSchedulingProduct();
//                        Integer stock = productStockMap.getOrDefault(schedulingProduct, 0);
//                        stock = resourceChange.apply(stock);
//                        if (stock < 0) {
//                            return true;
//                        }
//                        productStockMap.put(schedulingProduct, stock);
//                    }
//                    return false;
//                })
//                .penalize(
//                        HardSoftScore.ofHard(
//                                900
//                        ),
//                        (schedulingWarehouse, factoryActions) -> factoryActions.size() * factoryActions.size()
//                )
////                .penalize(
////                        BendableScore.ofHard(
////                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
////                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
////                                TownshipSchedulingProblem.HARD_BROKEN_STOCK,
////                                99999
////                        )
////                )
//                .asConstraint("forbidArrangeOutOfProductStock");
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
                            return shadowGameCompleteDateTime == null || shadowGameCompleteDateTime.isAfter(schedulingOrder.getDeadline());
                        }
                )
                .penalize(
                        HardSoftScore.ofSoft(
                                10000
                        ),
                        (schedulingOrder, action) -> {
                            LocalDateTime deadline = schedulingOrder.getDeadline();
                            LocalDateTime completeDateTime = action.getShadowGameCompleteDateTime();
                            return Math.toIntExact(Duration.between(
                                    deadline,
                                    completeDateTime == null
                                            ? action.getWorkTimeLimit().getEndDateTime()
                                            : completeDateTime
                            ).toMinutes());
                        }
                )
//                .penalize(
//                        BendableScore.ofHard(
//                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
//                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
//                                TownshipSchedulingProblem.SOFT_BATTER,
//                                10000
//                        ),
//                        (schedulingOrder, action) -> {
//                            LocalDateTime deadline = schedulingOrder.getDeadline();
//                            LocalDateTime completeDateTime = action.getShadowGameCompleteDateTime();
//                            return Math.toIntExact(Duration.between(
//                                    deadline,
//                                    completeDateTime == null
//                                            ? action.getWorkTimeLimit().getEndDateTime()
//                                            : completeDateTime
//                            ).toMinutes());
//                        }
//                )
                .asConstraint("forbidExceedOrderDueDateTime");
    }

//    private Constraint shouldEveryActionHasAssignedFactory(ConstraintFactory constraintFactory) {
//        return constraintFactory
//                .forEachIncludingUnassigned(SchedulingPlayerFactoryAction.class)
//                .filter(action -> action.getPlanningFactory() == null)
//                .penalize(
//                        BendableScore.ofSoft(
//                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
//                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
//                                TownshipSchedulingProblem.SOFT_ASSIGNED,
//                                1
//                        )
//                )
//                .asConstraint("shouldEveryActionHasAssignedFactory");
//    }

    private Constraint preferEarlyArrange(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingPlayerFactoryAction.class)
//                .penalize(
//                        BendableScore.ofSoft(
//                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
//                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
//                                TownshipSchedulingProblem.SOFT_MAKE_SPAN,
//                                100
//                        ), (factoryAction) -> {
//                            LocalDateTime workCalenderStart = factoryAction.getWorkTimeLimit().getStartDateTime();
//                            LocalDateTime arrangeDateTime = factoryAction.getPlanningPlayerArrangeDateTime();
//                            return Math.toIntExact(Duration.between(workCalenderStart, arrangeDateTime).toMinutes());
//                        }
//                )
                .penalize(
                        HardSoftScore.ofSoft(
                                300
                        ), (factoryAction) -> {
                            LocalDateTime workCalenderStart = factoryAction.getWorkTimeLimit().getStartDateTime();
                            LocalDateTime arrangeDateTime = factoryAction.getPlanningPlayerArrangeDateTime();
                            return Math.toIntExact(Duration.between(workCalenderStart, arrangeDateTime).toMinutes());
                        }
                )
                .asConstraint("preferEarlyArrange");
    }

    private Constraint preferArrangeNotInSleepTime(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingPlayerFactoryAction.class)
                .map(SchedulingPlayerFactoryAction::getPlanningPlayerArrangeDateTime)
                .filter(dateTime -> {
                    LocalTime localTime = dateTime.toLocalTime();
                    LocalTime sleepStart = LocalTime.MIDNIGHT.minusHours(1);
                    LocalTime sleepEnd = LocalTime.MIDNIGHT.plusHours(7);
                    return localTime.isAfter(sleepStart) && localTime.isBefore(sleepEnd);
                })
                .groupBy(ConstraintCollectors.count())
//                .penalize(
//                        BendableScore.ofSoft(
//                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
//                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
//                                TownshipSchedulingProblem.SOFT_BATTER,
//                                10000
//                        ), Math::toIntExact
//                )
                .penalize(
                        HardSoftScore.ofSoft(
                                200
                        ), Math::toIntExact
                )
                .asConstraint("preferArrangeNotInSleepTime");
    }

//    private Constraint preferMinimizeMakeSpan(ConstraintFactory constraintFactory) {
//        return constraintFactory
//                .forEach(SchedulingPlayerFactoryAction.class)
//                .penalize(
//                        BendableScore.ofSoft(
//                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
//                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
//                                TownshipSchedulingProblem.SOFT_MAKE_SPAN,
//                                1
//                        ), (factoryAction) -> {
//                            LocalDateTime startDateTime = factoryAction.getWorkTimeLimit().getStartDateTime();
//                            LocalDateTime completeDateTime = factoryAction.getShadowGameCompleteDateTime();
//                            return Math.toIntExact(Duration.between(startDateTime, completeDateTime).toMinutes());
//                        }
//                )
//                .asConstraint("preferMinimizeMakeSpan");
//    }

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
    private Constraint forbidCompositeProducingBeforeOrEqualMaterialProducing(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(
                        SchedulingPlayerFactoryAction.class,
                        Joiners.lessThanOrEqual(SchedulingPlayerFactoryAction::getPlanningPlayerArrangeDateTime),
                        Joiners.lessThan(SchedulingPlayerFactoryAction::getPlanningSequence),
                        Joiners.filtering((leftArrange, rightArrange) -> {
                            return leftArrange.getMaterials().containsKey(rightArrange.getSchedulingProduct());
                        })
                )
                .expand((leftAction, rightAction) -> leftAction.getSchedulingWarehouse().toProductAmountMap(leftAction))
                .filter((leftAction, rightAction,warehouseStock) -> {
                    Map<SchedulingProduct, Integer> warehouse = new HashMap<>(warehouseStock);
                    List<ActionConsequence> consequences = rightAction.calcActionConsequence()
                            .stream()
                            .filter(consequence -> consequence.getResource() instanceof ActionConsequence.ProductStock)
                            .toList();

                    for (ActionConsequence consequence : consequences) {
                        ActionConsequence.ProductStock productStock = (ActionConsequence.ProductStock) consequence.getResource();
                        ActionConsequence.SchedulingResourceChange resourceChange = consequence.getResourceChange();
                        SchedulingProduct schedulingProduct = productStock.getSchedulingProduct();
                        Integer stock = warehouse.getOrDefault(schedulingProduct, 0);
                        stock = resourceChange.apply(stock);
                        if (stock < 0) {
                            return true;
                        }
                        warehouse.put(schedulingProduct, stock);
                    }
                    return false;
                })
                .penalize(
                        HardSoftScore.ofHard(
                                999
                        ),
                        (leftAction, rightAction,warehouse) -> {
                            LocalDateTime leftWorkCalender = leftAction.getWorkTimeLimit().getEndDateTime();
                            LocalDateTime rightWorkCalender = rightAction.getWorkTimeLimit().getEndDateTime();
                            LocalDateTime leftArrangeDateTime = leftAction.getPlanningPlayerArrangeDateTime();
                            LocalDateTime rightArrangeDateTime = rightAction.getPlanningPlayerArrangeDateTime();
                            int left = Math.toIntExact(Duration.between(leftWorkCalender, leftArrangeDateTime)
                                    .toHours());
                            int right = Math.toIntExact(Duration.between(rightWorkCalender, rightArrangeDateTime)
                                    .toHours());
                            return (int) Math.sqrt(left ^ 2 + right ^ 2);
                        }
                )
//                .penalize(
//                        BendableScore.ofHard(
//                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
//                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
//                                TownshipSchedulingProblem.HARD_BROKEN_STOCK,
//                                99999
//                        )
//                )
                .asConstraint("forbidCompositeProducingBeforeOrEqualMaterialProducing");
    }

}
