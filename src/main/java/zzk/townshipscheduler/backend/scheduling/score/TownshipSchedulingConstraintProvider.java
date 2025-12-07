package zzk.townshipscheduler.backend.scheduling.score;


import ai.timefold.solver.core.api.score.buildin.bendablelong.BendableLongScore;
import ai.timefold.solver.core.api.score.stream.*;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintStream;
import org.javatuples.Pair;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import zzk.townshipscheduler.backend.OrderType;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingArrangementsGlobalState;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingOrder;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

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
        return prepareStateAndArrangementsStream(constraintFactory)
                .join(
                        SchedulingProducingArrangement.class,
                        Joiners.equal(
                                (globalState, arrangement) -> arrangement.getPlanningFactoryInstance(),
                                SchedulingProducingArrangement::getPlanningFactoryInstance
                        ),
                        Joiners.lessThan(
                                (globalState, arrangement) -> arrangement.getId(),
                                SchedulingProducingArrangement::getId
                        )
                )
                .filter((globalState, current, other) -> {
                            boolean b1 = !other.getArrangeDateTime()
                                    .isAfter(current.getArrangeDateTime());
                            boolean b2 = globalState.queryCompletedDateTime(other)
                                    .isAfter(current.getArrangeDateTime());
                            return b1 && b2;
                        }
                )
                .groupBy(
                        (globalState, current, other) -> current,
                        ConstraintCollectors.countDistinct((globalState, current, other) -> other)
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

    private BiConstraintStream<SchedulingArrangementsGlobalState, SchedulingProducingArrangement> prepareStateAndArrangementsStream(
            @NonNull ConstraintFactory constraintFactory
    ) {
        return constraintFactory.forEach(SchedulingArrangementsGlobalState.class)
                .join(
                        constraintFactory.forEach(SchedulingProducingArrangement.class)
                                .filter(SchedulingProducingArrangement::isPlanningAssigned),
                        Joiners.equal(
                                Function.identity(),
                                SchedulingProducingArrangement::getSchedulingArrangementsGlobalState
                        )
                );
    }

    private Constraint forbidBrokenPrerequisiteStock(@NonNull ConstraintFactory constraintFactory) {
        return prepareStateAndArrangementsStream(
                constraintFactory,
                arrangement -> !arrangement.getDeepPrerequisiteProducingArrangements()
                        .isEmpty()
        )
                .groupBy(
                        (globalState, arrangement) -> arrangement.getArrangeDateTime(),
                        (globalState, arrangement) -> arrangement.getDeepPrerequisiteProducingArrangementsCompletedDateTime()
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
                                Duration.between(
                                                arrangementDateTime,
                                                arrangementPrerequisiteCompleted
                                        )
                                        .toMinutes()
                )
                .asConstraint("forbidBrokenPrerequisiteStock");
    }

    private BiConstraintStream<SchedulingArrangementsGlobalState, SchedulingProducingArrangement> prepareStateAndArrangementsStream(
            @NonNull ConstraintFactory constraintFactory,
            Predicate<SchedulingProducingArrangement> arrangementPredicate
    ) {
        return constraintFactory.forEach(SchedulingArrangementsGlobalState.class)
                .join(
                        constraintFactory.forEach(SchedulingProducingArrangement.class)
                                .filter(arrangementPredicate),
                        Joiners.equal(
                                Function.identity(),
                                SchedulingProducingArrangement::getSchedulingArrangementsGlobalState
                        )
                );
    }

    private Constraint forbidBrokenDeadlineOrder(@NonNull ConstraintFactory constraintFactory) {
        return prepareStateAndArrangementsStream(
                constraintFactory,
                SchedulingProducingArrangement::isOrderDirect
        )
                .join(
                        constraintFactory.forEach(SchedulingOrder.class)
                                .filter(SchedulingOrder::boolHasDeadline),
                        Joiners.equal(
                                (globalState, arrangement) -> arrangement.getSchedulingOrder(),
                                Function.identity()
                        ),
                        Joiners.filtering((globalState, arrangement, schedulingOrder) -> {
                                    LocalDateTime deadline = schedulingOrder.getDeadline();
                                    LocalDateTime completedDateTime = globalState.queryCompletedDateTime(arrangement);
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
                        ((globalState, arrangement, schedulingOrder) -> {
                            LocalDateTime deadline = schedulingOrder.getDeadline();
                            LocalDateTime completedDateTime = globalState.queryCompletedDateTime(arrangement);
                            return completedDateTime != null
                                    ? Duration.between(
                                            deadline,
                                            completedDateTime
                                    )
                                    .toMinutes()
                                    : Duration.between(
                                                    arrangement.getSchedulingWorkCalendar()
                                                            .getStartDateTime(),
                                                    arrangement.getSchedulingWorkCalendar()
                                                            .getEndDateTime()
                                            )
                                            .toMinutes()
                                    ;
                        })
                )
                .asConstraint("shouldNotBrokenDeadlineOrder");
    }

    private Constraint shouldNotBrokenCalendarEnd(@NonNull ConstraintFactory constraintFactory) {
        return prepareStateAndArrangementsStream(constraintFactory)
                .filter((globalState, schedulingProducingArrangement) -> {
                    boolean orderDirect = schedulingProducingArrangement.isOrderDirect();
                    LocalDateTime arrangementCompletedDateTime = globalState.queryCompletedDateTime(schedulingProducingArrangement);
                    boolean completedDateTimeAfterWorkCalendar
                            = arrangementCompletedDateTime.isAfter(schedulingProducingArrangement.getSchedulingWorkCalendar()
                            .getEndDateTime());
                    return orderDirect && completedDateTimeAfterWorkCalendar;
                })
                .groupBy(
                        (globalState, arrangement) -> arrangement,
                        (globalState, arrangement) -> arrangement.getSchedulingWorkCalendar()
                                .getEndDateTime(),
                        (globalState, arrangement) -> globalState.queryCompletedDateTime(arrangement)
                )
                .penalizeLong(
                        BendableLongScore.ofSoft(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.SOFT_TOLERANCE,
                                10L
                        ),
                        (arrangement, workCalendarEnd, arrangementCompleted) -> {
                            return Duration.between(
                                            workCalendarEnd,
                                            arrangementCompleted
                                    )
                                    .toMinutes();
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
        return constraintFactory.forEach(SchedulingOrder.class)
                .join(
                        constraintFactory.forEach(SchedulingProducingArrangement.class)
                                .filter(SchedulingProducingArrangement::isOrderDirect),
                        Joiners.equal(
                                Function.identity(),
                                SchedulingProducingArrangement::getSchedulingOrder
                        )
                )
                .join(
                        SchedulingArrangementsGlobalState.class,
                        Joiners.equal(
                                (order, arrangement) -> arrangement.getSchedulingArrangementsGlobalState(),
                                Function.identity()
                        )
                )

                .groupBy(
                        (schedulingOrder, schedulingProducingArrangement, schedulingArrangementsGlobalState) -> schedulingOrder,
                        ConstraintCollectors.max(
                                (schedulingOrder, producingArrangement, globalState) -> new Pair<>(
                                        producingArrangement,
                                        globalState
                                ),
                                funcForArrangementAndStateToCompletedDateTime()
                        )
                )
                .penalizeLong(
                        BendableLongScore.ofSoft(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.SOFT_BATTER,
                                1L
                        ),
                        (order, pair) -> {
                            SchedulingProducingArrangement arrangement = pair.getValue0();
                            var calendarStartDateTime = arrangement.getSchedulingWorkCalendar()
                                    .getStartDateTime();
                            var completedDateTime = funcForArrangementAndStateToCompletedDateTime().apply(pair);
                            Duration between = Duration.between(
                                    calendarStartDateTime,
                                    completedDateTime
                            );
                            return calcFactor(arrangement) * between.toMinutes();
                        }
                )
                .asConstraint("preferMinimizeOrderCompletedDateTime");
    }

    @NotNull
    private static Function<Pair<SchedulingProducingArrangement, SchedulingArrangementsGlobalState>, LocalDateTime> funcForArrangementAndStateToCompletedDateTime() {
        return (pair) -> pair.getValue1()
                .queryCompletedDateTime(pair.getValue0());
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
                            LocalDateTime workCalendarStart = arrangement.getSchedulingWorkCalendar()
                                    .getStartDateTime();
                            LocalDateTime arrangeDateTime = arrangement.getArrangeDateTime();
                            return Duration.between(
                                            workCalendarStart,
                                            arrangeDateTime
                                    ).toMinutes()
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
                        ),
                        (factoryInstance, slotAmount) -> slotAmount - 1
                )
                .asConstraint("preferMinimizeProductArrangeDateTimeSlotUsage");
    }

    private BiConstraintStream<SchedulingProducingArrangement, SchedulingArrangementsGlobalState> prepareArrangementsAndStateStream(
            @NonNull ConstraintFactory constraintFactory,
            Predicate<SchedulingProducingArrangement> arrangementPredicate
    ) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .filter(arrangementPredicate)
                .join(
                        SchedulingArrangementsGlobalState.class,
                        Joiners.equal(
                                SchedulingProducingArrangement::getSchedulingArrangementsGlobalState,
                                Function.identity()
                        )
                );
    }

}
