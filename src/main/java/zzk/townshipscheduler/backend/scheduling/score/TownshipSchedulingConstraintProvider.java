package zzk.townshipscheduler.backend.scheduling.score;


import ai.timefold.solver.core.api.score.buildin.bendablelong.BendableLongScore;
import ai.timefold.solver.core.api.score.stream.*;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintStream;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintStream;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream;
import io.arxila.javatuples.Trio;
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
        return prepareConstraintOfArrangement(constraintFactory)
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

    private BiConstraintStream<SchedulingFactoryInstanceDateTimeSlotsState, SchedulingProducingArrangement> prepareConstraintOfArrangement(
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
        return prepareConstraintOfHierarchyArrangementPairWithFactorySlot(constraintFactory)
                .groupBy(
                        (whole_Partial_FactorySlotsState__Trio, wholeFactorySlot, partialFactorySlot)
                                -> whole_Partial_FactorySlotsState__Trio.value0(),
                        (whole_Partial_FactorySlotsState__Trio, wholeFactorySlot, partialFactorySlot)
                                -> whole_Partial_FactorySlotsState__Trio.value1()
                )
                .groupBy(
                        (whole, partial) -> whole,
                        ConstraintCollectors.max(
                                (whole, partial) -> partial,
                                SchedulingProducingArrangement::getCompletedDateTime
                        )
                )
                .filter((whole, partial) -> {
                    return whole.getArrangeDateTime().isBefore(partial.getCompletedDateTime());
                })
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

    //<<whole,partial,factorySlotsState>,wholeFactorySlot,partialFactorySlot>
    private TriConstraintStream<
            Trio<SchedulingProducingArrangement, SchedulingProducingArrangement, SchedulingFactoryInstanceDateTimeSlotsState>,
            SchedulingFactoryInstanceDateTimeSlot,
            SchedulingFactoryInstanceDateTimeSlot>
    prepareConstraintOfHierarchyArrangementPairWithFactorySlot(
            ConstraintFactory constraintFactory
    ) {
        UniConstraintStream<Trio<SchedulingProducingArrangement, SchedulingProducingArrangement, SchedulingFactoryInstanceDateTimeSlotsState>> uniConstraintStream
                = prepareConstraintOfHierarchyArrangementPair(
                constraintFactory).map(Trio::of);

        UniConstraintStream<SchedulingFactoryInstanceDateTimeSlot> factoryInstanceDateTimeSlotUniConstraintStream
                = constraintFactory.forEach(SchedulingFactoryInstanceDateTimeSlot.class)
                .filter(
                        factoryInstanceDateTimeSlot -> !factoryInstanceDateTimeSlot.getPlanningSchedulingProducingArrangements()
                                .isEmpty()
                );

        return uniConstraintStream
                .join(
                        factoryInstanceDateTimeSlotUniConstraintStream,
                        Joiners.equal(
                                (whole_Partial_FactorySlotsState__Trio) -> whole_Partial_FactorySlotsState__Trio.value0(),
                                factoryInstanceDateTimeSlot -> factoryInstanceDateTimeSlot
                        )
                )
                .join(
                        factoryInstanceDateTimeSlotUniConstraintStream,
                        Joiners.equal(
                                (whole_Partial_FactorySlotsState__Trio, wholeFactorySlot) -> whole_Partial_FactorySlotsState__Trio.value1(),
                                factoryInstanceDateTimeSlot -> factoryInstanceDateTimeSlot
                        )
                );
    }

    //<whole,partial,factorySlotsState>
    private TriConstraintStream<SchedulingProducingArrangement, SchedulingProducingArrangement, SchedulingFactoryInstanceDateTimeSlotsState> prepareConstraintOfHierarchyArrangementPair(
            ConstraintFactory constraintFactory
    ) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .join(
                        SchedulingProducingArrangement.class,
                        Joiners.filtering(
                                (whole, partial) -> whole.getPrerequisiteProducingArrangements().contains(partial)
                        )
                )
                .join(SchedulingFactoryInstanceDateTimeSlotsState.class);
    }

    private Constraint forbidBrokenDeadlineOrder(@NonNull ConstraintFactory constraintFactory) {
        return prepareConstraintOfArrangement(constraintFactory, SchedulingProducingArrangement::isOrderDirect)
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

    private BiConstraintStream<SchedulingFactoryInstanceDateTimeSlotsState, SchedulingProducingArrangement> prepareConstraintOfArrangement(
            @NonNull ConstraintFactory constraintFactory,
            Predicate<SchedulingProducingArrangement> arrangementPredicate
    ) {
        return constraintFactory.forEach(SchedulingFactoryInstanceDateTimeSlotsState.class)
                .join(
                        constraintFactory.forEachIncludingUnassigned(SchedulingProducingArrangement.class)
                                .filter(SchedulingProducingArrangement::isPlanningAssigned)
                                .filter(arrangementPredicate)
                        ,
                        Joiners.equal(
                                Function.identity(),
                                SchedulingProducingArrangement::getFactorySlotsState
                        )
                );
    }

    private Constraint shouldNotBrokenCalendarEnd(@NonNull ConstraintFactory constraintFactory) {
        return prepareConstraintOfArrangement(
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
        return prepareConstraintOfHierarchyArrangementPairWithFactorySlot(constraintFactory)
                .groupBy(
                        (whole_Partial_FactorySlotsState__Trio, wholeFactorySlot, partialFactorySlot)
                                -> whole_Partial_FactorySlotsState__Trio.value2(),
                        (whole_Partial_FactorySlotsState__Trio, wholeFactorySlot, partialFactorySlot)
                                -> whole_Partial_FactorySlotsState__Trio.value0(),
                        (whole_Partial_FactorySlotsState__Trio, wholeFactorySlot, partialFactorySlot)
                                -> wholeFactorySlot
                )
                .groupBy(
                        (factorySlotsState, schedulingProducingArrangement, factorySlot) -> schedulingProducingArrangement.getSchedulingOrder(),
                        (factorySlotsState, schedulingProducingArrangement, factorySlot) -> schedulingProducingArrangement,
                        (factorySlotsState, schedulingProducingArrangement, factorySlot) -> schedulingProducingArrangement.getCompletedDateTime()
                )
                .filter((order, arrangement, arrangementCompletedDateTime) -> arrangement.isOrderDirect())
                .groupBy(
                        (order, arrangement, arrangementCompletedDateTime)
                                -> arrangement.getSchedulingWorkCalendar().getStartDateTime(),
                        (order, arrangement, arrangementCompletedDateTime) -> arrangement,
                        ConstraintCollectors.max(
                                (order, arrangement, arrangementCompletedDateTime) -> arrangementCompletedDateTime,
                                arrangementCompletedDateTime -> arrangementCompletedDateTime
                        )
                )
                .penalizeLong(
                        BendableLongScore.ofSoft(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.SOFT_BATTER,
                                1L
                        ),
                        (calendarStartDateTime, arrangement, maxArrangementCompletedDateTime) -> {
                            Duration between = Duration.between(calendarStartDateTime, maxArrangementCompletedDateTime);
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
