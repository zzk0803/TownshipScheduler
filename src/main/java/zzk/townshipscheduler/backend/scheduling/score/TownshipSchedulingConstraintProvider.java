package zzk.townshipscheduler.backend.scheduling.score;


import ai.timefold.solver.core.api.score.HardMediumSoftScore;
import ai.timefold.solver.core.api.score.stream.*;
import org.jspecify.annotations.NonNull;
import zzk.townshipscheduler.backend.OrderType;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingOrder;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.function.Function;
import java.util.function.Predicate;

public class TownshipSchedulingConstraintProvider
        implements ConstraintProvider {


    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
        return new Constraint[]{
                forbidBrokenFactoryAbility(constraintFactory),
                forbidBrokenPrerequisiteArrangement(constraintFactory),
                shouldArrangementCompleted(constraintFactory),
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
                .join(
                        SchedulingProducingArrangement.class,
                        Joiners.equal(SchedulingProducingArrangement::getPlanningFactoryInstance)
                )
                .filter(
                        (left, right) -> {
                            LocalDateTime rightArrangeDateTime = right.getArrangeDateTime();
                            LocalDateTime rightCompletedDateTime = right.getCompletedDateTime();
                            LocalDateTime leftArrangeDateTime = left.getArrangeDateTime();
                            boolean b1 = !rightArrangeDateTime.isAfter(leftArrangeDateTime);
                            boolean b2 = rightCompletedDateTime.isAfter(leftArrangeDateTime);
                            return b1 && b2;
                        }
                )
                .groupBy(
                        (current, other) -> current,
                        ConstraintCollectors.countDistinct((current, other) -> other)
                )
                .filter((current, queueSize) ->
                        queueSize > current.getPlanningFactoryInstance()
                                .getProducingLength()
                )
                .penalize(
                        HardMediumSoftScore.ONE_HARD,
                        (current, queueSize) -> queueSize - current.getPlanningFactoryInstance()
                                .getProducingLength()
                )
                .asConstraint("forbidBrokenFactoryAbility");
    }

    private Constraint forbidBrokenPrerequisiteArrangement(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .join(
                        SchedulingProducingArrangement.class,
                        Joiners.equal(
                                Function.identity(),
                                SchedulingProducingArrangement::getSupportProducingArrangement
                        )
                )
                .groupBy(
                        (whole, partial) -> whole,
                        ConstraintCollectors.max(
                                (whole, partial) -> partial,
                                SchedulingProducingArrangement::getCompletedDateTime
                        )
                )
                .filter((whole, partialMax) -> whole.getArrangeDateTime()
                        .isBefore(partialMax.getCompletedDateTime()))
                .penalize(
                        HardMediumSoftScore.ONE_HARD,
                        (whole, partialMax) ->
                                Duration.between(whole.getArrangeDateTime(), partialMax.getCompletedDateTime())
                                        .toMinutes()
                )
                .asConstraint("forbidBrokenPrerequisiteArrangement");
    }

    private Constraint shouldArrangementCompleted(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .filter(Predicate.not(SchedulingProducingArrangement::boolCompleted))
                .penalize(
                        HardMediumSoftScore.ofMedium(50L),
                        SchedulingProducingArrangement::getEvaluateFactor
                )
                .asConstraint("shouldArrangementCompleted");
    }

    private Constraint shouldNotBrokenDeadlineOrder(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .filter(SchedulingProducingArrangement::boolOrderDirect)
                .ifExists(
                        constraintFactory.forEach(SchedulingOrder.class)
                                .filter(SchedulingOrder::boolHasDeadline),
                        Joiners.equal(SchedulingProducingArrangement::getSchedulingOrder, Function.identity()),
                        Joiners.filtering((producingArrangement, schedulingOrder) -> {
                            LocalDateTime deadline = schedulingOrder.getDeadline();
                            LocalDateTime completedDateTime = producingArrangement.getCompletedDateTime();
                            return completedDateTime == null || completedDateTime.isAfter(deadline);
                        })
                )
                .penalize(
                        HardMediumSoftScore.ONE_MEDIUM,
                        (schedulingProducingArrangement) -> schedulingProducingArrangement.calcDeadlineToCompletedDuration()
                                .toMinutes()
                )
                .asConstraint("shouldNotBrokenDeadlineOrder");
    }

    private Constraint shouldNotBrokenCalendarEnd(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .filter(SchedulingProducingArrangement::boolOrderDirect)
                .filter(SchedulingProducingArrangement::boolCompletedAfterCalendarEnd)
                .penalize(
                        HardMediumSoftScore.ofMedium(100L),
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
                        HardMediumSoftScore.ofSoft(10000L)
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
