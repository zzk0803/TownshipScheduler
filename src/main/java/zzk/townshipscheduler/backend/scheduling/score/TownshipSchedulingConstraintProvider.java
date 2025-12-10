package zzk.townshipscheduler.backend.scheduling.score;


import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import ai.timefold.solver.core.api.score.stream.*;
import org.jspecify.annotations.NonNull;
import zzk.townshipscheduler.backend.OrderType;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInstanceDateTimeSlot;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingOrder;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import java.util.function.Function;

public class TownshipSchedulingConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
        return new Constraint[]{
                forbidBrokenFactoryAbility(constraintFactory),
                forbidBrokenPrerequisiteStock(constraintFactory),
                forbidBrokenDeadlineOrder(constraintFactory),
                shouldNotBrokenCalendarEnd(constraintFactory),
                shouldNotArrangeInPlayerSleepTime(constraintFactory),
                preferMinimizeOrderCompletedDateTime(constraintFactory),
                preferArrangeDateTimeAsSoonAsPassible(constraintFactory),
                preferMinimizeProductArrangeDateTimeSlotUsage(constraintFactory)
        };
    }

    private Constraint forbidBrokenFactoryAbility(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .join(
                        SchedulingProducingArrangement.class,
                        Joiners.equal(SchedulingProducingArrangement::getPlanningFactoryDateTimeSlot)
                )
                .filter(
                        (current, other) -> {
                            boolean b1 = !other.getArrangeDateTime().isAfter(current.getArrangeDateTime());
                            boolean b2 = other.getCompletedDateTime().isAfter(current.getArrangeDateTime());
                            return b1 && b2;
                        }
                )
                .groupBy(
                        (current, other) -> current,
                        ConstraintCollectors.countDistinct((current, other) -> other)
                )
                .filter((current, queueSize) ->
                        queueSize > current.getPlanningFactoryDateTimeSlot().getProducingLength()
                )
                .penalizeLong(
                        HardMediumSoftLongScore.ONE_HARD,
                        (current, queueSize) -> queueSize - current.getPlanningFactoryDateTimeSlot()
                                .getProducingLength()
                )
                .asConstraint("forbidBrokenFactoryAbility");
    }

    private Constraint forbidBrokenPrerequisiteStock(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .join(
                        SchedulingProducingArrangement.class,
                        Joiners.filtering(SchedulingProducingArrangement::haveDeepPrerequisiteArrangement)
                )
                .join(
                        SchedulingFactoryInstanceDateTimeSlot.class,
                        Joiners.equal(
                                (leftArrangement, rightArrangement) -> leftArrangement.getPlanningFactoryDateTimeSlot(),
                                Function.identity()
                        )
                )
                .join(
                        SchedulingFactoryInstanceDateTimeSlot.class,
                        Joiners.equal(
                                (leftArrangement, rightArrangement, leftArrangementSlot) -> rightArrangement.getPlanningFactoryDateTimeSlot(),
                                Function.identity()
                        )
                )
                .groupBy(
                        (leftArrangement, rightArrangement, leftArrangementSlot, rightArrangementSlot) -> leftArrangement,
                        ConstraintCollectors.max(
                                (leftArrangement, rightArrangement, leftArrangementSlot, rightArrangementSlot) -> rightArrangement,
                                SchedulingProducingArrangement::getCompletedDateTime
                        )
                )
                .filter((whole, partialMax) -> whole.getArrangeDateTime().isBefore(partialMax.getCompletedDateTime()))
                .penalizeLong(
                        HardMediumSoftLongScore.ONE_HARD,
                        (whole, partialMax) ->
                                Duration.between(whole.getArrangeDateTime(), partialMax.getCompletedDateTime()).toMinutes()
                )
                .asConstraint("forbidBrokenPrerequisiteStock")
                ;
    }

    private Constraint forbidBrokenDeadlineOrder(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingOrder.class)
                .filter(SchedulingOrder::boolHasDeadline)
                .join(
                        constraintFactory.forEach(SchedulingProducingArrangement.class)
                                .filter(SchedulingProducingArrangement::isOrderDirect),
                        Joiners.equal(
                                Function.identity(),
                                SchedulingProducingArrangement::getSchedulingOrder
                        ),
                        Joiners.filtering((schedulingOrder, producingArrangement) -> {
                                    LocalDateTime deadline = schedulingOrder.getDeadline();
                                    LocalDateTime completedDateTime = producingArrangement.getCompletedDateTime();
                                    return completedDateTime == null || completedDateTime.isAfter(deadline);
                                }
                        )
                )
                .penalizeLong(
                        HardMediumSoftLongScore.ONE_MEDIUM,
                        (
                                (schedulingOrder, producingArrangement) -> {
                                    LocalDateTime deadline = schedulingOrder.getDeadline();
                                    LocalDateTime completedDateTime = producingArrangement.getCompletedDateTime();
                                    return completedDateTime != null
                                            ? Duration.between(deadline, completedDateTime).toMinutes()
                                            : Duration.between(
                                                    producingArrangement.getSchedulingWorkCalendar().getStartDateTime(),
                                                    producingArrangement.getSchedulingWorkCalendar().getEndDateTime()
                                            ).toMinutes();
                                }
                        )
                )
                .asConstraint("shouldNotBrokenDeadlineOrder");
    }

    private Constraint shouldNotBrokenCalendarEnd(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .filter(schedulingProducingArrangement -> {
                            LocalDateTime completedDateTime = schedulingProducingArrangement.getCompletedDateTime();
                            return schedulingProducingArrangement.isOrderDirect()
                                    && (
                                    Objects.isNull(completedDateTime)
                                            || completedDateTime.isAfter(
                                            schedulingProducingArrangement.getSchedulingWorkCalendar()
                                                    .getEndDateTime()
                                    )
                            );
                        }
                )
                .penalizeLong(
                        HardMediumSoftLongScore.ofMedium(100L),
                        schedulingProducingArrangement -> {
                            LocalDateTime completedDateTime = schedulingProducingArrangement.getCompletedDateTime();
                            LocalDateTime workCalendarStart = schedulingProducingArrangement.getSchedulingWorkCalendar()
                                    .getStartDateTime();
                            LocalDateTime workCalendarEnd = schedulingProducingArrangement.getSchedulingWorkCalendar()
                                    .getEndDateTime();
                            if (completedDateTime != null) {
                                return Duration.between(workCalendarEnd, completedDateTime).toMinutes();
                            } else {
                                return Duration.between(workCalendarStart, workCalendarEnd).toMinutes();
                            }
                        }
                )
                .asConstraint("shouldNotBrokenCalendarEnd");
    }

    private Constraint shouldNotArrangeInPlayerSleepTime(
            @NonNull ConstraintFactory constraintFactory
    ) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .filter(schedulingProducingArrangement -> {
                    LocalDateTime arrangeDateTime = schedulingProducingArrangement.getArrangeDateTime();
                    LocalTime sleepStart = schedulingProducingArrangement.getSchedulingPlayer().getSleepStart();
                    LocalTime sleepEnd = schedulingProducingArrangement.getSchedulingPlayer().getSleepEnd();
                    LocalTime arrangeTime = arrangeDateTime.toLocalTime();
                    return (arrangeTime.isAfter(sleepStart) && arrangeTime.isBefore(LocalTime.MAX))
                            || (arrangeTime.isAfter(LocalTime.MIN) && arrangeTime.isBefore(sleepEnd));
                })
                .penalizeLong(
                        HardMediumSoftLongScore.ofMedium(500L)
                )
                .asConstraint("shouldNotArrangeInPlayerSleepTime");
    }

    private Constraint preferMinimizeOrderCompletedDateTime(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .filter(SchedulingProducingArrangement::isOrderDirect)
                .groupBy(
                        SchedulingProducingArrangement::getSchedulingOrder,
                        ConstraintCollectors.max(
                                Function.identity(),
                                SchedulingProducingArrangement::getCompletedDateTime
                        )
                )
                .penalizeLong(
                        HardMediumSoftLongScore.ONE_SOFT,
                        (order, arrangement) -> {
                            var calendarStartDateTime = arrangement.getSchedulingWorkCalendar().getStartDateTime();
                            var completedDateTime = arrangement.getCompletedDateTime();
                            Duration between = Duration.between(calendarStartDateTime, completedDateTime);
                            return calcFactor(arrangement) * between.toMinutes();
                        }
                )
                .asConstraint("preferMinimizeOrderCompletedDateTime");
    }

    private int calcFactor(SchedulingProducingArrangement arrangement) {
        int factor = 1;
        if (arrangement.getSchedulingOrder() != null) {
            OrderType orderType = arrangement.getSchedulingOrder().getOrderType();
            switch (orderType) {
                case TRAIN -> {
                    factor = 10;
                }
                case AIRPLANE -> {
                    factor = 100;
                }
            }
        }
        return factor;
    }

    private Constraint preferArrangeDateTimeAsSoonAsPassible(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .penalizeLong(
                        HardMediumSoftLongScore.ONE_SOFT, (arrangement) -> {
                            return Duration.between(
                                    arrangement.getSchedulingWorkCalendar().getStartDateTime(),
                                    arrangement.getArrangeDateTime()
                            ).toMinutes() * calcFactor(arrangement);
                        }
                )
                .asConstraint("preferArrangeDateTimeAsSoonAsPassible");
    }

    private Constraint preferMinimizeProductArrangeDateTimeSlotUsage(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .groupBy(
                        SchedulingProducingArrangement::getPlanningFactoryDateTimeSlot,
                        ConstraintCollectors.countDistinct(SchedulingProducingArrangement::getSchedulingDateTimeSlot)
                )
                .penalizeLong(
                        HardMediumSoftLongScore.ofSoft(1000L), (factoryInstance, slotAmount) -> slotAmount - 1
                )
                .asConstraint("preferMinimizeProductArrangeDateTimeSlotUsage");
    }

}
