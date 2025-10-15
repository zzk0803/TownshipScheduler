package zzk.townshipscheduler.backend.scheduling.score;


import ai.timefold.solver.core.api.score.buildin.bendablelong.BendableLongScore;
import ai.timefold.solver.core.api.score.stream.*;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintStream;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintStream;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream;
import org.javatuples.Triplet;
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
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .filter(SchedulingProducingArrangement::isPlanningAssigned)
                .join(
                        SchedulingProducingArrangement.class,
                        Joiners.equal(SchedulingProducingArrangement::getPlanningFactoryInstance),
                        Joiners.lessThan(SchedulingProducingArrangement::getId)
                )
                .filter((current, other) -> !other.getArrangeDateTime().isAfter(current.getArrangeDateTime())
                                            && other.getCompletedDateTime().isAfter(current.getArrangeDateTime())
                )
                .groupBy(
                        (current, other) -> current,
                        ConstraintCollectors.countDistinct((current, other) -> other)
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

    private Constraint forbidBrokenPrerequisiteStock(@NonNull ConstraintFactory constraintFactory) {
        return prepareHierarchyArrangementPairConstraint(
                constraintFactory,
                SchedulingProducingArrangement::weatherPrerequisiteRequire,
                SchedulingProducingArrangement::isPlanningAssigned
        )
                .groupBy(
                        (wholeTriplet, partialTriplet) -> wholeTriplet.getValue1(),
                        (wholeTriplet, partialTriplet) -> wholeTriplet.getValue0(),
                        (wholeTriplet, partialTriplet) -> partialTriplet.getValue1(),
                        (wholeTriplet, partialTriplet) -> partialTriplet.getValue0()
                )
                .groupBy(
                        (whole, wholeFactoryInstance, partial, partialFactoryInstance) -> whole,
                        (whole, wholeFactoryInstance, partial, partialFactoryInstance) -> wholeFactoryInstance.getArrangementToComputedPairMap(),
                        (whole, wholeFactoryInstance, partial, partialFactoryInstance) -> partialFactoryInstance.getArrangementToComputedPairMap(),
                        ConstraintCollectors.toList(
                                (whole, wholeDateTimeSlot, partial, partialDateTimeSlot) -> partial.getCompletedDateTime()
                        )
                )
                .flattenLast(localDateTimes -> localDateTimes)
                .filter(
                        (whole, wholeFactoryInstanceComputedMap, partialFactoryInstanceComputedMap, partialCompletedDateTime)
                                -> whole.getArrangeDateTime().isBefore(partialCompletedDateTime)
                )
                .penalizeLong(
                        BendableLongScore.ofHard(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.HARD_BROKEN_PRODUCE_PREREQUISITE,
                                1L
                        ),
                        (whole, wholeFactoryInstanceComputedMap, partialFactoryInstanceComputedMap, partialCompletedDateTime) ->
                                Duration.between(whole.getArrangeDateTime(), partialCompletedDateTime).toMinutes()
                )
                .asConstraint("forbidBrokenPrerequisiteStock");
    }

    private BiConstraintStream<
            Triplet<SchedulingFactoryInstance, SchedulingProducingArrangement, SchedulingDateTimeSlot>,
            Triplet<SchedulingFactoryInstance, SchedulingProducingArrangement, SchedulingDateTimeSlot>
            > prepareHierarchyArrangementPairConstraint(
            @NonNull ConstraintFactory constraintFactory,
            Predicate<SchedulingProducingArrangement> wholeArrangementPredicate,
            Predicate<SchedulingProducingArrangement> partialArrangementPredicate
    ) {
        UniConstraintStream<Triplet<SchedulingFactoryInstance, SchedulingProducingArrangement, SchedulingDateTimeSlot>> wholeStream
                = prepareArrangementConstraint(
                constraintFactory,
                wholeArrangementPredicate
        ).map(Triplet::with);

        UniConstraintStream<Triplet<SchedulingFactoryInstance, SchedulingProducingArrangement, SchedulingDateTimeSlot>> partialStream
                = prepareArrangementConstraint(
                constraintFactory,
                partialArrangementPredicate
        ).map(Triplet::with);

        return constraintFactory.forEach(SchedulingArrangementHierarchies.class)
                .join(
                        wholeStream,
                        Joiners.equal(
                                (hierarchies) -> hierarchies.getWhole(),
                                Triplet::getValue1
                        )
                )
                .join(
                        partialStream,
                        Joiners.equal(
                                (hierarchies, wholeTriplet) -> hierarchies.getPartial(),
                                Triplet::getValue1
                        )
                )
                .groupBy(
                        (hierarchies, wholeTriplet, partialTriplet) -> wholeTriplet,
                        (hierarchies, wholeTriplet, partialTriplet) -> partialTriplet
                );
    }

    private TriConstraintStream<SchedulingFactoryInstance, SchedulingProducingArrangement, SchedulingDateTimeSlot> prepareArrangementConstraint(
            @NonNull ConstraintFactory constraintFactory,
            Predicate<SchedulingProducingArrangement> arrangementPredicate
    ) {
        return constraintFactory.forEach(SchedulingFactoryInstance.class)
                .join(
                        constraintFactory.forEach(SchedulingProducingArrangement.class).filter(arrangementPredicate),
                        Joiners.equal(
                                Function.identity(),
                                SchedulingProducingArrangement::getPlanningFactoryInstance
                        )
                )
                .join(
                        SchedulingDateTimeSlot.class,
                        Joiners.equal(
                                (schedulingFactoryInstance, schedulingProducingArrangement) -> schedulingProducingArrangement.getPlanningDateTimeSlot(),
                                Function.identity()
                        )
                );

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
                                    ? Duration.between(deadline, completedDateTime).toMinutes()
                                    : Duration.between(
                                            producingArrangement.getSchedulingWorkCalendar().getStartDateTime(),
                                            producingArrangement.getSchedulingWorkCalendar().getEndDateTime()
                                    ).toMinutes();
                        })
                )
                .asConstraint("shouldNotBrokenDeadlineOrder");
    }

    private Constraint shouldNotBrokenCalendarEnd(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .filter(schedulingProducingArrangement -> {
                            return schedulingProducingArrangement.isOrderDirect()
                                   && schedulingProducingArrangement.getCompletedDateTime()
                                           .isAfter(
                                                   schedulingProducingArrangement.getSchedulingWorkCalendar()
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
        return prepareHierarchyArrangementPairConstraint(
                constraintFactory,
                SchedulingProducingArrangement::isOrderDirect,
                SchedulingProducingArrangement::isPlanningAssigned
        )
                .groupBy(
                        (whole, partial) -> whole.getValue1().getSchedulingOrder(),
                        ConstraintCollectors.max(
                                (whole, partial) -> whole.getValue1(),
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

}
