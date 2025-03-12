package zzk.townshipscheduler.backend.scheduling.score;


import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.*;
import zzk.townshipscheduler.backend.scheduling.model.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TownshipSchedulingConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[]{
                //HARD-BROKEN
                //forbidMismatchFactory(constraintFactory),
                //forbidMismatchExecutionMode(constraintFactory),
                forbidSameSequence(constraintFactory),
                forbidFactoryBackwardActionArrangeDateTimeLessThatForward(constraintFactory),
                forbidCompositeProducingBeforeOrEqualMaterialProducing(constraintFactory),
                forbidArrangeOutOfFactoryProducingCapacity(constraintFactory),
                forbidExceedOrderDueDateTime(constraintFactory),
                //SOFT-ASSIGN
//                shouldEveryActionHasAssignedFactory(constraintFactory),
                //SOFT-MAKESPAN
//                preferEarlyArrange(constraintFactory),
                //SOFT-BATTER
                preferArrangeNotInSleepTime(constraintFactory)
                //preferLoadBalanceForFactoryInstance(constraintFactory),
                //preferArrangeInSameTimeGroupingMore(constraintFactory),
        };
    }

    private Constraint forbidSameSequence(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachIncludingUnassigned(AbstractPlayerProducingArrangement.class)
                .filter(factoryAction -> factoryAction.getPlanningSequence() != null)
                .join(
                        AbstractPlayerProducingArrangement.class,
                        Joiners.equal(AbstractPlayerProducingArrangement::getPlanningSequence)
                )
                .penalize(HardSoftScore.ofHard(9))
                .asConstraint("forbidSameSequence");
    }

//    private Constraint forbidMismatchFactory(ConstraintFactory constraintFactory) {
//        return constraintFactory.forEach(SchedulingPlayerFactoryAction.class)
//                .filter(SchedulingPlayerFactoryAction::boolFactoryMismatch)
//                .penalize(
//                        HardSoftScore.ofHard(
//                                999
//                        )
//                )
//                .asConstraint("forbidMismatchFactory");
//    }

