package zzk.townshipscheduler.backend.scheduling.score;


import ai.timefold.solver.core.api.score.HardMediumSoftBigDecimalScore;
import ai.timefold.solver.core.api.score.stream.*;
import ai.timefold.solver.core.api.score.stream.common.LoadBalance;
import org.jspecify.annotations.NonNull;
import zzk.townshipscheduler.backend.OrderType;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInstance;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingOrder;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingWorkCalendar;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

public class TownshipSchedulingConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
        return new Constraint[]{
                penalizeInconsistent(constraintFactory),
                mustSureArrangementAssign(constraintFactory),
                //forbidBadDateTimeSlotAssignInFactorySequences(constraintFactory),
                forbidBrokenFactoryAbility(constraintFactory),
                forbidBrokenPrerequisiteArrangement(constraintFactory),
                shouldNotBrokenDeadlineOrder(constraintFactory),
                shouldNotBrokenCalendarEnd(constraintFactory),
                preferNotArrangeInPlayerSleepTime(constraintFactory),
                preferMinimizeCompletedDateTime(constraintFactory),
                preferArrangeDateTimeAsSoonAsPassible(constraintFactory),
                preferMinimizeProductArrangeDateTimeSlotUsage(constraintFactory),
                preferLoadBalanceArrangementsInFactoryInstance(constraintFactory)
        };
    }

    public Constraint penalizeInconsistent(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUnfiltered(SchedulingProducingArrangement.class)
                .filter(SchedulingProducingArrangement::getShadowVariablesInconsistent)
                .penalize(HardMediumSoftBigDecimalScore.ONE_HARD)
                .asConstraint("penalizeInconsistent");
    }

    private Constraint mustSureArrangementAssign(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEachIncludingUnassigned(SchedulingProducingArrangement.class)
                .filter(Predicate.not(SchedulingProducingArrangement::boolPlanningWellBeing))
                .penalize(HardMediumSoftBigDecimalScore.ONE_HARD)
                .asConstraint("mustSureArrangementAssign");
    }

