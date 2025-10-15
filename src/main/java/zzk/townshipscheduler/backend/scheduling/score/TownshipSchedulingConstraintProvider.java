package zzk.townshipscheduler.backend.scheduling.score;


import ai.timefold.solver.core.api.score.buildin.bendablelong.BendableLongScore;
import ai.timefold.solver.core.api.score.stream.*;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintStream;
import org.jspecify.annotations.NonNull;
import zzk.townshipscheduler.backend.OrderType;
import zzk.townshipscheduler.backend.scheduling.model.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.function.Function;
import java.util.function.Predicate;

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
        return constraintPrepare(constraintFactory)
                .join(
                        SchedulingProducingArrangement.class,
                        Joiners.equal(
                                (factorySlotsState, arrangement) -> arrangement.getSchedulingFactoryInstance(),
                                SchedulingProducingArrangement::getSchedulingFactoryInstance
                        ),
                        Joiners.lessThan(
                                (factorySlotsState, arrangement) -> arrangement.getId(),
                                SchedulingProducingArrangement::getId
                        )
                )
                .filter((factorySlotsState, current, other) -> {
                            boolean b1 = !other.getArrangeDateTime().isAfter(current.getArrangeDateTime());
                            boolean b2 = other.getCompletedDateTime().isAfter(current.getArrangeDateTime());
                            return b1 && b2;
                        }
                )
                .groupBy(
                        (factorySlotsState, current, other) -> current,
                        ConstraintCollectors.countDistinct((factorySlotsState, current, other) -> other)
                )
                .filter((current, queueSize) ->
                        queueSize > current.getSchedulingFactoryInstance().getProducingQueue()
                )
                .penalizeLong(
                        BendableLongScore.ofHard(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.HARD_BROKEN_FACTORY_ABILITY,
                                1L
                        ),
                        (current, queueSize) -> queueSize - current.getSchedulingFactoryInstance().getProducingQueue()
                )
                .asConstraint("forbidBrokenFactoryAbility");
    }

    private BiConstraintStream<SchedulingFactoryInstanceDateTimeSlotsState, SchedulingProducingArrangement> constraintPrepare(
            @NonNull ConstraintFactory constraintFactory
    ) {
        return constraintFactory.forEach(SchedulingFactoryInstanceDateTimeSlotsState.class)
                .join(
                        constraintFactory.forEach(SchedulingProducingArrangement.class)
                                .filter(SchedulingProducingArrangement::isPlanningAssigned),
                        Joiners.equal(
                                Function.identity(),
                                SchedulingProducingArrangement::getFactorySlotsState
                        )
                );
    }

    private Constraint forbidBrokenPrerequisiteStock(@NonNull ConstraintFactory constraintFactory) {
        return constraintPrepare(
                constraintFactory,
                arrangement -> !arrangement.getDeepPrerequisiteProducingArrangements().isEmpty()
        )
                .join(
                        SchedulingFactoryInstanceDateTimeSlot.class,
                        Joiners.equal(
                                (factorySlotsState, arrangement) -> arrangement.getPlanningFactoryDateTimeSlot(),
                                Function.identity()
                        )
                )
                .groupBy(
                        (factorySlotsState, arrangement, factorySlot) -> arrangement.getArrangeDateTime(),
                        (factorySlotsState, arrangement, factorySlot) -> arrangement.getDeepPrerequisiteProducingArrangementsCompletedDateTime()
                )
                .filter(LocalDateTime::isBefore)
                .penalizeLong(
                        BendableLongScore.ofHard(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.HARD_BROKEN_PRODUCE_PREREQUISITE,
                                1L
                        ),
                        (arrangementDateTime, arrangementPrerequisiteCompleted) ->
                                Duration.between(arrangementDateTime, arrangementPrerequisiteCompleted).toMinutes()
                )
                .asConstraint("forbidBrokenPrerequisiteStock");
    }

    private BiConstraintStream<SchedulingFactoryInstanceDateTimeSlotsState, SchedulingProducingArrangement> constraintPrepare(
            @NonNull ConstraintFactory constraintFactory,
            Predicate<SchedulingProducingArrangement> arrangementPredicate
    ) {
        return constraintFactory.forEach(SchedulingFactoryInstanceDateTimeSlotsState.class)
                .join(
                        constraintFactory.forEach(SchedulingProducingArrangement.class)
                                .filter(SchedulingProducingArrangement::isPlanningAssigned)
                                .filter(arrangementPredicate)
                        ,
                        Joiners.equal(
                                Function.identity(),
                                SchedulingProducingArrangement::getFactorySlotsState
                        )
                );
    }

    private Constraint forbidBrokenDeadlineOrder(@NonNull ConstraintFactory constraintFactory) {
        return constraintPrepare(constraintFactory, SchedulingProducingArrangement::isOrderDirect)
                .join(
                        constraintFactory.forEach(SchedulingOrder.class)
                                .filter(SchedulingOrder::boolHasDeadline),
                        Joiners.equal(
                                (factorySlotsState, arrangement) -> arrangement.getSchedulingOrder(),
                                Function.identity()
                        ),
                        Joiners.filtering((factorySlotsState, arrangement, schedulingOrder) -> {
                                    LocalDateTime deadline = schedulingOrder.getDeadline();
                                    LocalDateTime completedDateTime = arrangement.getCompletedDateTime();
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
                        ((factorySlotsState, arrangement, schedulingOrder) -> {
                            LocalDateTime deadline = schedulingOrder.getDeadline();
                            LocalDateTime completedDateTime = arrangement.getCompletedDateTime();
                            return completedDateTime != null
                                    ? Duration.between(deadline, completedDateTime).toMinutes()
                                    : Duration.between(
                                            arrangement.getSchedulingWorkCalendar().getStartDateTime(),
                                            arrangement.getSchedulingWorkCalendar().getEndDateTime()
                                    ).toMinutes()
                                    ;
                        })
                )
                .asConstraint("shouldNotBrokenDeadlineOrder");
    }

    private Constraint shouldNotBrokenCalendarEnd(@NonNull ConstraintFactory constraintFactory) {
        return constraintPrepare(
                constraintFactory,
                (arrangement) -> {
                    return arrangement.isOrderDirect() && arrangement.getCompletedDateTime().isAfter(
                            arrangement.getSchedulingWorkCalendar().getEndDateTime());
                }
        )
                .groupBy(
                        (factorySlotsState, arrangement) -> arrangement,
                        (factorySlotsState, arrangement) -> arrangement.getSchedulingWorkCalendar().getEndDateTime(),
                        (factorySlotsState, arrangement) -> arrangement.getCompletedDateTime()
                )
                .penalizeLong(
                        BendableLongScore.ofSoft(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.SOFT_TOLERANCE,
                                10L
                        ),
                        (arrangement, workCalendarEnd, arrangementCompleted) -> {
                            return Duration.between(workCalendarEnd, arrangementCompleted).toMinutes();
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
        return constraintPrepare(constraintFactory, SchedulingProducingArrangement::isOrderDirect)
                .join(
                        SchedulingFactoryInstanceDateTimeSlot.class,
                        Joiners.equal(
                                (factorySlotsState, schedulingProducingArrangement) -> schedulingProducingArrangement.getPlanningFactoryDateTimeSlot(),
                                Function.identity()
                        )
                )
                .groupBy(
                        (factorySlotsState, schedulingProducingArrangement, factorySlot) -> schedulingProducingArrangement.getSchedulingOrder(),
                        (factorySlotsState, schedulingProducingArrangement, factorySlot) -> schedulingProducingArrangement
                )
                .groupBy(
                        (order, arrangement) -> arrangement.getSchedulingOrder(),
                        ConstraintCollectors.max(
                                (order, arrangement) -> arrangement,
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
                        BendableLongScore.ofSoft(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.SOFT_BATTER,
                                1L
                        ), (arrangement) -> {
                            LocalDateTime workCalendarStart = arrangement.getSchedulingWorkCalendar()
                                    .getStartDateTime();
                            LocalDateTime arrangeDateTime = arrangement.getArrangeDateTime();
                            return Duration.between(workCalendarStart, arrangeDateTime).toMinutes()
                                   * calcFactor(arrangement);
                        }
                )
                .asConstraint("preferArrangeDateTimeAsSoonAsPassible");
    }

    private Constraint preferMinimizeProductArrangeDateTimeSlotUsage(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .groupBy(
                        SchedulingProducingArrangement::getSchedulingFactoryInstance,
                        ConstraintCollectors.countDistinct(SchedulingProducingArrangement::getSchedulingDateTimeSlot)
                )
                .penalizeLong(
                        BendableLongScore.ofSoft(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.SOFT_BATTER,
                                1000L
                        ), (factoryInstance, slotAmount) -> slotAmount - 1
                )
                .asConstraint("preferMinimizeProductArrangeDateTimeSlotUsage");
    }

}
