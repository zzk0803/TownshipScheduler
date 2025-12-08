package zzk.townshipscheduler.backend.scheduling.score;


import ai.timefold.solver.core.api.score.buildin.bendablelong.BendableLongScore;
import ai.timefold.solver.core.api.score.stream.*;
import org.jspecify.annotations.NonNull;
import zzk.townshipscheduler.backend.OrderType;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingDateTimeSlot;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingOrder;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

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
        return constraintFactory.forEachUniquePair(
                        SchedulingProducingArrangement.class,
                        Joiners.equal(SchedulingProducingArrangement::getPlanningFactoryInstance),
                        Joiners.lessThan(SchedulingProducingArrangement::getFactoryProcessSequence)
                )
                .join(
                        SchedulingDateTimeSlot.class,
                        Joiners.equal(
                                (current, other) -> current.getPlanningDateTimeSlot(),
                                Function.identity()
                        )
                )
                .join(
                        SchedulingDateTimeSlot.class,
                        Joiners.equal(
                                (current, other, currentDateTimeSlot) -> other.getPlanningDateTimeSlot(),
                                Function.identity()
                        )
                )
                .filter((current, other, currentDateTimeSlot, otherDateTimeSlot) -> {
                            boolean b1 = !other.getArrangeDateTime()
                                    .isAfter(current.getArrangeDateTime());
                            boolean b2 = other.getCompletedDateTime()
                                    .isAfter(current.getArrangeDateTime());
                            return b1 && b2;
                        }
                )
                .groupBy(
                        (current, other, currentDateTimeSlot, otherDateTimeSlot) -> current,
                        ConstraintCollectors.countDistinct((current, other, currentDateTimeSlot, otherDateTimeSlot) -> other)
                )
                .filter((current, queueSize) ->
                        queueSize > current.getPlanningFactoryInstance()
                                .getProducingLength()
                )
                .penalizeLong(
                        BendableLongScore.ofHard(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.HARD_BROKEN_FACTORY_ABILITY,
                                1L
                        ),
                        (current, queueSize) -> queueSize - current.getPlanningFactoryInstance()
                                .getProducingLength()
                )
                .asConstraint("forbidBrokenFactoryAbility");
    }

    private Constraint forbidBrokenPrerequisiteStock(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .filter(SchedulingProducingArrangement::weatherPrerequisiteRequire)
                .join(
                        SchedulingProducingArrangement.class,
                        Joiners.filtering(
                                (whole, partial) -> {
                                    return whole.getDeepPrerequisiteProducingArrangements()
                                            .contains(partial);
                                }
                        )
                )
                .join(
                        SchedulingDateTimeSlot.class,
                        Joiners.equal(
                                (whole, partial) -> whole.getPlanningDateTimeSlot(),
                                Function.identity()
                        )
                )
                .join(
                        SchedulingDateTimeSlot.class,
                        Joiners.equal(
                                (whole, partial, wholeDateTimeSlot) -> partial.getPlanningDateTimeSlot(),
                                Function.identity()
                        )
                )
                .groupBy(
                        (whole, partial, wholeDateTimeSlot, partialDateTimeSlot) -> whole,
                        (whole, partial, wholeDateTimeSlot, partialDateTimeSlot) -> partial,
                        ConstraintCollectors.max(
                                (whole, partial, wholeDateTimeSlot, partialDateTimeSlot) -> partial,
                                SchedulingProducingArrangement::getCompletedDateTime
                        )
                )
                .filter((whole, partial, partialMax) -> whole.getArrangeDateTime()
                        .isBefore(partialMax.getCompletedDateTime()))
                .penalizeLong(
                        BendableLongScore.ofHard(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.HARD_BROKEN_PRODUCE_PREREQUISITE,
                                1L
                        ),
                        (whole, partial, partialMax) ->
                                Duration.between(
                                                whole.getArrangeDateTime(),
                                                partialMax.getCompletedDateTime()
                                        )
                                        .toMinutes()
                )
                .asConstraint("forbidBrokenPrerequisiteStock");
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
                        BendableLongScore.ofHard(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.HARD_BROKEN_DEADLINE,
                                1L
                        ),
                        ((schedulingOrder, producingArrangement) -> {
                            LocalDateTime deadline = schedulingOrder.getDeadline();
                            LocalDateTime completedDateTime = producingArrangement.getCompletedDateTime();
                            return completedDateTime != null
                                    ? Duration.between(
                                            deadline,
                                            completedDateTime
                                    )
                                    .toMinutes()
                                    : Duration.between(
                                                    producingArrangement.getSchedulingWorkCalendar()
                                                            .getStartDateTime(),
                                                    producingArrangement.getSchedulingWorkCalendar()
                                                            .getEndDateTime()
                                            )
                                            .toMinutes();
                        })
                )
                .asConstraint("shouldNotBrokenDeadlineOrder");
    }

    private Constraint shouldNotBrokenCalendarEnd(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .filter(schedulingProducingArrangement -> {
                            LocalDateTime completedDateTime = schedulingProducingArrangement.getCompletedDateTime();
                            return schedulingProducingArrangement.isOrderDirect()
                                   && (Objects.isNull(completedDateTime)
                                       || completedDateTime.isAfter(
                                    schedulingProducingArrangement.getSchedulingWorkCalendar()
                                            .getEndDateTime()
                            ));
                        }
                )
                .penalizeLong(
                        BendableLongScore.ofSoft(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.SOFT_TOLERANCE,
                                10L
                        ),
                        schedulingProducingArrangement -> {
                            LocalDateTime completedDateTime = schedulingProducingArrangement.getCompletedDateTime();
                            LocalDateTime workCalendarStart = schedulingProducingArrangement.getSchedulingWorkCalendar()
                                    .getStartDateTime();
                            LocalDateTime workCalendarEnd = schedulingProducingArrangement.getSchedulingWorkCalendar()
                                    .getEndDateTime();
                            if (completedDateTime != null) {
                                return Duration.between(
                                                workCalendarEnd,
                                                completedDateTime
                                        )
                                        .toMinutes();
                            } else {
                                return Duration.between(
                                                workCalendarStart,
                                                workCalendarEnd
                                        )
                                        .toMinutes();
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
                    LocalTime sleepStart = schedulingProducingArrangement.getSchedulingPlayer()
                            .getSleepStart();
                    LocalTime sleepEnd = schedulingProducingArrangement.getSchedulingPlayer()
                            .getSleepEnd();
                    LocalTime arrangeTime = arrangeDateTime.toLocalTime();
                    return (arrangeTime.isAfter(sleepStart) && arrangeTime.isBefore(LocalTime.MAX))
                           || (arrangeTime.isAfter(LocalTime.MIN) && arrangeTime.isBefore(sleepEnd));
                })
                .penalizeLong(
                        BendableLongScore.ofSoft(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.SOFT_TOLERANCE,
                                500L
                        )
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
                        BendableLongScore.ofSoft(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.SOFT_BATTER,
                                1L
                        ),
                        (order, arrangement) -> {
                            var calendarStartDateTime = arrangement.getSchedulingWorkCalendar()
                                    .getStartDateTime();
                            var completedDateTime = arrangement.getCompletedDateTime();
                            Duration between = Duration.between(
                                    calendarStartDateTime,
                                    completedDateTime
                            );
                            return calcFactor(arrangement) * between.toMinutes();
                        }
                )
                .asConstraint("preferMinimizeOrderCompletedDateTime");
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
                        BendableLongScore.ofSoft(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.SOFT_BATTER,
                                1L
                        ),
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
                        SchedulingProducingArrangement::getPlanningFactoryInstance,
                        ConstraintCollectors.countDistinct(SchedulingProducingArrangement::getPlanningDateTimeSlot)
                )
                .penalizeLong(
                        BendableLongScore.ofSoft(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.SOFT_BATTER,
                                1000L
                        ),
                        (factoryInstance, slotAmount) -> slotAmount - 1
                )
                .asConstraint("preferMinimizeProductArrangeDateTimeSlotUsage");
    }

}
