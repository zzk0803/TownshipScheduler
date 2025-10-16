package zzk.townshipscheduler.backend.scheduling.score;


import ai.timefold.solver.core.api.score.buildin.bendablelong.BendableLongScore;
import ai.timefold.solver.core.api.score.stream.*;
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
        return prepareArrangementStreamAsIt(
                constraintFactory,
                arrangement -> arrangement.getArrangeDateTime() != null && arrangement.getCompletedDateTime() != null
        )
                .join(
                        prepareArrangementStreamAsIt(
                                constraintFactory,
                                arrangement -> arrangement.getArrangeDateTime() != null && arrangement.getCompletedDateTime() != null
                        ),
                        Joiners.equal(SchedulingProducingArrangement::getSchedulingFactoryInstance)
                )
                .filter((current, other) -> {
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

    private UniConstraintStream<SchedulingProducingArrangement> prepareArrangementStreamAsIt(
            @NonNull ConstraintFactory constraintFactory,
            Predicate<SchedulingProducingArrangement> arrangementPredicate
    ) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .filter(arrangementPredicate)
                .join(
                        SchedulingFactoryInstance.class,
                        Joiners.equal(SchedulingProducingArrangement::getSchedulingFactoryInstance, Function.identity())
                )
                .join(
                        SchedulingFactoryInstanceDateTimeSlot.class,
                        Joiners.equal(
                                (arrangement, factory) -> arrangement.getPlanningFactoryDateTimeSlot(),
                                Function.identity()
                        )
                )
                .groupBy((arrangement, factoryInstance, factoryInstanceDateTimeSlot) -> arrangement);
    }

    private Constraint forbidBrokenPrerequisiteStock(@NonNull ConstraintFactory constraintFactory) {
        return prepareArrangementStreamAsTrio(constraintFactory)
                .join(
                        SchedulingArrangementHierarchies.class,
                        Joiners.equal(
                                Trio::value0,
                                SchedulingArrangementHierarchies::getWhole
                        )
                )
                .join(
                        prepareArrangementStreamAsTrio(constraintFactory),
                        Joiners.equal(
                                (wholeTrio, hierarchies) -> hierarchies.getPartial(),
                                Trio::value0
                        )
                )
                .groupBy(
                        (trio, hierarchies, trio2) -> trio.value0(),
                        ConstraintCollectors.max(
                                (trio, hierarchies, trio2) -> trio2.value0(),
                                SchedulingProducingArrangement::getCompletedDateTime
                        )
                )
                .filter((whole, partialMax) ->
                        whole.getArrangeDateTime().isBefore(partialMax.getCompletedDateTime())
                )
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

    private UniConstraintStream<SchedulingProducingArrangement> prepareArrangementStreamAsIt(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .join(
                        SchedulingFactoryInstance.class,
                        Joiners.equal(SchedulingProducingArrangement::getSchedulingFactoryInstance, Function.identity())
                )
                .join(
                        SchedulingFactoryInstanceDateTimeSlot.class,
                        Joiners.equal(
                                (arrangement, factory) -> arrangement.getPlanningFactoryDateTimeSlot(),
                                Function.identity()
                        )
                )
                .groupBy((arrangement, factoryInstance, factoryInstanceDateTimeSlot) -> arrangement);
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
                                    ).toMinutes()
                                    ;
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
                .join(
                        SchedulingPlayer.class,
                        Joiners.equal(SchedulingProducingArrangement::getSchedulingPlayer, Function.identity())
                )
                .filter((schedulingProducingArrangement, schedulingPlayer) -> {
                    LocalDateTime arrangeDateTime = schedulingProducingArrangement.getArrangeDateTime();
                    LocalTime sleepStart = schedulingPlayer.getSleepStart();
                    LocalTime sleepEnd = schedulingPlayer.getSleepEnd();
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
        return prepareArrangementStream(constraintFactory, SchedulingProducingArrangement::isOrderDirect)
                .groupBy(
                        (arrangement, factoryInstance, factoryInstanceDateTimeSlot) ->arrangement.getSchedulingOrder(),
                        ConstraintCollectors.max(
                                (arrangement, factoryInstance, factoryInstanceDateTimeSlot)->arrangement,
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
                        SchedulingProducingArrangement::getSchedulingFactoryInstance,
                        ConstraintCollectors.countDistinct(SchedulingProducingArrangement::getSchedulingDateTimeSlot)
                )
                .penalizeLong(
                        BendableLongScore.ofSoft(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.SOFT_BATTER,
                                1000L
                        )
                )
                .asConstraint("preferMinimizeProductArrangeDateTimeSlotUsage");
    }

    private UniConstraintStream<Trio<SchedulingProducingArrangement, SchedulingFactoryInstance, SchedulingFactoryInstanceDateTimeSlot>>
    prepareArrangementStreamAsTrio(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .join(
                        SchedulingFactoryInstance.class,
                        Joiners.equal(SchedulingProducingArrangement::getSchedulingFactoryInstance, Function.identity())
                )
                .join(
                        SchedulingFactoryInstanceDateTimeSlot.class,
                        Joiners.equal(
                                (arrangement, factory) -> arrangement.getPlanningFactoryDateTimeSlot(),
                                Function.identity()
                        )
                )
                .map(Trio::of);
    }

    private UniConstraintStream<Trio<SchedulingProducingArrangement, SchedulingFactoryInstance, SchedulingFactoryInstanceDateTimeSlot>>
    prepareArrangementStreamAsTrio(
            @NonNull ConstraintFactory constraintFactory,
            Predicate<SchedulingProducingArrangement> arrangementPredicate
    ) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .filter(arrangementPredicate)
                .join(
                        SchedulingFactoryInstance.class,
                        Joiners.equal(SchedulingProducingArrangement::getSchedulingFactoryInstance, Function.identity())
                )
                .join(
                        SchedulingFactoryInstanceDateTimeSlot.class,
                        Joiners.equal(
                                (arrangement, factory) -> arrangement.getPlanningFactoryDateTimeSlot(),
                                Function.identity()
                        )
                )
                .map(Trio::of);
    }

    private TriConstraintStream<SchedulingProducingArrangement, SchedulingFactoryInstance, SchedulingFactoryInstanceDateTimeSlot>
    prepareArrangementStream(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .join(
                        SchedulingFactoryInstance.class,
                        Joiners.equal(SchedulingProducingArrangement::getSchedulingFactoryInstance, Function.identity())
                )
                .join(
                        SchedulingFactoryInstanceDateTimeSlot.class,
                        Joiners.equal(
                                (arrangement, factory) -> arrangement.getPlanningFactoryDateTimeSlot(),
                                Function.identity()
                        )
                );
    }

    private TriConstraintStream<SchedulingProducingArrangement, SchedulingFactoryInstance, SchedulingFactoryInstanceDateTimeSlot>
    prepareArrangementStream(
            @NonNull ConstraintFactory constraintFactory,
            Predicate<SchedulingProducingArrangement> arrangementPredicate
    ) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .filter(arrangementPredicate)
                .join(
                        SchedulingFactoryInstance.class,
                        Joiners.equal(SchedulingProducingArrangement::getSchedulingFactoryInstance, Function.identity())
                )
                .join(
                        SchedulingFactoryInstanceDateTimeSlot.class,
                        Joiners.equal(
                                (arrangement, factory) -> arrangement.getPlanningFactoryDateTimeSlot(),
                                Function.identity()
                        )
                );
    }

}
