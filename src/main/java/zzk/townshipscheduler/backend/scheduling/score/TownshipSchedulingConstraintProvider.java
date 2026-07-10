package zzk.townshipscheduler.backend.scheduling.score;


import ai.timefold.solver.core.api.score.HardMediumSoftScore;
import ai.timefold.solver.core.api.score.stream.*;
import ai.timefold.solver.core.api.score.stream.common.ConnectedRangeChain;
import org.jspecify.annotations.NonNull;
import zzk.townshipscheduler.backend.OrderType;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingOrder;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;

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
                preferArrangeDateTimeAsSoonAsPassible(constraintFactory),
                preferMinimizeProductArrangeDateTimeSlotUsage(constraintFactory)
        };
    }

    private Constraint forbidBrokenFactoryAbility(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .groupBy(
                        SchedulingProducingArrangement::getPlanningFactoryInstance,
                        SchedulingProducingArrangement::getPlanningDateTimeSlot,
                        ConstraintCollectors.toConnectedTemporalRanges(
                                SchedulingProducingArrangement::getArrangeDateTime,
                                SchedulingProducingArrangement::getCompletedDateTime
                        )
                )
                .flattenLast(ConnectedRangeChain::getConnectedRanges)
                .filter((factoryInstance, dateTimeSlot, connectedRange) -> connectedRange.getContainedRangeCount() > factoryInstance.getProducingLength())
                .penalize(
                        HardMediumSoftScore.ONE_HARD,
                        (factoryInstance, dateTimeSlot, connectedRange) -> connectedRange.getContainedRangeCount() - factoryInstance.getProducingLength()
                )
                .asConstraint("forbidBrokenFactoryAbility");
    }

    private Constraint forbidBrokenPrerequisiteArrangement(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.precompute(
                        precomputeFactory -> precomputeFactory.forEachUnfiltered(SchedulingProducingArrangement.class)
                                .join(
                                        precomputeFactory.forEachUnfiltered(SchedulingProducingArrangement.class),
                                        Joiners.containing(
                                                SchedulingProducingArrangement::getDeepPrerequisiteProducingArrangements,
                                                Function.identity()
                                        )
                                )
                )
                .filter((whole, partial) -> whole.boolCompleted() && partial.boolCompleted())
                .groupBy(
                        (whole, partial) -> whole,
                        ConstraintCollectors.max(
                                (whole, partial) -> partial,
                                SchedulingProducingArrangement::getCompletedDateTime
                        )
                )
                .filter((whole, partialMax) -> whole.getArrangeDateTime()
                        .isBefore(partialMax.getCompletedDateTime())
                )
                .penalize(
                        HardMediumSoftScore.ONE_HARD,
                        (whole, partialMax) ->
                                Duration.between(whole.getArrangeDateTime(), partialMax.getCompletedDateTime())
                                        .toMinutes()
                )
                .asConstraint("forbidBrokenPrerequisiteArrangement");
    }

    private Constraint mustArrangementCompleted(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .filter(Predicate.not(SchedulingProducingArrangement::boolCompleted))
                .penalize(
                        HardMediumSoftScore.ONE_HARD,
                        schedulingProducingArrangement -> schedulingProducingArrangement.getEvaluateFactor() * schedulingProducingArrangement.getWorkCalendarSpan().toMinutes()
                )
                .asConstraint("shouldArrangementCompleted");
    }

    private Constraint shouldNotBrokenDeadlineOrder(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.precompute(
                        precomputeFactory -> precomputeFactory.forEachUnfiltered(
                                        SchedulingProducingArrangement.class)
                                .filter(SchedulingProducingArrangement::boolOrderDirect)
                                .ifExists(
                                        precomputeFactory.forEachUnfiltered(SchedulingOrder.class)
                                                .filter(SchedulingOrder::boolHasDeadline),
                                        Joiners.equal(SchedulingProducingArrangement::getSchedulingOrder, Function.identity())
                                )
                )
                .filter(SchedulingProducingArrangement::boolCompleted)
                .filter(SchedulingProducingArrangement::boolCompletedAfterDeadline)
                .penalize(
                        HardMediumSoftScore.ONE_MEDIUM,
                        (schedulingProducingArrangement)
                                -> schedulingProducingArrangement.calcDeadlineToCompletedDuration()
                                           .toMinutes() * calcFactor(schedulingProducingArrangement)
                )
                .asConstraint("shouldNotBrokenDeadlineOrder");
    }

    private Constraint shouldNotBrokenCalendarEnd(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .filter(SchedulingProducingArrangement::boolOrderDirect)
                .filter(SchedulingProducingArrangement::boolCompleted)
                .filter(SchedulingProducingArrangement::boolCompletedAfterCalendarEnd)
                .penalize(
                        HardMediumSoftScore.ONE_MEDIUM,
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
                        HardMediumSoftScore.ofSoft(10),
                        schedulingProducingArrangement -> schedulingProducingArrangement.calcSleepArrangeDateTimeToNextAvailableDuration()
                                .toMinutes()
                )
                .asConstraint("preferNotArrangeInPlayerSleepTime");
    }

    private Constraint preferMinimizeCompletedDateTime(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .filter(SchedulingProducingArrangement::boolCompleted)
                .penalize(
                        HardMediumSoftScore.ONE_SOFT,
                        (arrangement) -> calcFactor(arrangement) * arrangement.calcCalendarStartToCompletedDuration()
                                .toMinutes()
                )
                .asConstraint("preferMinimizeCompletedDateTime");
    }

    private Constraint preferArrangeDateTimeAsSoonAsPassible(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .penalize(
                        HardMediumSoftScore.ONE_SOFT,
                        (arrangement) -> arrangement.calcCalendarStartToArrangedDuration().toMinutes() *
                                         calcFactor(arrangement)
                )
                .asConstraint("preferArrangeDateTimeAsSoonAsPassible");
    }

    private Constraint preferMinimizeProductArrangeDateTimeSlotUsage(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .groupBy(
                        SchedulingProducingArrangement::getPlanningFactoryInstance,
                        ConstraintCollectors.countDistinct(SchedulingProducingArrangement::getPlanningDateTimeSlot)
                )
                .penalize(
                        HardMediumSoftScore.ofSoft(5000L), (factoryInstance, slotAmount) -> slotAmount - 1
                )
                .asConstraint("preferMinimizeProductArrangeDateTimeSlotUsage");
    }

    public int calcFactor(SchedulingProducingArrangement arrangement) {
        int factor = arrangement.getDeadline() != null ? 100 : 1;
        if (arrangement.getSchedulingOrder() != null) {
            OrderType orderType = arrangement.getSchedulingOrder()
                    .getOrderType();
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

}
