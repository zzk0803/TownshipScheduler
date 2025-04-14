package zzk.townshipscheduler.backend.scheduling.score;


import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.core.api.score.stream.*;
import ai.timefold.solver.core.api.score.stream.common.Break;
import ai.timefold.solver.core.api.score.stream.common.ConnectedRange;
import ai.timefold.solver.core.api.score.stream.common.RangeGap;
import ai.timefold.solver.core.api.score.stream.common.Sequence;
import com.nimbusds.jose.shaded.gson.internal.LinkedTreeMap;
import org.jspecify.annotations.NonNull;
import zzk.townshipscheduler.backend.scheduling.model.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.chrono.ChronoLocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TownshipSchedulingConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
        return new Constraint[]{
                forbidMismatchFactory(constraintFactory),
                forbidBrokenFactoryAbility(constraintFactory),
                forbidBrokenPrerequisiteStock(constraintFactory),
                shouldArrangementDateTimeInQueueLegal(constraintFactory),
                shouldNotBrokenDeadlineOrder(constraintFactory),
                shouldNotArrangeInPlayerSleepTime(constraintFactory),
                preferArrangeAsSoonAsPassable(constraintFactory),
                preferMinimizeMakeSpan(constraintFactory)
        };
    }

    private Constraint forbidMismatchFactory(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(BaseSchedulingProducingArrangement.class)
                .filter(producingArrangement -> !producingArrangement.isFactoryMatch())
                .penalize(
                        BendableScore.ofHard(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.HARD_BROKEN_FACTORY,
                                1000
                        )
                )
                .asConstraint("forbidMismatchFactory");
    }

    private Constraint forbidBrokenFactoryAbility(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(BaseSchedulingFactoryInstance.class)
                .join(
                        BaseSchedulingProducingArrangement.class,
                        Joiners.equal(
                                Function.identity(),
                                BaseSchedulingProducingArrangement::getPlanningFactoryInstance
                        )
                )
                .expand((factoryInstance, producingArrangement) -> factoryInstance.remainProducingCapacityAndNextAvailableDuration(
                                producingArrangement.getPlanningDateTimeSlot()
                        )
                )
                .filter((factoryInstance, arrangement, integerDurationPair) -> {
                    return integerDurationPair != null && integerDurationPair.getValue0() < 0;
                })
                .penalize(
                        BendableScore.ofHard(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.HARD_BROKEN_ARRANGEMENT_DATE_TIME,
                                1
                        ),
                        (factoryInstance, arrangement, integerDurationPair) -> {
                            return Math.toIntExact(integerDurationPair.getValue1().toMinutes());
                        }
                )
                .asConstraint("forbidBrokenFactoryAbility");
    }

    private Constraint forbidBrokenPrerequisiteStock(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(BaseSchedulingProducingArrangement.class)
                .filter(producingArrangement -> !producingArrangement.getPrerequisiteProducingArrangements().isEmpty())
                .join(
                        constraintFactory.forEach(BaseSchedulingProducingArrangement.class)
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
                                    .map(BaseSchedulingProducingArrangement::getCompletedDateTime)
                                    .max(LocalDateTime::compareTo)
                                    .orElse(
                                            compositeProducingArrangement.getSchedulingWorkTimeLimit().getEndDateTime()
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
                                TownshipSchedulingProblem.HARD_BROKEN_ARRANGEMENT_DATE_TIME,
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

    private Constraint shouldArrangementDateTimeInQueueLegal(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducingArrangementFactoryTypeQueue.class)
                .filter(schedulingProducingArrangementFactoryTypeQueue -> schedulingProducingArrangementFactoryTypeQueue.getArrangeDateTime() != null)
                .join(
                        constraintFactory.forEach(SchedulingProducingArrangementFactoryTypeQueue.class)
                                .filter(schedulingProducingArrangementFactoryTypeQueue -> schedulingProducingArrangementFactoryTypeQueue.getArrangeDateTime() != null),
                        Joiners.equal(SchedulingProducingArrangementFactoryTypeQueue::getPlanningAnchorFactory),
                        Joiners.filtering((former, latter) -> {
                            boolean adjacent
                                    = former.getNextQueueProducingArrangement() == latter
                                      || latter.getPlanningPreviousProducingArrangementOrFactory() == former;
                            boolean legal
                                    = former.getArrangeDateTime().isBefore(latter.getArrangeDateTime())
                                      || former.getArrangeDateTime().isEqual(latter.getArrangeDateTime());
                            return adjacent && !legal;
                        })
                )
                .penalize(
                        BendableScore.ofHard(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.HARD_QUEUE_DATETIME_SEQUENCE_CHAOS,
                                1
                        ),
                        ((former, latter) -> {
                            LocalDateTime formerArrangeDateTime = former.getArrangeDateTime();
                            LocalDateTime latterArrangeDateTime = latter.getArrangeDateTime();
                            long minutes = Duration.between(latterArrangeDateTime, formerArrangeDateTime).toMinutes();
                            return Math.toIntExact(minutes);
                        })
                )
                .asConstraint("shouldArrangementDateTimeInQueueLegal");
    }

    private Constraint shouldNotBrokenDeadlineOrder(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingOrder.class)
                .filter(SchedulingOrder::boolHasDeadline)
                .join(
                        BaseSchedulingProducingArrangement.class,
                        Joiners.filtering((schedulingOrder, producingArrangement) -> {
                            return producingArrangement.isOrderDirect();
                        })
                )
                .filter((schedulingOrder, producingArrangement) -> {
                    LocalDateTime deadline = schedulingOrder.getDeadline();
                    LocalDateTime completedDateTime = producingArrangement.getCompletedDateTime();
                    return completedDateTime.isAfter(deadline);
                })
                .penalize(
                        BendableScore.ofSoft(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.SOFT_ORDER_DEAD_LINE,
                                1000
                        ),
                        ((schedulingOrder, producingArrangement) -> {
                            LocalDateTime deadline = schedulingOrder.getDeadline();
                            LocalDateTime completedDateTime = producingArrangement.getCompletedDateTime();
                            Duration between = Duration.between(deadline, completedDateTime);
                            return Math.toIntExact(between.toMinutes());
                        })
                )
                .asConstraint("shouldNotBrokenDeadlineOrder");
    }

    private Constraint shouldNotArrangeInPlayerSleepTime(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(BaseSchedulingProducingArrangement.class)
                .filter(producingArrangement -> producingArrangement.getArrangeDateTime() != null)
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
        return constraintFactory.forEach(BaseSchedulingProducingArrangement.class)
                .filter(producingArrangement -> {
                    var planningDateTimeSlot = producingArrangement.getPlanningDateTimeSlot();
                    var producingDateTime = producingArrangement.getProducingDateTime();
                    var completedDateTime = producingArrangement.getCompletedDateTime();
                    return Objects.nonNull(planningDateTimeSlot)
                           && Objects.nonNull(producingDateTime)
                           && Objects.nonNull(completedDateTime);
                })
                .penalize(
                        BendableScore.ofSoft(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.SOFT_BATTER,
                                1
                        ),
                        (arrangement) -> {
                            LocalDateTime startDateTime = arrangement.getSchedulingWorkTimeLimit().getStartDateTime();
                            LocalDateTime arrangementLocalDateTime = arrangement.getArrangeDateTime();
                            return Math.toIntExact(Duration.between(startDateTime, arrangementLocalDateTime)
                                    .toMinutes());
                        }
                )
                .asConstraint("preferArrangeAsSoonAsPassable");
    }

    private Constraint preferMinimizeMakeSpan(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(BaseSchedulingProducingArrangement.class)
                .filter(producingArrangement -> {
                    var planningDateTimeSlot = producingArrangement.getPlanningDateTimeSlot();
                    var producingDateTime = producingArrangement.getProducingDateTime();
                    var completedDateTime = producingArrangement.getCompletedDateTime();
                    return Objects.nonNull(planningDateTimeSlot)
                           && Objects.nonNull(producingDateTime)
                           && Objects.nonNull(completedDateTime);
                })
                .penalize(
                        BendableScore.ofSoft(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.SOFT_BATTER,
                                1
                        ),
                        (arrangement) -> {
                            var startDateTime = arrangement.getSchedulingWorkTimeLimit().getStartDateTime();
                            var arrangementLocalDateTime = arrangement.getCompletedDateTime();
                            return Math.toIntExact(Duration.between(startDateTime, arrangementLocalDateTime)
                                    .toMinutes());
                        }
                )
                .asConstraint("preferMinimizeMakeSpan");
    }

    private Constraint forbidTwistChainWithArrangeDateTimeInQueueProducingArrangement(
            @NonNull ConstraintFactory constraintFactory
    ) {
        constraintFactory.forEach(SchedulingProducingArrangementFactoryTypeQueue.class)
                .filter(schedulingProducingArrangementFactoryTypeQueue -> schedulingProducingArrangementFactoryTypeQueue.getArrangeDateTime() != null)
                .groupBy(
                        SchedulingProducingArrangementFactoryTypeQueue::getPlanningAnchorFactory,
                        ConstraintCollectors.collectAndThen(
                                ConstraintCollectors.toList(),
                                producingArrangements -> {
                                    return producingArrangements.stream()
                                            .collect(
                                                    Collectors.groupingBy(
                                                            SchedulingProducingArrangementFactoryTypeQueue::getArrangeDateTime,
                                                            LinkedTreeMap::new,
                                                            Collectors.toList()
                                                    )
                                            );
                                }
                        )
                )
                .filter((factory, dateTimeArrangementListTreeMap) -> {
                    List<SchedulingProducingArrangementFactoryTypeQueue> producingArrangements = factory.getFlattenProducingArrangements();
                    Set<Map.Entry<LocalDateTime, List<SchedulingProducingArrangementFactoryTypeQueue>>> entries = dateTimeArrangementListTreeMap.entrySet();

                    for (Map.Entry<LocalDateTime, List<SchedulingProducingArrangementFactoryTypeQueue>> entry : entries) {
                        LocalDateTime localDateTime = entry.getKey();
                        List<SchedulingProducingArrangementFactoryTypeQueue> arrangements = entry.getValue();
                    }
                    return false;
                })
                .penalize(BendableScore.zero(
                        TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                        TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE
                ))
                .asConstraint("forbidTwistChainWithArrangeDateTimeInQueueProducingArrangement");
        return null;
    }

    Constraint testConsecutive1(ConstraintFactory constraintFactory) {
        constraintFactory.forEach(BaseSchedulingProducingArrangement.class)
                .groupBy(
                        BaseSchedulingProducingArrangement::getPlanningFactoryInstance,
                        ConstraintCollectors.toConsecutiveSequences(BaseSchedulingProducingArrangement::getId)
                )
                .filter((baseSchedulingFactoryInstance, baseSchedulingProducingArrangementIntegerSequenceChain) -> {
                    //ai.timefold.solver.core.api.score.stream.common.SequenceChain
                    return true;
                })
                .flattenLast(producingArrangementIdSequenceChain -> {
                    Collection<Sequence<BaseSchedulingProducingArrangement, Integer>> consecutiveSequences = producingArrangementIdSequenceChain.getConsecutiveSequences();
                    Collection<Break<BaseSchedulingProducingArrangement, Integer>> breaks = producingArrangementIdSequenceChain.getBreaks();
                    Sequence<BaseSchedulingProducingArrangement, Integer> firstSequence = producingArrangementIdSequenceChain.getFirstSequence();
                    Sequence<BaseSchedulingProducingArrangement, Integer> lastSequence = producingArrangementIdSequenceChain.getLastSequence();
                    Break<BaseSchedulingProducingArrangement, Integer> firstBreak = producingArrangementIdSequenceChain.getFirstBreak();
                    Break<BaseSchedulingProducingArrangement, Integer> lastBreak = producingArrangementIdSequenceChain.getLastBreak();
                    return consecutiveSequences;
                })
                .filter((baseSchedulingFactoryInstance, producingArrangementIdSequence) -> {
                    int count = producingArrangementIdSequence.getCount();
                    Collection<BaseSchedulingProducingArrangement> items = producingArrangementIdSequence.getItems();
                    Integer length = producingArrangementIdSequence.getLength();
                    BaseSchedulingProducingArrangement firstItem = producingArrangementIdSequence.getFirstItem();
                    BaseSchedulingProducingArrangement lastItem = producingArrangementIdSequence.getLastItem();
                    Break<BaseSchedulingProducingArrangement, Integer> nextBreak = producingArrangementIdSequence.getNextBreak();
                    Break<BaseSchedulingProducingArrangement, Integer> previousBreak = producingArrangementIdSequence.getPreviousBreak();
                    return count > baseSchedulingFactoryInstance.getProducingLength();
                })
                .penalize(
                        BendableScore.ofHard(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.HARD_BROKEN_ARRANGEMENT_DATE_TIME,
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
        constraintFactory.forEach(BaseSchedulingProducingArrangement.class)
                .groupBy(
                        BaseSchedulingProducingArrangement::getPlanningFactoryInstance,
                        ConstraintCollectors.toConnectedTemporalRanges(
                                BaseSchedulingProducingArrangement::getArrangeDateTime,
                                BaseSchedulingProducingArrangement::getCompletedDateTime
                        )
                )
                .flattenLast(producingArrangementOccupyLocalDateTimeConnectedRangeChain -> {
                    Iterable<RangeGap<ChronoLocalDateTime<?>, Duration>> gaps = producingArrangementOccupyLocalDateTimeConnectedRangeChain.getGaps();
                    Iterable<ConnectedRange<BaseSchedulingProducingArrangement, ChronoLocalDateTime<?>, Duration>> connectedRanges
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
                    return maximumOverlap > baseSchedulingFactoryInstance.getProducingLength();
                })
                .penalize(
                        BendableScore.ofHard(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.HARD_BROKEN_ARRANGEMENT_DATE_TIME,
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
