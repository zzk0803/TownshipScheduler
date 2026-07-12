package zzk.townshipscheduler.backend.scheduling.score;


import ai.timefold.solver.core.api.score.HardMediumSoftBigDecimalScore;
import ai.timefold.solver.core.api.score.stream.*;
import ai.timefold.solver.core.api.score.stream.common.ConnectedRangeChain;
import ai.timefold.solver.core.api.score.stream.common.LoadBalance;
import org.jspecify.annotations.NonNull;
import zzk.townshipscheduler.backend.OrderType;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInstance;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingOrder;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.function.Function;
import java.util.function.Predicate;

public class TownshipSchedulingConstraintProvider
        implements ConstraintProvider {


    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
        return new Constraint[]{
                forbidBrokenFactoryAbility(constraintFactory),
                forbidBrokenPrerequisiteArrangement(constraintFactory),
                mustArrangementCompleted(constraintFactory),
                shouldNotBrokenDeadlineOrder(constraintFactory),
                shouldNotBrokenCalendarEnd(constraintFactory),
                preferNotArrangeInPlayerSleepTime(constraintFactory),
                preferMinimizeCompletedDateTime(constraintFactory),
                preferMinimizeArrangeDateTimeToPrerequisiteDone(constraintFactory),
                //preferArrangeDateTimeAsSoonAsPassible(constraintFactory),
                preferMinimizeProductArrangeDateTimeSlotUsage(constraintFactory),
                preferLoadBalanceArrangementsInFactoryInstance(constraintFactory)
        };
    }

    private Constraint forbidBrokenFactoryAbility(ConstraintFactory constraintFactory) {
//        return constraintFactory.forEach(SchedulingFactoryInstance.class)
//                .join(SchedulingProducingArrangement.class, Joiners.equal(Function.identity(), SchedulingProducingArrangement::getPlanningFactoryInstance))
//                .join(
//                        SchedulingProducingArrangement.class,
//                        Joiners.equal((factory, formerArrange) -> factory, SchedulingProducingArrangement::getPlanningFactoryInstance)
//                )
//                .groupBy(
//                        (factory, left, right) -> factory,
//                        (factory, left, right) -> left,
//                        ConstraintCollectors.conditionally(
//                                (factory, left, right) -> {
//                                    if (factory != left.getPlanningFactoryInstance() && factory != right.getPlanningFactoryInstance() && left.getPlanningFactoryInstance() != right.getPlanningFactoryInstance()) {
//                                        return false;
//                                    }
//                                    LocalDateTime rightArrangeDateTime = right.getArrangeDateTime();
//                                    LocalDateTime rightCompletedDateTime = right.getCompletedDateTime();
//                                    LocalDateTime leftArrangeDateTime = left.getArrangeDateTime();
//                                    boolean b1 = !rightArrangeDateTime.isAfter(leftArrangeDateTime);
//                                    boolean b2 = rightCompletedDateTime.isAfter(leftArrangeDateTime);
//                                    return b1 && b2;
//                                },
//                                ConstraintCollectors.countDistinct((factory, left, right) -> right)
//                        )
//                )
//                .filter((factory, current, queueSize) -> queueSize > factory.getProducingLength())
//                .penalize(
//                        HardMediumSoftBigDecimalScore.ONE_HARD,
//                        (factory, current, queueSize) -> queueSize - factory.getProducingLength()
//                )
//                .asConstraint("forbidBrokenFactoryAbility");

//        return constraintFactory.forEach(SchedulingProducingArrangement.class)
//                .filter(SchedulingProducingArrangement::boolPlanningAssigned)
//                .join(
//                        constraintFactory.forEach(SchedulingProducingArrangement.class)
//                                .filter(SchedulingProducingArrangement::boolPlanningAssigned),
//                        Joiners.equal(SchedulingProducingArrangement::getPlanningFactoryInstance)
//                )
//                .groupBy(
//                        (left, right) -> left, ConstraintCollectors.conditionally(
//                                (left, right) -> {
//                                    LocalDateTime rightArrangeDateTime = right.getArrangeDateTime();
//                                    LocalDateTime rightCompletedDateTime = right.getCompletedDateTime();
//                                    LocalDateTime leftArrangeDateTime = left.getArrangeDateTime();
//                                    boolean b1 = !rightArrangeDateTime.isAfter(leftArrangeDateTime);
//                                    boolean b2 = rightCompletedDateTime.isAfter(leftArrangeDateTime);
//                                    return b1 && b2;
//                                }, ConstraintCollectors.countDistinct((left, right) -> right)
//                        )
//                )
//                .filter((current, queueSize) -> queueSize > current.getPlanningFactoryInstance().getProducingLength())
//                .penalize(
//                        HardMediumSoftBigDecimalScore.ONE_HARD,
//                        (current, queueSize) -> queueSize - current.getPlanningFactoryInstance().getProducingLength()
//                )
//                .asConstraint("forbidBrokenFactoryAbility");

        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .groupBy(
                        SchedulingProducingArrangement::getPlanningFactoryInstance,
                        ConstraintCollectors.toConnectedTemporalRanges(
                                SchedulingProducingArrangement::getArrangeDateTime,
                                SchedulingProducingArrangement::getCompletedDateTime
                        )
                )
                .flattenLast(ConnectedRangeChain::getConnectedRanges)
                .filter((factoryInstance, connectedRange) -> connectedRange.getContainedRangeCount() > factoryInstance.getProducingLength())
                .penalize(
                        HardMediumSoftBigDecimalScore.ONE_HARD,
                        (factoryInstance, connectedRange) -> connectedRange.getContainedRangeCount() - factoryInstance.getProducingLength()
                )
                .asConstraint("forbidBrokenFactoryAbility");
    }

    private Constraint forbidBrokenPrerequisiteArrangement(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.precompute(precomputeFactory -> precomputeFactory.forEachUnfiltered(
                        SchedulingProducingArrangement.class).join(
                        precomputeFactory.forEachUnfiltered(SchedulingProducingArrangement.class),
                        Joiners.containing(
                                SchedulingProducingArrangement::getPrerequisiteProducingArrangements,
                                Function.identity()
                        )
                ))
                .filter((whole, partial) -> whole.boolCompleted() && partial.boolCompleted())
                .groupBy(
                        (whole, partial) -> whole,
                        ConstraintCollectors.max(
                                (whole, partial) -> partial,
                                SchedulingProducingArrangement::getCompletedDateTime
                        )
                )
                .filter((whole, partialMax) -> whole.getArrangeDateTime().isBefore(partialMax.getCompletedDateTime()))
                .penalize(
                        HardMediumSoftBigDecimalScore.ONE_HARD,
                        (whole, partialMax) -> Duration.between(
                                whole.getArrangeDateTime(),
                                partialMax.getCompletedDateTime()
                        ).toMinutes()
                )
                .asConstraint("forbidBrokenPrerequisiteArrangement");
    }

    private Constraint mustArrangementCompleted(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .filter(Predicate.not(SchedulingProducingArrangement::boolCompleted))
                .penalize(
                        HardMediumSoftBigDecimalScore.ONE_HARD,
                        schedulingProducingArrangement -> schedulingProducingArrangement.getEvaluateFactor() * schedulingProducingArrangement.getWorkCalendarSpan()
                                .toMinutes()
                )
                .asConstraint("shouldArrangementCompleted");
    }

    private Constraint shouldNotBrokenDeadlineOrder(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.precompute(precomputeFactory -> precomputeFactory.forEachUnfiltered(
                                SchedulingProducingArrangement.class)
                        .filter(SchedulingProducingArrangement::boolOrderDirect)
                        .ifExists(
                                precomputeFactory.forEachUnfiltered(SchedulingOrder.class)
                                        .filter(SchedulingOrder::boolHasDeadline),
                                Joiners.equal(SchedulingProducingArrangement::getSchedulingOrder, Function.identity())
                        ))
                .filter(SchedulingProducingArrangement::boolCompleted)
                .filter(SchedulingProducingArrangement::boolCompletedAfterDeadline)
                .penalize(
                        HardMediumSoftBigDecimalScore.ONE_MEDIUM,
                        (schedulingProducingArrangement) -> schedulingProducingArrangement.calcDeadlineToCompletedDuration()
                                                                    .toMinutes() * schedulingProducingArrangement.getEvaluateFactor()
                )
                .asConstraint("shouldNotBrokenDeadlineOrder");
    }

    private Constraint shouldNotBrokenCalendarEnd(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .filter(SchedulingProducingArrangement::boolOrderDirect)
                .filter(SchedulingProducingArrangement::boolCompleted)
                .filter(SchedulingProducingArrangement::boolCompletedAfterCalendarEnd)
                .penalize(
                        HardMediumSoftBigDecimalScore.ONE_MEDIUM,
                        (schedulingProducingArrangement) -> schedulingProducingArrangement.calcCalendarEndToCompletedDuration()
                                .toMinutes()
                )
                .asConstraint("shouldNotBrokenCalendarEnd");
    }

    private Constraint preferNotArrangeInPlayerSleepTime(
            @NonNull ConstraintFactory constraintFactory
    ) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .filter(SchedulingProducingArrangement::boolArrangeDateTimeInPlayerSleepTime)
                .penalize(
                        HardMediumSoftBigDecimalScore.ofSoft(BigDecimal.valueOf(50)),
                        schedulingProducingArrangement -> schedulingProducingArrangement.calcSleepArrangeDateTimeToNextAvailableDuration()
                                .toMinutes()
                )
                .asConstraint("preferNotArrangeInPlayerSleepTime");
    }

    private Constraint preferMinimizeCompletedDateTime(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .filter(SchedulingProducingArrangement::boolOrderDirect)
                .filter(SchedulingProducingArrangement::boolCompleted)
                .penalize(
                        HardMediumSoftBigDecimalScore.ONE_SOFT,
                        (arrangement) -> arrangement.getEvaluateFactor() * arrangement.calcCalendarStartToCompletedDuration()
                                .toMinutes()
                )
                .asConstraint("preferMinimizeCompletedDateTime");
    }

    private Constraint preferMinimizeArrangeDateTimeToPrerequisiteDone(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .filter(SchedulingProducingArrangement::boolPlanningAssigned)
                .penalize(
                        HardMediumSoftBigDecimalScore.ONE_SOFT,
                        arrangement -> arrangement.calcArrangeDateTimeToPrerequisiteDuration().abs().toMinutes()
                )
                .asConstraint("preferMinimizeArrangeDateTimeToPrerequisiteDone");
    }

    private Constraint preferLoadBalanceArrangementsInFactoryInstance(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .groupBy(
                        SchedulingProducingArrangement::getPlanningFactoryInstance,
                        ConstraintCollectors.count()
                )
                .complement(SchedulingFactoryInstance.class, factoryInstance -> 0L)
                .groupBy(ConstraintCollectors.loadBalance(
                        (factoryInstance, arrangementCount) -> factoryInstance,
                        (factoryInstance, arrangementCount) -> arrangementCount
                ))
                .penalizeBigDecimal(
                        HardMediumSoftBigDecimalScore.ofSoft(
                                BigDecimal.valueOf(10000)
                        ),
                        LoadBalance::unfairness
                )
                .asConstraint("preferLoadBalanceArrangementsInFactoryInstance");
    }

    private Constraint preferMinimizeProductArrangeDateTimeSlotUsage(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .groupBy(
                        SchedulingProducingArrangement::getPlanningFactoryInstance,
                        ConstraintCollectors.countDistinct(SchedulingProducingArrangement::getPlanningDateTimeSlot)
                )
                .penalize(
                        HardMediumSoftBigDecimalScore.ofSoft(BigDecimal.valueOf(5000)),
                        (factoryInstance, slotAmount) -> slotAmount - 1
                )
                .asConstraint("preferMinimizeProductArrangeDateTimeSlotUsage");
    }

    public int calcFactor(SchedulingProducingArrangement arrangement) {
        int factor = arrangement.getDeadline() != null
                ? 100
                : 1;
        if (arrangement.getSchedulingOrder() != null) {
            OrderType orderType = arrangement.getSchedulingOrder().getOrderType();
            switch (orderType) {
                case TRAIN -> {
                    factor *= 10;
                }
                case AIRPLANE -> {
                    factor *= 100;
                }
            }
        }
        return factor;
    }

    private Constraint preferArrangeDateTimeAsSoonAsPassible(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .penalize(
                        HardMediumSoftBigDecimalScore.ONE_SOFT,
                        (arrangement) -> arrangement.calcCalendarStartToArrangedDuration().toMinutes() * arrangement.getEvaluateFactor()
                )
                .asConstraint("preferArrangeDateTimeAsSoonAsPassible");
    }

}
