package zzk.townshipscheduler.backend.scheduling.score;


import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.core.api.score.stream.*;
import ai.timefold.solver.core.api.score.stream.common.ConnectedRangeChain;
import org.jspecify.annotations.NonNull;
import zzk.townshipscheduler.backend.ProducingStructureType;
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
                shouldNotBrokenDeadlineOrder(constraintFactory),
                shouldNotBrokenCalendarEnd(constraintFactory),
                shouldNotArrangeInPlayerSleepTime(constraintFactory),
                preferMinimizeMakeSpan(constraintFactory),
                preferMinimizeProductArrangeDateTimeSlotUsage(constraintFactory)
        };
    }

    private Constraint forbidBrokenFactoryAbility(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .filter(schedulingProducingArrangement ->
                        Objects.nonNull(schedulingProducingArrangement.getProducingDateTime())
                        && Objects.nonNull(schedulingProducingArrangement.getCompletedDateTime())
                )
                .groupBy(
                        SchedulingProducingArrangement::getPlanningFactoryInstance,
                        ConstraintCollectors.toConnectedTemporalRanges(
                                SchedulingProducingArrangement::getProducingDateTime,
                                SchedulingProducingArrangement::getCompletedDateTime
                        )
                )
                .flattenLast(ConnectedRangeChain::getConnectedRanges)
                .filter((schedulingFactoryInstance, producingArrangementChronoLocalDateTimeDurationConnectedRange) -> {
                    int containedRangeCount = producingArrangementChronoLocalDateTimeDurationConnectedRange.getContainedRangeCount();
                    return containedRangeCount > schedulingFactoryInstance.getProducingLength();
                })
                .penalize(
                        BendableScore.ofHard(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.HARD_BROKEN_FACTORY_ABILITY,
                                1
                        ),
                        (baseSchedulingFactoryInstance, arrangementChronoLocalDateTimeDurationConnectedRange) -> {
                            Duration between = Duration.between(
                                    arrangementChronoLocalDateTimeDurationConnectedRange.getStart(),
                                    arrangementChronoLocalDateTimeDurationConnectedRange.getEnd()
                            );
                            long minutes = between.toMinutes();
                            return Math.toIntExact(minutes);
                        }
                )
                .asConstraint("forbidBrokenFactoryAbility");
    }

    private Constraint forbidBrokenPrerequisiteStock(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .filter(producingArrangement -> !producingArrangement.getPrerequisiteProducingArrangements().isEmpty())
                .join(
                        SchedulingProducingArrangement.class,
                        Joiners.filtering(
                                (compositeProducingArrangement, materialProducingArrangement) -> {
                                    return compositeProducingArrangement.getDeepPrerequisiteProducingArrangements()
                                            .contains(materialProducingArrangement);
                                }
                        )
                )
                .groupBy(
                        (compositeProducingArrangement, materialProducingArrangement) -> compositeProducingArrangement,
                        ConstraintCollectors.toList((compositeProducingArrangement, materialProducingArrangement) -> materialProducingArrangement.getCompletedDateTime())
                )
                .flattenLast(localDateTimes -> localDateTimes)
                .filter((compositeProducingArrangement, materialProducingArrangementsCompletedDateTime) -> {
                    LocalDateTime productArrangeDateTime
                            = compositeProducingArrangement.getArrangeDateTime();

                    return productArrangeDateTime.isBefore(materialProducingArrangementsCompletedDateTime);
                })
                .penalize(
                        BendableScore.ofHard(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.HARD_BROKEN_PRODUCE_PREREQUISITE,
                                1
                        ),
                        ((productArrangement, materialProducingArrangementsCompletedDateTime) -> {
                            return Math.toIntExact(
                                    Duration.between(
                                            productArrangement.getArrangeDateTime(),
                                            materialProducingArrangementsCompletedDateTime
                                    ).toMinutes()
                            );
                        })
                )
                .asConstraint("forbidBrokenPrerequisiteStock");
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
                .penalize(
                        BendableScore.ofSoft(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.SOFT_TOLERANCE,
                                1
                        ),
                        ((schedulingOrder, producingArrangement) -> {
                            LocalDateTime deadline = schedulingOrder.getDeadline();
                            LocalDateTime completedDateTime = producingArrangement.getCompletedDateTime();
                            return completedDateTime == null
                                    ? Math.toIntExact(Duration.between(
                                    producingArrangement.getSchedulingWorkCalendar().getStartDateTime(),
                                    producingArrangement.getSchedulingWorkCalendar().getEndDateTime()
                            ).toMinutes())
                                    : Math.toIntExact(Duration.between(deadline, completedDateTime).toMinutes());
                        })
                )
                .asConstraint("shouldNotBrokenDeadlineOrder");
    }

    private Constraint shouldNotBrokenCalendarEnd(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .filter(schedulingProducingArrangement -> schedulingProducingArrangement.getCompletedDateTime() == null
                                                          || schedulingProducingArrangement.getCompletedDateTime()
                                                                  .isAfter(
                                                                          schedulingProducingArrangement.getSchedulingWorkCalendar()
                                                                                  .getEndDateTime()
                                                                  )
                )
                .penalize(BendableScore.ofSoft(
                        TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                        TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                        TownshipSchedulingProblem.SOFT_TOLERANCE,
                        1000
                ))
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
                    LocalTime localTime = arrangeDateTime.toLocalTime();
                    return localTime.isAfter(sleepStart) && localTime.isBefore(LocalTime.MIDNIGHT)
                           || localTime.isAfter(LocalTime.MIDNIGHT) && localTime.isBefore(sleepEnd);
                })
                .penalize(
                        BendableScore.ofSoft(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.SOFT_TOLERANCE,
                                50
                        )
                )
                .asConstraint("shouldNotArrangeInPlayerSleepTime");
    }

    private Constraint preferMinimizeMakeSpan(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .penalize(
                        BendableScore.ofSoft(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.SOFT_BATTER,
                                1
                        ),
                        (arrangement) -> {
                            var startDateTime = arrangement.getSchedulingWorkCalendar().getStartDateTime();
                            var completedDateTime = arrangement.getCompletedDateTime();
                            Duration between = Duration.between(startDateTime, completedDateTime);
                            return Math.toIntExact(between.toMinutes());
                        }
                )
                .asConstraint("preferMinimizeMakeSpan");
    }

    private Constraint preferMinimizeProductArrangeDateTimeSlotUsage(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .groupBy(
                        SchedulingProducingArrangement::getPlanningFactoryInstance,
                        ConstraintCollectors.countDistinct(SchedulingProducingArrangement::getPlanningDateTimeSlot)
                )
                .penalize(
                        BendableScore.ofSoft(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.SOFT_BATTER,
                                100
                        ),
                        (schedulingFactoryInstance, dataTimeSlotUsage) -> {
                            ProducingStructureType producingStructureType
                                    = schedulingFactoryInstance.getSchedulingFactoryInfo().getProducingStructureType();
                            return producingStructureType == ProducingStructureType.SLOT
                                    ? 7 * dataTimeSlotUsage
                                    : 3 * dataTimeSlotUsage;
                        }
                )
                .asConstraint("preferMinimizeProductArrangeDateTimeSlotUsage");
    }


}