//    private Constraint forbidBadDateTimeSlotAssignInFactorySequences(@NonNull ConstraintFactory constraintFactory) {
//        return constraintFactory.forEach(SchedulingProducingArrangement.class)
//                .filter(SchedulingProducingArrangement::boolPlanningWellBeing)
//                .join(
//                        constraintFactory.forEach(SchedulingProducingArrangement.class)
//                                .filter(SchedulingProducingArrangement::boolPlanningWellBeing)
//                        , Joiners.equal(SchedulingProducingArrangement::getPlanningFactoryInstance)
//                        , Joiners.equal(
//                                (formerArrangement) -> formerArrangement
//                                , SchedulingProducingArrangement::getPreviousProducingArrangement
//                        )
//                        , Joiners.filtering((formerArrangement, latterArrangement) -> {
//                            SchedulingDateTimeSlot formerArrangementShadowDateTimeSlot = formerArrangement.getShadowDateTimeSlot();
//                            SchedulingDateTimeSlot latterArrangementShadowDateTimeSlot = latterArrangement.getShadowDateTimeSlot();
//                            if (formerArrangementShadowDateTimeSlot != null && latterArrangementShadowDateTimeSlot != null) {
//                                return (latterArrangementShadowDateTimeSlot.compareTo(formerArrangementShadowDateTimeSlot) < 0);
//                            }
//                            else {
//                                return (formerArrangementShadowDateTimeSlot == null && latterArrangementShadowDateTimeSlot != null);
//                            }
//                        })
//                )
//                .penalize(HardMediumSoftBigDecimalScore.ofHard(BigDecimal.valueOf(100)))
//                .asConstraint("forbidBadDateTimeSlotAssignInFactorySequences");
//    }


    private Constraint forbidBrokenFactoryAbility(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .filter(SchedulingProducingArrangement::boolPlanningWellBeing)
                .join(
                        constraintFactory.forEach(SchedulingProducingArrangement.class)
                                .filter(SchedulingProducingArrangement::boolPlanningWellBeing)
                        , Joiners.equal(SchedulingProducingArrangement::getPlanningFactoryInstance)
//                        , Joiners.lessThanOrEqual(SchedulingProducingArrangement::getIndexInFactory)
                )
                .filter((left, right) -> {
                    LocalDateTime rightArrangeDateTime = right.getArrangeDateTime();
                    LocalDateTime rightCompletedDateTime = right.getCompletedDateTime();
                    LocalDateTime leftArrangeDateTime = left.getArrangeDateTime();
                    boolean b1 = !rightArrangeDateTime.isAfter(leftArrangeDateTime);
                    boolean b2 = rightCompletedDateTime.isAfter(leftArrangeDateTime);
                    return b1 && b2;
                })
                .groupBy(
                        (current, other) -> current,
                        ConstraintCollectors.countDistinct((current, other) -> other)
                )
                .filter((current, queueSize) -> queueSize > current.getPlanningFactoryInstance()
                        .getProducingLength()
                )
                .penalize(
                        HardMediumSoftBigDecimalScore.ONE_HARD,
                        (current, queueSize) -> queueSize - current.getPlanningFactoryInstance()
                                .getProducingLength()
                )
                .asConstraint("forbidBrokenFactoryAbility");
    }

    private Constraint forbidBrokenPrerequisiteArrangement(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .filter(SchedulingProducingArrangement::boolPlanningWellBeing)
                .join(
                        constraintFactory.forEach(SchedulingProducingArrangement.class)
                                .filter(SchedulingProducingArrangement::boolPlanningWellBeing),
                        Joiners.equal(
                                Function.identity(),
                                SchedulingProducingArrangement::getSuccessorProducingArrangement
                        )
                )
                .groupBy(
                        (whole, partial) -> whole,
                        ConstraintCollectors.max((whole, partial) -> partial, SchedulingProducingArrangement::getCompletedDateTime)
                )
                .filter((whole, partialMax) -> whole.getArrangeDateTime()
                        .isBefore(partialMax.getCompletedDateTime()))
                .penalize(
                        HardMediumSoftBigDecimalScore.ONE_HARD,
                        (whole, partialMax) -> Duration.between(whole.getArrangeDateTime(), partialMax.getCompletedDateTime())
                                .toMinutes()
                )
                .asConstraint("forbidBrokenPrerequisiteArrangement");
    }

    private Constraint shouldNotBrokenDeadlineOrder(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .filter(SchedulingProducingArrangement::boolPlanningWellBeing)
                .filter(SchedulingProducingArrangement::boolHasDeadline)
                .ifExists(
                        constraintFactory.forEach(SchedulingOrder.class)
                                .filter(SchedulingOrder::boolHasDeadline),
                        Joiners.equal(SchedulingProducingArrangement::getSchedulingOrder, Function.identity()),
                        Joiners.filtering((producingArrangement, schedulingOrder) -> {
                            LocalDateTime deadline = schedulingOrder.getDeadline();
                            LocalDateTime completedDateTime = producingArrangement.getCompletedDateTime();
                            return completedDateTime == null || completedDateTime.isAfter(deadline);
                        })
                )
                .join(SchedulingWorkCalendar.class)
                .penalize(
                        HardMediumSoftBigDecimalScore.ONE_MEDIUM,
                        (schedulingProducingArrangement, schedulingWorkCalendar) -> {
                            LocalDateTime deadline = schedulingProducingArrangement.getDeadline();
                            LocalDateTime completedDateTime = schedulingProducingArrangement.getCompletedDateTime();
                            return completedDateTime != null
                                    ? Duration.between(deadline, completedDateTime)
                                    .toMinutes()
                                    : Duration.between(
                                                    schedulingWorkCalendar.getStartDateTime(), schedulingWorkCalendar.getEndDateTime()
                                            )
                                            .toMinutes();
                        }
                )
                .asConstraint("shouldNotBrokenDeadlineOrder");

//        constraintFactory.forEach(SchedulingOrder.class)
//                .filter(SchedulingOrder::boolHasDeadline)
//                .join(
//                        constraintFactory.forEach(SchedulingProducingArrangement.class)
//                                .filter(SchedulingProducingArrangement::isOrderDirect),
//                        Joiners.equal(Function.identity(), SchedulingProducingArrangement::getSchedulingOrder)
//                )
//                .filter((schedulingOrder, producingArrangement) -> {
//                    LocalDateTime deadline = schedulingOrder.getDeadline();
//                    LocalDateTime completedDateTime = producingArrangement.getCompletedDateTime();
//                    return completedDateTime == null || completedDateTime.isAfter(deadline);
//                })
//                .join(SchedulingWorkCalendar.class)
//                .penalize(
//                        HardMediumSoftBigDecimalScore.ONE_MEDIUM, (
//                                (schedulingOrder, producingArrangement, schedulingWorkCalendar) -> {
//                                    LocalDateTime deadline = schedulingOrder.getDeadline();
//                                    LocalDateTime completedDateTime = producingArrangement.getCompletedDateTime();
//                                    return completedDateTime != null
//                                            ? Duration.between(deadline, completedDateTime)
//                                            .toMinutes()
//                                            : Duration.between(schedulingWorkCalendar.getStartDateTime(), schedulingWorkCalendar
//                                            .getEndDateTime())
//                                                    .toMinutes();
//                                }
//                        )
//                )
//                .asConstraint("shouldNotBrokenDeadlineOrder");
    }

    private Constraint shouldNotBrokenCalendarEnd(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .filter(SchedulingProducingArrangement::boolPlanningWellBeing)
                .filter(SchedulingProducingArrangement::boolOrderDirect)
                .join(SchedulingWorkCalendar.class)
                .filter((schedulingProducingArrangement, schedulingWorkCalendar) -> {
                    LocalDateTime completedDateTime = schedulingProducingArrangement.getCompletedDateTime();
                    return Objects.isNull(completedDateTime) || completedDateTime.isAfter(schedulingWorkCalendar.getEndDateTime());
                })
                .penalize(
                        HardMediumSoftBigDecimalScore.ofMedium(
                                BigDecimal.valueOf(100L)
                        ),
                        (schedulingProducingArrangement, schedulingWorkCalendar) -> {
                            LocalDateTime completedDateTime = schedulingProducingArrangement.getCompletedDateTime();
                            LocalDateTime workCalendarStart = schedulingWorkCalendar.getStartDateTime();
                            LocalDateTime workCalendarEnd = schedulingWorkCalendar.getEndDateTime();
                            if (completedDateTime != null) {
                                return Duration.between(workCalendarEnd, completedDateTime)
                                        .toMinutes();
                            }
                            else {
                                return Duration.between(workCalendarStart, workCalendarEnd)
                                        .toMinutes();
                            }
                        }
                )
                .asConstraint("shouldNotBrokenCalendarEnd");
    }

    private Constraint preferNotArrangeInPlayerSleepTime(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .filter(SchedulingProducingArrangement::boolPlanningWellBeing)
                .filter(schedulingProducingArrangement -> {
                    LocalDateTime arrangeDateTime = schedulingProducingArrangement.getArrangeDateTime();
                    LocalTime sleepStart = schedulingProducingArrangement.getSchedulingPlayer()
                            .getSleepStart();
                    LocalTime sleepEnd = schedulingProducingArrangement.getSchedulingPlayer()
                            .getSleepEnd();
                    LocalTime arrangeTime = arrangeDateTime.toLocalTime();
                    return (arrangeTime.isAfter(sleepStart) && arrangeTime.isBefore(LocalTime.MAX)) || (
                            arrangeTime.isAfter(LocalTime.MIN) && arrangeTime.isBefore(sleepEnd)
                    );
                })
                .penalize(HardMediumSoftBigDecimalScore.ofSoft(BigDecimal.valueOf(10000L)))
                .asConstraint("preferNotArrangeInPlayerSleepTime");
    }

    private Constraint preferMinimizeCompletedDateTime(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .filter(SchedulingProducingArrangement::boolPlanningWellBeing)
                .join(SchedulingWorkCalendar.class)
                .penalize(
                        HardMediumSoftBigDecimalScore.ONE_SOFT,
                        (arrangement, workCalendar) -> {
                            var calendarStartDateTime = workCalendar.getStartDateTime();
                            var completedDateTime = arrangement.getCompletedDateTime();
                            Duration between = Duration.between(calendarStartDateTime, completedDateTime);
                            return calcFactor(arrangement) * between.toMinutes();
                        }
                )
                .asConstraint("preferMinimizeCompletedDateTime");
    }

    private Constraint preferArrangeDateTimeAsSoonAsPassible(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .filter(SchedulingProducingArrangement::boolPlanningWellBeing)
                .join(SchedulingWorkCalendar.class)
                .penalize(
                        HardMediumSoftBigDecimalScore.ONE_SOFT,
                        (arrangement, workCalendar) -> {
                            return Duration.between(workCalendar.getStartDateTime(), arrangement.getArrangeDateTime())
                                    .toMinutes() * calcFactor(arrangement);
                        }
                )
                .asConstraint("preferArrangeDateTimeAsSoonAsPassible");
    }

    private Constraint preferMinimizeProductArrangeDateTimeSlotUsage(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .filter(SchedulingProducingArrangement::boolPlanningWellBeing)
                .groupBy(
                        SchedulingProducingArrangement::getPlanningFactoryInstance,
                        ConstraintCollectors.countDistinct(SchedulingProducingArrangement::getShadowDateTimeSlot)
                )
                .penalize(
                        HardMediumSoftBigDecimalScore.ofSoft(BigDecimal.valueOf(10000L)),
                        (factoryInstance, slotAmount) -> slotAmount - 1
                )
                .asConstraint("preferMinimizeProductArrangeDateTimeSlotUsage");
    }

    private Constraint preferLoadBalanceArrangementsInFactoryInstance(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .groupBy(SchedulingProducingArrangement::getPlanningFactoryInstance, ConstraintCollectors.count())
                .complement(SchedulingFactoryInstance.class, factoryInstance -> 0L)
                .groupBy(ConstraintCollectors.loadBalance(
                        (factoryInstance, arrangementCount) -> factoryInstance,
                        (factoryInstance, arrangementCount) -> arrangementCount
                ))
                .penalizeBigDecimal(HardMediumSoftBigDecimalScore.ONE_SOFT, LoadBalance::unfairness)
                .asConstraint("preferLoadBalanceArrangementsInFactoryInstance");
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

}
