package zzk.townshipscheduler.backend.scheduling.score;


import ai.timefold.solver.core.api.score.buildin.bendablelong.BendableLongScore;
import ai.timefold.solver.core.api.score.stream.*;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintStream;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintStream;
import org.javatuples.Pair;
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
        return prepareArrangementsConstraint(constraintFactory)
                .join(
                        SchedulingProducingArrangement.class,
                        Joiners.equal(
                                (factoryState, arrangement) -> arrangement.getPlanningFactoryInstance(),
                                SchedulingProducingArrangement::getPlanningFactoryInstance
                        )
                )
                .filter((factoryState, current, other) -> {
                            boolean b1 = !other.getArrangeDateTime().isAfter(current.getArrangeDateTime());
                            boolean b2 = other.getCompletedDateTime().isAfter(current.getArrangeDateTime());
                            return b1 && b2;
                        }
                )
                .groupBy(
                        (factoryState, current, other) -> current,
                        ConstraintCollectors.countDistinct((factoryState, current, other) -> other)
                )
                .filter((current, queueSize) ->
                        queueSize > current.getPlanningFactoryInstance().getProducingLength()
                )
                .penalizeLong(
                        BendableLongScore.ofHard(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.HARD_BROKEN_FACTORY_ABILITY,
                                1L
                        ),
                        (current, queueSize) -> queueSize - current.getPlanningFactoryInstance().getProducingLength()
                )
                .asConstraint("forbidBrokenFactoryAbility");
    }

    private BiConstraintStream<SchedulingArrangementsFactoriesState, SchedulingProducingArrangement> prepareArrangementsConstraint(
            @NonNull ConstraintFactory constraintFactory
    ) {
        return constraintFactory.forEach(SchedulingArrangementsFactoriesState.class)
                .join(
                        constraintFactory.forEach(SchedulingProducingArrangement.class)
                                .filter(SchedulingProducingArrangement::isPlanningAssigned),
                        Joiners.equal(
                                Function.identity(),
                                SchedulingProducingArrangement::getSchedulingArrangementsFactoriesState
                        )
                );
    }

    private Constraint forbidBrokenPrerequisiteStock(@NonNull ConstraintFactory constraintFactory) {
        return prepareWholePartialArrangementsConstraint(constraintFactory)
                .groupBy(
                        (whole,partial,wholeState,partialState) -> whole,
                        (whole,partial,wholeState,partialState) -> partial
                )
                .filter((whole, partial) -> whole.getArrangeDateTime().isBefore(partial.getCompletedDateTime()))
                .penalizeLong(
                        BendableLongScore.ofHard(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.HARD_BROKEN_PRODUCE_PREREQUISITE,
                                1L
                        ),
                        (whole, partial) ->
                                Duration.between(whole.getArrangeDateTime(), partial.getCompletedDateTime()).toMinutes()
                )
                .asConstraint("forbidBrokenPrerequisiteStock");
    }

    private BiConstraintStream<SchedulingArrangementsFactoriesState, SchedulingProducingArrangement> prepareArrangementsConstraint(
            @NonNull ConstraintFactory constraintFactory,
            Predicate<SchedulingProducingArrangement> arrangementPredicate
    ) {
        return constraintFactory.forEach(SchedulingArrangementsFactoriesState.class)
                .join(
                        constraintFactory.forEach(SchedulingProducingArrangement.class)
                                .filter(SchedulingProducingArrangement::isPlanningAssigned)
                                .filter(arrangementPredicate)
                        ,
                        Joiners.equal(
                                Function.identity(),
                                SchedulingProducingArrangement::getSchedulingArrangementsFactoriesState
                        )
                );
    }

    private Constraint forbidBrokenDeadlineOrder(@NonNull ConstraintFactory constraintFactory) {
        return prepareArrangementsConstraint(constraintFactory, SchedulingProducingArrangement::isOrderDirect)
                .join(
                        constraintFactory.forEach(SchedulingOrder.class)
                                .filter(SchedulingOrder::boolHasDeadline),
                        Joiners.equal(
                                (factoryState, arrangement) -> arrangement.getSchedulingOrder(),
                                Function.identity()
                        ),
                        Joiners.filtering((factoryState, arrangement, schedulingOrder) -> {
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
                        ((factoryState, arrangement, schedulingOrder) -> {
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
        return prepareArrangementsConstraint(
                constraintFactory,
                (arrangement) -> {
                    return arrangement.isOrderDirect() && arrangement.getCompletedDateTime()
                            .isAfter(
                                    arrangement.getSchedulingWorkCalendar()
                                            .getEndDateTime()
                            );
                }
        )
                .penalizeLong(
                        BendableLongScore.ofSoft(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.SOFT_TOLERANCE,
                                10L
                        ),
                        (factoryState, arrangement) -> {
                            LocalDateTime completedDateTime = arrangement.getCompletedDateTime();
                            LocalDateTime workCalendarStart = arrangement.getSchedulingWorkCalendar()
                                    .getStartDateTime();
                            LocalDateTime workCalendarEnd = arrangement.getSchedulingWorkCalendar()
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
        return prepareArrangementsConstraint(constraintFactory, SchedulingProducingArrangement::isOrderDirect)
                .groupBy(
                        (schedulingArrangementsFactoriesState, schedulingProducingArrangement) -> schedulingProducingArrangement.getSchedulingOrder(),
                        ConstraintCollectors.max(
                                (schedulingArrangementsFactoriesState, schedulingProducingArrangement) -> schedulingProducingArrangement,
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
                        SchedulingProducingArrangement::getPlanningFactoryInstance,
                        ConstraintCollectors.countDistinct(SchedulingProducingArrangement::getPlanningDateTimeSlot)
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

    //<whole,partial,wholeState,partialState>
    private QuadConstraintStream<SchedulingProducingArrangement, SchedulingProducingArrangement, SchedulingArrangementsFactoriesState, SchedulingArrangementsFactoriesState>
    prepareWholePartialArrangementsConstraint(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(
                        SchedulingProducingArrangement.class,
                        Joiners.lessThan(SchedulingProducingArrangement::getId),
                        Joiners.filtering(SchedulingProducingArrangement::isDeepPrerequisiteArrangement)
                )
                .join(
                        SchedulingArrangementsFactoriesState.class,
                        Joiners.equal(
                                (whole, partial) -> whole.getSchedulingArrangementsFactoriesState(),
                                Function.identity()
                        )
                )
                .join(
                        SchedulingArrangementsFactoriesState.class,
                        Joiners.equal(
                                (whole, partial, wholeState) -> partial.getSchedulingArrangementsFactoriesState(),
                                Function.identity()
                        )
                );
    }

}
