package zzk.townshipscheduler.backend.scheduling.score;


import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import ai.timefold.solver.core.api.score.stream.*;
import org.jspecify.annotations.NonNull;
import zzk.townshipscheduler.backend.scheduling.model.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

public class TownshipSchedulingConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
        return new Constraint[]{
                forbidMismatchQueueFactory(constraintFactory),
                forbidMismatchSlotFactory(constraintFactory),
//                forbidBrokenQueueFactoryAbility(constraintFactory),
//                forbidBrokenSlotFactoryAbility(constraintFactory),
//                forbidBrokenStock(constraintFactory),
                shouldArrangementClear(constraintFactory),
                preferMinimizeMakeSpan(constraintFactory)
        };
    }

    private Constraint shouldArrangementClear(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(BaseProducingArrangement.class)
                .filter(producingArrangement -> producingArrangement.getProducingDateTime() == null || producingArrangement.getCompletedDateTime() == null)
                .penalize(
                        HardMediumSoftScore.ONE_MEDIUM
                )
                .asConstraint("shouldArrangementClear");
    }

    private Constraint preferMinimizeMakeSpan(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(BaseProducingArrangement.class)
                .filter(producingArrangement -> producingArrangement.getCompletedDateTime() != null)
                .groupBy(
                        BaseProducingArrangement::getSchedulingWorkTimeLimit,
                        ConstraintCollectors.toList(BaseProducingArrangement::getCompletedDateTime)
                )
                .groupBy(
                        (workTimeLimit, localDateTimes) -> workTimeLimit,
                        (workTimeLimit, localDateTimes) -> localDateTimes.stream()
                                .max(LocalDateTime::compareTo)
                                .orElse(workTimeLimit.getEndDateTime())
                )
                .penalize(
                        HardMediumSoftScore.ONE_SOFT,
                        ((workTimeLimit, dateTime) -> {
                            return Math.toIntExact(Duration.between(workTimeLimit.getStartDateTime(), dateTime)
                                    .toMinutes());
                        })
                )
                .asConstraint("preferMinimizeMakeSpan");
    }

    private Constraint forbidMismatchQueueFactory(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingFactoryQueueProducingArrangement.class)
                .filter(schedulingPlayerProducingArrangement -> {
                    SchedulingTypeQueueFactoryInstance planningFactoryInstance = schedulingPlayerProducingArrangement.getPlanningFactoryInstance();
                    return planningFactoryInstance == null
                           || planningFactoryInstance.getSchedulingFactoryInfo() != schedulingPlayerProducingArrangement.requiredFactoryInfo();
                })
                .penalize(HardMediumSoftScore.ofHard(1))
                .asConstraint("forbidMismatchQueueFactory");
    }

    private Constraint forbidMismatchSlotFactory(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingFactorySlotProducingArrangement.class)
                .filter(schedulingPlayerProducingArrangement -> {
                    SchedulingTypeSlotFactoryInstance planningFactoryInstance = schedulingPlayerProducingArrangement.getPlanningFactoryInstance();
                    return planningFactoryInstance == null
                           || planningFactoryInstance.getSchedulingFactoryInfo() != schedulingPlayerProducingArrangement.requiredFactoryInfo();
                })
                .penalize(HardMediumSoftScore.ofHard(1))
                .asConstraint("forbidMismatchSlotFactory");
    }

    private Constraint forbidBrokenQueueFactoryAbility(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingFactoryQueueProducingArrangement.class)
                .groupBy(
                        SchedulingFactoryQueueProducingArrangement::getPlanningFactory,
                        ConstraintCollectors.toList()
                )
                .groupBy(
                        (factoryInstance, slotProducingArrangements) -> factoryInstance,
                        (factoryInstance, slotProducingArrangements) -> slotProducingArrangements.stream()
                                .map(SchedulingFactoryQueueProducingArrangement::calcConsequence)
                                .flatMap(Collection::stream)
                                .toList()
                )
                .complement(SchedulingTypeQueueFactoryInstance.class, (factoryInstance -> List.of()))
                .filter((factoryInstance, consequences) -> {
                    int remain = factoryInstance.getProducingLength();
                    var resourceChanges = consequences.stream()
                            .filter(consequence -> consequence.getResource().getRoot() == factoryInstance)
                            .filter(consequence -> consequence.getResource() instanceof ActionConsequence.FactoryProducingLength)
                            .sorted()
                            .map(ActionConsequence::getResourceChange)
                            .toList();
                    for (ActionConsequence.SchedulingResourceChange resourceChange : resourceChanges) {
                        remain = resourceChange.apply(remain);
                        if (remain < 0) {
                            return true;
                        }
                    }
                    return false;
                })
                .penalize(HardMediumSoftScore.ofHard(1))
                .asConstraint("forbidBrokenQueueFactoryAbility");
    }

    private Constraint forbidBrokenSlotFactoryAbility(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingFactorySlotProducingArrangement.class)
                .groupBy(
                        SchedulingFactorySlotProducingArrangement::getPlanningFactory,
                        ConstraintCollectors.toList()
                )
                .groupBy(
                        (factoryInstance, slotProducingArrangements) -> factoryInstance,
                        (factoryInstance, slotProducingArrangements) -> slotProducingArrangements.stream()
                                .map(SchedulingFactorySlotProducingArrangement::calcConsequence)
                                .flatMap(Collection::stream)
                                .toList()
                )
                .complement(SchedulingTypeSlotFactoryInstance.class, (factoryInstance -> List.of()))
                .filter((factoryInstance, consequences) -> {
                    int remain = factoryInstance.getProducingLength();
                    var resourceChanges = consequences.stream()
                            .filter(consequence -> consequence.getResource().getRoot() == factoryInstance)
                            .filter(consequence -> consequence.getResource() instanceof ActionConsequence.FactoryProducingLength)
                            .sorted()
                            .map(ActionConsequence::getResourceChange)
                            .toList();
                    for (ActionConsequence.SchedulingResourceChange resourceChange : resourceChanges) {
                        remain = resourceChange.apply(remain);
                        if (remain < 0) {
                            return true;
                        }
                    }
                    return false;
                })
                .penalize(HardMediumSoftScore.ofHard(1))
                .asConstraint("forbidBrokenSlotFactoryAbility");
    }

    private Constraint forbidBrokenStock(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(BaseProducingArrangement.class)
                .join(
                        SchedulingPlayer.class,
                        Joiners.equal(BaseProducingArrangement::getSchedulingPlayer, Function.identity())
                )
                .expand((producingArrangement, schedulingPlayer) -> producingArrangement.calcConsequence())
                .groupBy(
                        (producingArrangement, schedulingPlayer, actionConsequences) -> schedulingPlayer,
                        ConstraintCollectors.collectAndThen(
                                ConstraintCollectors.toList(
                                        (producingArrangement, schedulingPlayer, actionConsequences) -> actionConsequences
                                ),
                                listList -> listList.stream()
                                        .flatMap(Collection::stream)
                                        .sorted()
                                        .filter(consequence -> consequence.getResource() instanceof ActionConsequence.ProductStock)
                                        .toList()
                        )
                )
                .filter((player, actionConsequences) -> {
                    HashMap<SchedulingProduct, Integer> stockMap = new HashMap<>(player.getProductAmountMap());
                    for (ActionConsequence consequence : actionConsequences) {
                        SchedulingProduct actionConsequenceProduct
                                = ((SchedulingProduct) consequence.getResource().getRoot());
                        Integer stock = stockMap.getOrDefault(
                                actionConsequenceProduct, 0
                        );
                        Integer changedStock = consequence.getResourceChange().apply(stock);
                        if (changedStock < 0) {
                            return true;
                        }
                    }
                    return false;
                })
                .penalize(HardMediumSoftScore.ofHard(1))
                .asConstraint("forbidBrokenStock");
    }

}
