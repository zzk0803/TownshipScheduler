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
        return prepareConstrainStreamAsTrio(constraintFactory)
                .filter(trio -> trio.value0().getArrangeDateTime() != null && trio.value0()
                                                                                      .getCompletedDateTime() != null)
                .join(
                        prepareConstrainStreamAsTrio(constraintFactory),
                        Joiners.equal(Trio::value1)
                )
                .filter((currentTrio, otherTrio) -> !otherTrio.value0()
                        .getArrangeDateTime()
                        .isAfter(currentTrio.value0().getArrangeDateTime())
                                                    && otherTrio.value0()
                                                            .getCompletedDateTime()
                                                            .isAfter(currentTrio.value0().getArrangeDateTime())
                )
                .groupBy(
                        (currentTrio, otherTrio) -> currentTrio.value0(),
                        ConstraintCollectors.countDistinct((currentTrio, otherTrio) -> otherTrio.value0())
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

    private UniConstraintStream<Trio<SchedulingProducingArrangement, SchedulingFactoryInstance, SchedulingFactoryInstanceDateTimeSlot>>
    prepareConstrainStreamAsTrio(@NonNull ConstraintFactory constraintFactory) {
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
                .map(Trio::new);
    }

    private Constraint forbidBrokenPrerequisiteStock(@NonNull ConstraintFactory constraintFactory) {
        return prepareConstrainStreamAsTrio(constraintFactory)
                .join(
                        SchedulingArrangementHierarchies.class,
                        Joiners.equal(
                                Trio::value0,
                                SchedulingArrangementHierarchies::getWhole
                        )
                )
                .join(
                        prepareConstrainStreamAsTrio(constraintFactory),
                        Joiners.equal(
                                (wholeTrio, hierarchies) -> hierarchies.getPartial(),
                                Trio::value0
                        )
                )
                .groupBy(
                        (trio, hierarchies, trio2) -> trio.value0(),
                        (trio, hierarchies, trio2) -> trio2.value0()
                )
                .groupBy(
                        (whole, partial) -> whole,
                        ConstraintCollectors.max(
                                (whole, partial) -> partial,
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
                .filter(SchedulingProducingArrangement::isOrderDirect)
                .join(
                        SchedulingWorkCalendar.class,
                        Joiners.equal(SchedulingProducingArrangement::getSchedulingWorkCalendar, Function.identity())
                )
                .filter((schedulingProducingArrangement, workCalendar) -> {
                            return schedulingProducingArrangement.getCompletedDateTime()
                                    .isAfter(workCalendar.getEndDateTime());
                        }
                )
                .penalizeLong(
                        BendableLongScore.ofSoft(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.SOFT_TOLERANCE,
                                10L
                        ),
                        (schedulingProducingArrangement, workCalendar) -> {
                            LocalDateTime completedDateTime = schedulingProducingArrangement.getCompletedDateTime();
                            LocalDateTime workCalendarStart = workCalendar.getStartDateTime();
                            LocalDateTime workCalendarEnd = workCalendar.getEndDateTime();
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
        return prepareConstrainStreamAsTrio((constraintFactory))
                .filter(trio -> trio.value0().isOrderDirect())
                .groupBy(
                        trio -> trio.value0().getSchedulingOrder(),
                        Trio::value0
                )
                .groupBy(
                        ((schedulingOrder, schedulingProducingArrangement) -> schedulingOrder),
                        ConstraintCollectors.max(
                                (schedulingOrder, schedulingProducingArrangement) -> schedulingProducingArrangement,
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
                .join(
                        SchedulingWorkCalendar.class,
                        Joiners.equal(SchedulingProducingArrangement::getSchedulingWorkCalendar, Function.identity())
                )
                .penalizeLong(
                        BendableLongScore.ofSoft(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.SOFT_BATTER,
                                1L
                        ), (arrangement, workCalendar) -> {
                            LocalDateTime arrangeDateTime = arrangement.getArrangeDateTime();
                            return Duration.between(workCalendar.getStartDateTime(), arrangeDateTime).toMinutes();
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
                                700L
                        )
                )
                .asConstraint("preferMinimizeProductArrangeDateTimeSlotUsage");
    }

    private TriConstraintStream<SchedulingProducingArrangement, SchedulingFactoryInstance, SchedulingFactoryInstanceDateTimeSlot>
    prepareConstrainStream(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(
                        SchedulingProducingArrangement.class)
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
