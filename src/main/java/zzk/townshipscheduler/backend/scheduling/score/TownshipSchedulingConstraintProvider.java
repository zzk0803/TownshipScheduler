package zzk.townshipscheduler.backend.scheduling.score;


import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import ai.timefold.solver.core.api.score.stream.*;
import org.jspecify.annotations.NonNull;
import zzk.townshipscheduler.backend.OrderType;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInstanceDateTimeSlot;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingOrder;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingWorkCalendar;

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
                forbidBrokenPrerequisiteArrangement(constraintFactory),
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
                        Joiners.equal(SchedulingProducingArrangement::getSchedulingFactoryInstance)
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
                        queueSize > current.getSchedulingFactoryInstance()
                                .getProducingLength()
                )
                .penalizeLong(
                        HardMediumSoftLongScore.ONE_HARD,
                        (current, queueSize) -> queueSize - current.getSchedulingFactoryInstance()
                                .getProducingLength()
                )
                .asConstraint("forbidBrokenFactoryAbility");
    }

    private Constraint forbidBrokenPrerequisiteArrangement(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .join(
                        SchedulingFactoryInstanceDateTimeSlot.class,
                        Joiners.equal(
                                SchedulingProducingArrangement::getPlanningFactoryDateTimeSlot,
                                Function.identity()
                        )
                )
                .filter((arrangement, factoryInstanceDateTimeSlot) -> {
                            LocalDateTime arrangeDateTime = arrangement.getArrangeDateTime();
                            LocalDateTime prerequisiteProducingArrangementsCompletedDateTime =
                                    arrangement.getPrerequisiteProducingArrangementsCompletedDateTime();
                            return prerequisiteProducingArrangementsCompletedDateTime == null
                                    || arrangeDateTime.isBefore(prerequisiteProducingArrangementsCompletedDateTime);
                        }
                )
                .penalizeLong(
                        HardMediumSoftLongScore.ofHard(1000)
                )
                .asConstraint("forbidBrokenPrerequisiteArrangement");
    }

    private Constraint shouldNotBrokenDeadlineOrder(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingOrder.class)
                .filter(SchedulingOrder::boolHasDeadline)
                .join(
                        constraintFactory.forEach(SchedulingProducingArrangement.class)
                                .filter(SchedulingProducingArrangement::isOrderDirect),
                        Joiners.equal(
                                Function.identity(),
                                SchedulingProducingArrangement::getSchedulingOrder
                        )
                )
                .filter((schedulingOrder, producingArrangement) -> {
                    LocalDateTime deadline = schedulingOrder.getDeadline();
                    LocalDateTime completedDateTime = producingArrangement.getCompletedDateTime();
                    return completedDateTime == null || completedDateTime.isAfter(deadline);
                })
                .penalizeLong(
                        HardMediumSoftLongScore.ONE_MEDIUM,
                        (
                                (schedulingOrder, producingArrangement) -> {
                                    LocalDateTime deadline = schedulingOrder.getDeadline();
                                    LocalDateTime completedDateTime = producingArrangement.getCompletedDateTime();
                                    return completedDateTime != null
                                            ? Duration.between(deadline, completedDateTime)
                                            .toMinutes()
                                            : Duration.between(
                                                            producingArrangement.getSchedulingWorkCalendar()
                                                                    .getStartDateTime(),
                                                            producingArrangement.getSchedulingWorkCalendar()
                                                                    .getEndDateTime()
                                                    )
                                                    .toMinutes();
                                }
                        )
                )
                .asConstraint("shouldNotBrokenDeadlineOrder");
    }

    private Constraint shouldNotBrokenCalendarEnd(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .filter(SchedulingProducingArrangement::isOrderDirect)
                .filter(schedulingProducingArrangement -> {
                            LocalDateTime completedDateTime = schedulingProducingArrangement.getCompletedDateTime();
                            return Objects.isNull(completedDateTime)
                                    || completedDateTime.isAfter(
                                    schedulingProducingArrangement.getSchedulingWorkCalendar()
                                            .getEndDateTime()
                            );
                        }
                )
                .map(schedulingProducingArrangement -> {
                            LocalDateTime completedDateTime = schedulingProducingArrangement.getCompletedDateTime();
                            LocalDateTime workCalendarStart = schedulingProducingArrangement.getSchedulingWorkCalendar()
                                    .getStartDateTime();
                            LocalDateTime workCalendarEnd = schedulingProducingArrangement.getSchedulingWorkCalendar()
                                    .getEndDateTime();
                            if (completedDateTime != null) {
                                return Duration.between(workCalendarEnd, completedDateTime);
                            } else {
                                return Duration.between(workCalendarStart, workCalendarEnd);
                            }
                        }
                )
                .penalizeLong(
                        HardMediumSoftLongScore.ofMedium(100L),
                        Duration::toMinutes
                )
                .asConstraint("shouldNotBrokenCalendarEnd");
    }

    private Constraint preferNotArrangeInPlayerSleepTime(
            @NonNull ConstraintFactory constraintFactory
    ) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .filter(schedulingProducingArrangement -> {
                    LocalDateTime arrangeDateTime = schedulingProducingArrangement.getArrangeDateTime();
                    LocalTime sleepStart = schedulingProducingArrangement.getSchedulingPlayer()
                            .getSleepStart();
                    LocalTime sleepEnd = schedulingProducingArrangement.getSchedulingPlayer()
                            .getSleepEnd();
                    LocalTime arrangeTime = arrangeDateTime.toLocalTime();
                    return (arrangeTime.isAfter(sleepStart) && arrangeTime.isBefore(LocalTime.MAX))
                            || (arrangeTime.isAfter(LocalTime.MIN) && arrangeTime.isBefore(sleepEnd));
                })
                .penalizeLong(
                        HardMediumSoftLongScore.ofSoft(10000L)
                )
                .asConstraint("preferNotArrangeInPlayerSleepTime");
    }

    private Constraint preferMinimizeCompletedDateTime(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .join(
                        SchedulingFactoryInstanceDateTimeSlot.class,
                        Joiners.equal(
                                SchedulingProducingArrangement::getPlanningFactoryDateTimeSlot,
                                Function.identity()
                        )
                )
                .join(
                        SchedulingWorkCalendar.class,
                        Joiners.equal(
                                (arrangement, factoryDateTimeSlot) -> arrangement.getSchedulingWorkCalendar(),
                                Function.identity()
                        )
                )
                .penalizeLong(
                        HardMediumSoftLongScore.ONE_SOFT,
                        (arrangement, factoryDateTimeSlot, workCalendar) -> {
                            var calendarStartDateTime = workCalendar.getStartDateTime();
                            var completedDateTime = arrangement.getCompletedDateTime();
                            Duration between = Duration.between(calendarStartDateTime, completedDateTime);
                            return calcFactor(arrangement) * between.toMinutes();
                        }
                )
                .asConstraint("preferMinimizeCompletedDateTime");
//        return constraintFactory.forEach(SchedulingProducingArrangement.class)
//                .penalizeLong(
//                        HardMediumSoftLongScore.ONE_SOFT,
//                        (arrangement) -> {
//                            var calendarStartDateTime = arrangement.getSchedulingWorkCalendar()
//                                    .getStartDateTime();
//                            var completedDateTime = arrangement.getCompletedDateTime();
//                            Duration between = Duration.between(calendarStartDateTime, completedDateTime);
//                            return calcFactor(arrangement) * between.toMinutes();
//                        }
//                )
//                .asConstraint("preferMinimizeCompletedDateTime");
    }

    private int calcFactor(SchedulingProducingArrangement arrangement) {
        int factor = 1;
        if (arrangement.getSchedulingOrder() != null) {
            OrderType orderType = arrangement.getSchedulingOrder()
                    .getOrderType();
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
                        HardMediumSoftLongScore.ONE_SOFT,
                        (arrangement) -> {
                            return Duration.between(
                                            arrangement.getSchedulingWorkCalendar()
                                                    .getStartDateTime(),
                                            arrangement.getArrangeDateTime()
                                    )
                                    .toMinutes() * calcFactor(arrangement);
                        }
                )
                .asConstraint("preferArrangeDateTimeAsSoonAsPassible");
    }

    private Constraint preferMinimizeProductArrangeDateTimeSlotUsage(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .groupBy(
                        SchedulingProducingArrangement::getSchedulingFactoryInstance,
                        ConstraintCollectors.countDistinct(
                                SchedulingProducingArrangement::getSchedulingDateTimeSlot
                        )
                )
                .penalizeLong(
                        HardMediumSoftLongScore.ofSoft(5000L), (factoryInstance, slotAmount) -> slotAmount - 1
                )
                .asConstraint("preferMinimizeProductArrangeDateTimeSlotUsage");
    }

}
