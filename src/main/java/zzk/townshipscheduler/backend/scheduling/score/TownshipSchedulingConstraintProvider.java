package zzk.townshipscheduler.backend.scheduling.score;


import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.core.api.score.stream.*;
import ai.timefold.solver.core.api.score.stream.common.*;
import org.jspecify.annotations.NonNull;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingOrder;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.chrono.ChronoLocalDateTime;
import java.util.Collection;
import java.util.Objects;

public class TownshipSchedulingConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
        return new Constraint[]{
                forbidBrokenFactoryAbility(constraintFactory),
                forbidBrokenPrerequisiteStock(constraintFactory),
                shouldNotBrokenDeadlineOrder(constraintFactory),
                shouldNotArrangeInPlayerSleepTime(constraintFactory),
                preferArrangeAsSoonAsPassable(constraintFactory)
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
                    Duration length = producingArrangementChronoLocalDateTimeDurationConnectedRange.getLength();
                    ChronoLocalDateTime<?> start = producingArrangementChronoLocalDateTimeDurationConnectedRange.getStart();
                    ChronoLocalDateTime<?> end = producingArrangementChronoLocalDateTimeDurationConnectedRange.getEnd();
                    int minimumOverlap = producingArrangementChronoLocalDateTimeDurationConnectedRange.getMinimumOverlap();
                    int maximumOverlap = producingArrangementChronoLocalDateTimeDurationConnectedRange.getMaximumOverlap();
                    int containedRangeCount = producingArrangementChronoLocalDateTimeDurationConnectedRange.getContainedRangeCount();
                    boolean hasOverlap = producingArrangementChronoLocalDateTimeDurationConnectedRange.hasOverlap();
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
                        constraintFactory.forEach(SchedulingProducingArrangement.class)
                                .filter(producingArrangement -> producingArrangement.getCompletedDateTime() != null)
                        ,
                        Joiners.filtering(
                                (compositeProducingArrangement, materialProducingArrangement) -> {
                                    return compositeProducingArrangement.getDeepPrerequisiteProducingArrangements()
                                            .contains(materialProducingArrangement);
                                }
                        )
                )
                .groupBy(
                        (compositeProducingArrangement, materialProducingArrangement) -> compositeProducingArrangement,
                        ConstraintCollectors.toList((compositeProducingArrangement, materialProducingArrangement) -> materialProducingArrangement)
                )
                .groupBy(
                        (compositeProducingArrangement, materialProducingArrangements) -> compositeProducingArrangement,
                        (compositeProducingArrangement, materialProducingArrangements) -> {
                            return materialProducingArrangements.stream()
                                    .map(SchedulingProducingArrangement::getCompletedDateTime)
                                    .max(LocalDateTime::compareTo)
                                    .orElse(
                                            compositeProducingArrangement.getSchedulingWorkCalendar().getEndDateTime()
                                    );
                        }
                )
                .filter((compositeProducingArrangement, materialProducingArrangementsCompletedDateTime) -> {
                    LocalDateTime productArrangeDateTime
                            = compositeProducingArrangement.getArrangeDateTime();

                    return (productArrangeDateTime.isBefore(materialProducingArrangementsCompletedDateTime)
                            || productArrangeDateTime.isEqual(materialProducingArrangementsCompletedDateTime));
                })
                .penalize(
                        BendableScore.ofHard(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.HARD_BROKEN_PRODUCE_PREREQUISITE,
                                5
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
                        SchedulingProducingArrangement.class,
                        Joiners.filtering((schedulingOrder, producingArrangement) -> {
                            return producingArrangement.isOrderDirect();
                        })
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
                                1000
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

    private Constraint shouldNotArrangeInPlayerSleepTime(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .filter(SchedulingProducingArrangement::isPlanningAssigned)
                .filter(producingArrangement -> {
                    LocalDateTime arrangeDateTime = producingArrangement.getArrangeDateTime();
                    LocalTime localTime = arrangeDateTime.toLocalTime();
                    return localTime.isAfter(
                            producingArrangement.getSchedulingPlayer().getSleepStart()
                    ) || localTime.isBefore(
                            producingArrangement.getSchedulingPlayer().getSleepEnd()
                    );
                })
                .penalize(
                        BendableScore.ofSoft(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.SOFT_BATTER,
                                1000
                        )
                )
                .asConstraint("shouldNotArrangeInPlayerSleepTime");
    }

    private Constraint preferArrangeAsSoonAsPassable(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .filter(SchedulingProducingArrangement::isPlanningAssigned)
                .penalize(
                        BendableScore.ofSoft(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.SOFT_BATTER,
                                1
                        ),
                        (arrangement) -> {
                            LocalDateTime startDateTime = arrangement.getSchedulingWorkCalendar().getStartDateTime();
                            LocalDateTime arrangementLocalDateTime = arrangement.getArrangeDateTime();
                            return Math.toIntExact(Duration.between(startDateTime, arrangementLocalDateTime)
                                    .toMinutes());
                        }
                )
                .asConstraint("preferArrangeAsSoonAsPassable");
    }

    private Constraint preferMinimizeMakeSpan(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangement.class)
                .filter(SchedulingProducingArrangement::isPlanningAssigned)
                .penalize(
                        BendableScore.ofSoft(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.SOFT_BATTER,
                                1
                        ),
                        (arrangement) -> {
                            var startDateTime = arrangement.getSchedulingWorkCalendar().getStartDateTime();
                            var endDateTime = arrangement.getSchedulingWorkCalendar().getEndDateTime();
                            var completedDateTime = arrangement.getCompletedDateTime();
                            return Math.toIntExact(Duration.between(
                                            startDateTime,
                                            completedDateTime == null ? endDateTime : startDateTime
                                    )
                                    .toMinutes());
                        }
                )
                .asConstraint("preferMinimizeMakeSpan");
    }


    Constraint testConsecutive1(ConstraintFactory constraintFactory) {
        constraintFactory.forEach(SchedulingProducingArrangement.class)
                .groupBy(
                        SchedulingProducingArrangement::getPlanningFactoryInstance,
                        ConstraintCollectors.toConsecutiveSequences(SchedulingProducingArrangement::getId)
                )
                .filter((baseSchedulingFactoryInstance, baseSchedulingProducingArrangementIntegerSequenceChain) -> {
                    //ai.timefold.solver.core.api.score.stream.common.SequenceChain
                    return true;
                })
                .flattenLast(producingArrangementIdSequenceChain -> {
                    Collection<Sequence<SchedulingProducingArrangement, Integer>> consecutiveSequences = producingArrangementIdSequenceChain.getConsecutiveSequences();
                    Collection<Break<SchedulingProducingArrangement, Integer>> breaks = producingArrangementIdSequenceChain.getBreaks();
                    Sequence<SchedulingProducingArrangement, Integer> firstSequence = producingArrangementIdSequenceChain.getFirstSequence();
                    Sequence<SchedulingProducingArrangement, Integer> lastSequence = producingArrangementIdSequenceChain.getLastSequence();
                    Break<SchedulingProducingArrangement, Integer> firstBreak = producingArrangementIdSequenceChain.getFirstBreak();
                    Break<SchedulingProducingArrangement, Integer> lastBreak = producingArrangementIdSequenceChain.getLastBreak();
                    return consecutiveSequences;
                })
                .filter((baseSchedulingFactoryInstance, producingArrangementIdSequence) -> {
                    int count = producingArrangementIdSequence.getCount();
                    Collection<SchedulingProducingArrangement> items = producingArrangementIdSequence.getItems();
                    Integer length = producingArrangementIdSequence.getLength();
                    SchedulingProducingArrangement firstItem = producingArrangementIdSequence.getFirstItem();
                    SchedulingProducingArrangement lastItem = producingArrangementIdSequence.getLastItem();
                    Break<SchedulingProducingArrangement, Integer> nextBreak = producingArrangementIdSequence.getNextBreak();
                    Break<SchedulingProducingArrangement, Integer> previousBreak = producingArrangementIdSequence.getPreviousBreak();
                    return count > baseSchedulingFactoryInstance.getProducingLength();
                })
                .penalize(
                        BendableScore.ofHard(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.HARD_BROKEN_PRODUCE_PREREQUISITE,
                                1
                        ),
                        (baseSchedulingFactoryInstance, producingArrangementIdSequence) -> {
                            return 1;
                        }
                )
                .asConstraint("testConsecutive");
        return null;
    }

    Constraint testConsecutive2(ConstraintFactory constraintFactory) {
        constraintFactory.forEach(SchedulingProducingArrangement.class)
                .groupBy(
                        SchedulingProducingArrangement::getPlanningFactoryInstance,
                        ConstraintCollectors.toConnectedTemporalRanges(
                                SchedulingProducingArrangement::getArrangeDateTime,
                                SchedulingProducingArrangement::getCompletedDateTime
                        )
                )
                .flattenLast(producingArrangementOccupyLocalDateTimeConnectedRangeChain -> {
                    Iterable<RangeGap<ChronoLocalDateTime<?>, Duration>> gaps = producingArrangementOccupyLocalDateTimeConnectedRangeChain.getGaps();
                    Iterable<ConnectedRange<SchedulingProducingArrangement, ChronoLocalDateTime<?>, Duration>> connectedRanges
                            = producingArrangementOccupyLocalDateTimeConnectedRangeChain.getConnectedRanges();
                    return connectedRanges;
                })
                .filter((baseSchedulingFactoryInstance, baseSchedulingProducingArrangements) -> {
                    Duration length = baseSchedulingProducingArrangements.getLength();
                    ChronoLocalDateTime<?> start = baseSchedulingProducingArrangements.getStart();
                    ChronoLocalDateTime<?> end = baseSchedulingProducingArrangements.getEnd();
                    int minimumOverlap = baseSchedulingProducingArrangements.getMinimumOverlap();
                    int maximumOverlap = baseSchedulingProducingArrangements.getMaximumOverlap();
                    int containedRangeCount = baseSchedulingProducingArrangements.getContainedRangeCount();
                    boolean hasOverlap = baseSchedulingProducingArrangements.hasOverlap();
                    return containedRangeCount > baseSchedulingFactoryInstance.getProducingLength();
                })
                .penalize(
                        BendableScore.ofHard(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.HARD_BROKEN_PRODUCE_PREREQUISITE,
                                1
                        ),
                        (baseSchedulingFactoryInstance, producingArrangementIdSequence) -> {
                            return 1;
                        }
                )
                .asConstraint("testConsecutive2");
        return null;
    }

}