//    private Constraint forbidNoDateTimeSlot(ConstraintFactory constraintFactory) {
//        return constraintFactory.forEach(SchedulingPlayerFactoryAction.class)
//                .filter(factoryAction -> Objects.isNull(factoryAction.getPlanningDateTimeSlot()))
//                .penalize(HardSoftScore.ofHard(
//                        999
//                ))
//                .asConstraint("forbidNoDateTimeSlot");
//    }

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

    private Constraint forbidFactoryBackwardActionArrangeDateTimeLessThatForward(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(
                        AbstractPlayerProducingArrangement.class,
                        Joiners.filtering((leftAction, rightAction) -> {
                            boolean factoryIsSame = leftAction.getFactory() == rightAction.getFactory();
                            LocalDateTime leftLocalDateTime = leftAction.getPlanningDateTimeSlotStartAsLocalDateTime();
                            LocalDateTime rightLocalDateTime = rightAction.getPlanningDateTimeSlotStartAsLocalDateTime();
                            return factoryIsSame && rightLocalDateTime.isBefore(leftLocalDateTime);
                        })
                )
                .penalize(
                        HardSoftScore.ofHard(5),
                        (leftAction, rightAction) -> Math.abs(
                                leftAction.getPlanningDateTimeSlot().getId() -
                                rightAction.getPlanningDateTimeSlot().getId()
                        )
                )
                .asConstraint("forbidBackwardActionArrangeDateTimeLessThatForward");
    }

    private Constraint forbidCompositeProducingBeforeOrEqualMaterialProducing(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachIncludingUnassigned(
                        AbstractPlayerProducingArrangement.class
                )
                .filter(producingArrangement -> producingArrangement.getPlanningSequence() != null && producingArrangement.getPlanningDateTimeSlot() != null && producingArrangement.getFactory() != null)
                .join(
                        AbstractPlayerProducingArrangement.class,
                        Joiners.lessThanOrEqual(AbstractPlayerProducingArrangement::getPlanningDateTimeSlotStartAsLocalDateTime),
                        Joiners.filtering((leftAction, rightAction) -> {
                            return leftAction.getMaterials().containsKey(rightAction.getSchedulingProduct());
                        })
                )
                .expand((leftAction, rightAction) -> leftAction.getSchedulingWarehouse().toProductAmountMap(leftAction))
                .filter((leftAction, rightAction, warehouseStock) -> {
                    Map<SchedulingProduct, Integer> warehouse = new HashMap<>(warehouseStock);
                    List<ActionConsequence> consequences = rightAction.calcConsequence()
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
                                5
                        ),
                        (leftAction, rightAction, warehouse) -> {
                            LocalDateTime leftWorkCalender = leftAction.getWorkTimeLimit().getEndDateTime();
                            LocalDateTime rightWorkCalender = rightAction.getWorkTimeLimit().getEndDateTime();
                            LocalDateTime leftArrangeDateTime = leftAction.getPlanningDateTimeSlotStartAsLocalDateTime();
                            LocalDateTime rightArrangeDateTime = rightAction.getPlanningDateTimeSlotStartAsLocalDateTime();
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

//    private Constraint forbidArrangeOutOfFactoryProducingCapacity(ConstraintFactory constraintFactory) {
//        return constraintFactory.forEach(SchedulingPlayerFactoryAction.class)
//                .join(
//                        SchedulingFactoryInstance.class,
//                        Joiners.equal(SchedulingPlayerFactoryAction::getPlanningFactory, Function.identity())
//                )
//                .expand((action, factoryInstance) -> factoryInstance.calcRemainProducingQueueSize(
//                        action.getPlanningDateTimeSlot().getStart()))
//                .filter((action, factoryInstance, remainSize) -> {
//                    int tempSize = remainSize;
//                    List<ActionConsequence.SchedulingResourceChange> resourceChanges = action.calcConsequence()
//                            .stream()
//                            .filter(consequence -> consequence.getResource().getRoot() == factoryInstance)
//                            .filter(consequence -> consequence.getResource() instanceof ActionConsequence.FactoryProducingQueue)
//                            .filter(consequence -> consequence.getLocalDateTime().isEqual(action.getPlanningDateTimeSlot().getStart()))
//                            .map(ActionConsequence::getResourceChange)
//                            .toList();
//                    for (ActionConsequence.SchedulingResourceChange resourceChange : resourceChanges) {
//                        if ((tempSize = resourceChange.apply(tempSize)) < 0) {
//                            return true;
//                        }
//                    }
//                    return false;
//                })
//                .groupBy(ConstraintCollectors.toList((factoryInstance, remainSize, action) -> action))
//                .penalize(
//                        HardSoftScore.ofHard(
//                                800
//                        )
//                )
//                .penalize(
//                        BendableScore.ofHard(
//                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
//                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
//                                TownshipSchedulingProblem.HARD_BROKEN_QUEUE,
//                                600
//                        ),
//                        (actions) -> actions.size() * actions.size()
//                )
//                .asConstraint("forbidArrangeOutOfFactoryProducingCapacity");
//    }

    private Constraint forbidArrangeOutOfFactoryProducingCapacity(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachIncludingUnassigned(AbstractPlayerProducingArrangement.class)
                .filter(producingArrangement -> producingArrangement.getFactory() != null)
                .groupBy(AbstractPlayerProducingArrangement::getFactory, ConstraintCollectors.toList())
                .expand(AbstractFactoryInstance::calcRemainProducingQueueSize)
                .filter((factoryInstance, factoryActions, integerIntegerMap) -> integerIntegerMap.values()
                        .stream()
                        .anyMatch(integer -> integer < 0))
                .penalize(
                        HardSoftScore.ofHard(
                                99
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

    private Constraint forbidExceedOrderDueDateTime(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(SchedulingOrder.class)
                .filter(schedulingOrder -> schedulingOrder.optionalDeadline().isPresent())
                .join(
                        AbstractPlayerProducingArrangement.class,
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
                                1
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
                .asConstraint("forbidExceedOrderDueDateTime");
    }

//    private Constraint preferEarlyArrange(ConstraintFactory constraintFactory) {
//        return constraintFactory.forEach(SchedulingPlayerFactoryAction.class)
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
//                .penalize(
//                        HardSoftScore.ofSoft(
//                                300
//                        ), (factoryAction) -> {
//                            LocalDateTime workCalenderStart = factoryAction.getWorkTimeLimit().getStartDateTime();
//                            LocalDateTime arrangeDateTime = factoryAction.getPlanningPlayerArrangeDateTime();
//                            return Math.toIntExact(Duration.between(workCalenderStart, arrangeDateTime).toMinutes());
//                        }
//                )
//                .asConstraint("preferEarlyArrange");
//    }

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

    private Constraint preferArrangeNotInSleepTime(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(AbstractPlayerProducingArrangement.class)
                .map(AbstractPlayerProducingArrangement::getPlanningDateTimeSlotStartAsLocalDateTime)
                .filter(dateTime -> {
                    LocalTime localTime = dateTime.toLocalTime();
                    LocalTime sleepStart = LocalTime.MIDNIGHT.minusHours(1);
                    LocalTime sleepEnd = LocalTime.MIDNIGHT.plusHours(7);
                    return localTime.isAfter(sleepStart) && localTime.isBefore(sleepEnd);
                })
                .groupBy(ConstraintCollectors.count())
                .penalize(
                        HardSoftScore.ofSoft(
                                1
                        ), Math::toIntExact
                )
                .asConstraint("preferArrangeNotInSleepTime");
    }

}
