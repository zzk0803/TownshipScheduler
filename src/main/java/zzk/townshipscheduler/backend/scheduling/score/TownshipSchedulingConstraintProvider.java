package zzk.townshipscheduler.backend.scheduling.score;


import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import org.jspecify.annotations.NonNull;
import zzk.townshipscheduler.backend.scheduling.model.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

public class TownshipSchedulingConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
        return new Constraint[]{
                forbidMismatchQueueFactory(constraintFactory),
                forbidMismatchSlotFactory(constraintFactory),
                forbidBrokenQueueFactoryAbility(constraintFactory),
                forbidBrokenSlotFactoryAbility(constraintFactory),
//                forbidBrokenStock(constraintFactory),
                forbidBrokenPrerequisite(constraintFactory),
                shouldEveryArrangementAssigned(constraintFactory),
                shouldNotBrokenDeadlineOrder(constraintFactory),
                shouldNotArrangeInPlayerSleepTime(constraintFactory),
//                preferMinimizeMakeSpan(constraintFactory),
                preferArrangeAsSoonAsPassable(constraintFactory)
        };
    }

    private Constraint forbidMismatchQueueFactory(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingFactoryQueueProducingArrangement.class)
                .filter(schedulingPlayerProducingArrangement -> {
                    SchedulingTypeQueueFactoryInstance planningFactoryInstance = schedulingPlayerProducingArrangement.getPlanningFactoryInstance();
                    return planningFactoryInstance == null
                           || planningFactoryInstance.getSchedulingFactoryInfo() != schedulingPlayerProducingArrangement.requiredFactoryInfo();
                })
                .penalize(
                        BendableScore.ofHard(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.HARD_BROKEN_FACTORY,
                                500
                        )
                )
                .asConstraint("forbidMismatchQueueFactory");
    }

    private Constraint forbidMismatchSlotFactory(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingFactorySlotProducingArrangement.class)
                .filter(schedulingPlayerProducingArrangement -> {
                    SchedulingTypeSlotFactoryInstance planningFactoryInstance = schedulingPlayerProducingArrangement.getPlanningFactoryInstance();
                    return planningFactoryInstance == null
                           || planningFactoryInstance.getSchedulingFactoryInfo() != schedulingPlayerProducingArrangement.requiredFactoryInfo();
                })
                .penalize(
                        BendableScore.ofHard(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.HARD_BROKEN_FACTORY,
                                500
                        )
                )
                .asConstraint("forbidMismatchSlotFactory");
    }

    private Constraint forbidBrokenQueueFactoryAbility(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingTypeQueueFactoryInstance.class)
                .filter(SchedulingTypeQueueFactoryInstance::remainProducingLengthHadIllegal)
                .penalize(
                        BendableScore.ofHard(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.HARD_ASSIGN,
                                100
                        )
                )
                .asConstraint("forbidBrokenQueueFactoryAbility");
    }

    private Constraint forbidBrokenSlotFactoryAbility(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingTypeSlotFactoryInstance.class)
                .filter(SchedulingTypeSlotFactoryInstance::remainProducingLengthHadIllegal)
                .penalize(
                        BendableScore.ofHard(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.HARD_BROKEN_FACTORY,
                                100
                        )
                )
                .asConstraint("forbidBrokenSlotFactoryAbility");
    }

    private Constraint forbidBrokenPrerequisite(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingFactoryQueueProducingArrangement.class)
                .filter(queueProducingArrangement
                        ->
                        !queueProducingArrangement.getPrerequisiteProducingArrangements().isEmpty()
                )
                .join(
                        BaseProducingArrangement.class,
                        Joiners.filtering(
                                (compositeProducingArrangement, producingArrangement) ->
                                        compositeProducingArrangement.getPrerequisiteProducingArrangements()
                                                .contains(producingArrangement)
                        )
                )
                .filter((productArrangement, materialArrangement) -> {
                    if (!productArrangement.getPrerequisiteProducingArrangements().contains(materialArrangement)) {
                        return false;
                    }

                    LocalDateTime productArrangeDateTime
                            = productArrangement.getPlanningDateTimeSlotStartAsLocalDateTime();
                    LocalDateTime materialCompletedDateTime
                            = materialArrangement.getCompletedDateTime();
                    if (productArrangeDateTime == null
                        || materialCompletedDateTime == null
                    ) {
                        return false;
                    }

                    return productArrangeDateTime.isBefore(materialCompletedDateTime);
                })
                .penalize(
                        BendableScore.ofHard(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.HARD_ASSIGN,
                                10
                        ),
                        ((productArrangement, materialArrangement) -> {
                            return Math.toIntExact(
                                    Duration.between(
                                            productArrangement.getPlanningDateTimeSlotStartAsLocalDateTime(),
                                            materialArrangement.getCompletedDateTime()
                                    ).toMinutes()
                            );
                        })
                )
                .asConstraint("forbidBrokenPrerequisite");
    }

    private Constraint shouldEveryArrangementAssigned(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(BaseProducingArrangement.class)
                .filter(producingArrangement -> producingArrangement.getProducingDateTime() == null || producingArrangement.getCompletedDateTime() == null)
                .penalize(
                        BendableScore.ofHard(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.SOFT_AS_MIDDLE_ASSIGN,
                                10
                        )
                )
                .asConstraint("shouldArrangementClear");
    }

    private Constraint shouldNotBrokenDeadlineOrder(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingOrder.class)
                .filter(SchedulingOrder::boolHasDeadline)
                .join(
                        BaseProducingArrangement.class,
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
                        BendableScore.ofHard(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.SOFT_AS_MIDDLE_ASSIGN,
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
        return constraintFactory.forEach(BaseProducingArrangement.class)
                .filter(producingArrangement -> producingArrangement.getPlanningDateTimeSlot() != null)
                .filter(producingArrangement -> {
                    LocalDateTime arrangeDateTime = producingArrangement.getPlanningDateTimeSlotStartAsLocalDateTime();
                    LocalTime localTime = arrangeDateTime.toLocalTime();
                    return localTime.isAfter(
                            producingArrangement.getSchedulingPlayer().getSleepStart()
                    ) && localTime.isBefore(
                            producingArrangement.getSchedulingPlayer().getSleepEnd()
                    );
                })
                .penalize(
                        BendableScore.ofHard(
                                TownshipSchedulingProblem.BENDABLE_SCORE_HARD_SIZE,
                                TownshipSchedulingProblem.BENDABLE_SCORE_SOFT_SIZE,
                                TownshipSchedulingProblem.HARD_ASSIGN,
                                100
                        )
                )
                .asConstraint("shouldNotArrangeInPlayerSleepTime");
    }

//    private Constraint preferMinimizeMakeSpan(@NonNull ConstraintFactory constraintFactory) {
//        return constraintFactory.forEach(BaseProducingArrangement.class)
//                .filter(producingArrangement -> producingArrangement.getCompletedDateTime() != null)
//                .groupBy(
//                        BaseProducingArrangement::getSchedulingWorkTimeLimit,
//                        ConstraintCollectors.toList(BaseProducingArrangement::getCompletedDateTime)
//                )
//                .groupBy(
//                        (workTimeLimit, localDateTimes) -> workTimeLimit,
//                        (workTimeLimit, localDateTimes) -> localDateTimes.stream()
//                                .max(LocalDateTime::compareTo)
//                                .orElse(workTimeLimit.getEndDateTime())
//                )
//                .penalize(
//                        HardMediumSoftScore.ONE_SOFT,
//                        ((workTimeLimit, dateTime) -> {
//                            return Math.toIntExact(Duration.between(workTimeLimit.getStartDateTime(), dateTime)
//                                    .toMinutes());
//                        })
//                )
//                .asConstraint("preferMinimizeMakeSpan");
//    }

    private Constraint preferArrangeAsSoonAsPassable(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(BaseProducingArrangement.class)
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
                            LocalDateTime arrangementLocalDateTime = arrangement.getPlanningDateTimeSlotStartAsLocalDateTime();
                            return Math.toIntExact(Duration.between(startDateTime, arrangementLocalDateTime)
                                    .toMinutes());
                        }
                )
                .asConstraint("preferArrangeAsSoonAsPassable");
    }

//    private Constraint forbidBrokenStock(@NonNull ConstraintFactory constraintFactory) {
//         constraintFactory.forEach(BaseProducingArrangement.class)
//                .groupBy(
//                        BaseProducingArrangement::getSchedulingPlayer,
//                        ConstraintCollectors.toList()
//                )
//                .filter(SchedulingPlayer::remainStockHadIllegal)
//                .penalize(HardMediumSoftScore.ofHard(1))
//                .asConstraint("forbidBrokenStock");
//    }

}
