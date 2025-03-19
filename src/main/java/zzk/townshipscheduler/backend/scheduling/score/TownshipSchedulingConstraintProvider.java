package zzk.townshipscheduler.backend.scheduling.score;


import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.*;
import org.jspecify.annotations.NonNull;
import zzk.townshipscheduler.backend.scheduling.model.*;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public class TownshipSchedulingConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
        return new Constraint[]{
                forbidSamePlanningSequence(constraintFactory),
                forbidBrokenStock(constraintFactory),
                forbidBrokenFactoryAbility(constraintFactory)
        };
    }

    private Constraint forbidBrokenFactoryAbility(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(AbstractPlayerProducingArrangement.class)
                .join(
                        AbstractFactoryInstance.class,
                        Joiners.equal(AbstractPlayerProducingArrangement::getFactory, Function.identity())
                )
                .groupBy(
                        (arrangement, factory) -> factory,
                        ConstraintCollectors.toList((arrangement, factory) -> arrangement)
                )
                .groupBy(
                        ((factoryInstance, abstractPlayerProducingArrangements) -> factoryInstance),
                        AbstractFactoryInstance::calcFactoryConsequence
                )
                .filter((factoryInstance, consequenceList) -> {
                    boolean boolValue = false;
                    int producingLength = factoryInstance.getProducingLength();
                    List<ActionConsequence> filteredSorted = consequenceList.stream()
                            .filter(consequence -> consequence.getResource().getRoot()==factoryInstance)
                            .filter(consequence -> consequence.getResource() instanceof ActionConsequence.FactoryProducingQueue)
                            .sorted(Comparator.comparing(ActionConsequence::getLocalDateTime))
                            .toList();
                    for (ActionConsequence consequence : filteredSorted) {
                        producingLength = consequence.getResourceChange().apply(producingLength);
                        if (producingLength < 0) {
                            boolValue = true;
                            break;
                        }
                    }
                    return boolValue;
                })
                .penalize(HardSoftScore.ofHard(100))
                .asConstraint("forbidBrokenFactoryAbility");
    }

    private Constraint forbidBrokenStock(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(AbstractPlayerProducingArrangement.class)
                .filter(AbstractPlayerProducingArrangement::boolCompositeProductProducing)
                .ifExists(
                        constraintFactory.forEach(AbstractPlayerProducingArrangement.class)
                                .groupBy(
                                        AbstractPlayerProducingArrangement::getSchedulingPlayer,
                                        ConstraintCollectors.toList()
                                )
                                .groupBy(SchedulingPlayer::mergeToProductAmountMap),
                        Joiners.filtering((producingArrangement, schedulingProductIntegerMap) -> {
                            List<ActionConsequence> actionConsequences
                                    = producingArrangement.calcConsequence()
                                    .stream()
                                    .filter(consequence -> consequence.getResource() instanceof ActionConsequence.ProductStock)
                                    .sorted(Comparator.comparing(ActionConsequence::getLocalDateTime))
                                    .filter(consequence -> !consequence.getLocalDateTime().isAfter(
                                            producingArrangement.getPlanningDateTimeSlotStartAsLocalDateTime()
                                    ))
                                    .toList();

                            for (ActionConsequence consequence : actionConsequences) {
                                SchedulingProduct actionConsequenceProduct
                                        = ((SchedulingProduct) consequence.getResource().getRoot());
                                Integer stock = schedulingProductIntegerMap.getOrDefault(
                                        actionConsequenceProduct, 0
                                );
                                Integer changedStock = consequence.getResourceChange().apply(stock);
                                if (changedStock < 0) {
                                    return true;
                                }
//                                schedulingProductIntegerMap.put(actionConsequenceProduct, changedStock);
                            }
                            return false;
                        })
                )
                .penalize(HardSoftScore.ofHard(100))
                .asConstraint("forbidBrokenStock");
    }

    private Constraint forbidSamePlanningSequence(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(AbstractPlayerProducingArrangement.class)
                .join(
                        AbstractPlayerProducingArrangement.class,
                        Joiners.equal(AbstractPlayerProducingArrangement::getPlanningSequence)
                )
                .penalize(
                        HardSoftScore.ofHard(1)
                )
                .asConstraint("forbidSamePlanningSequence");
    }

}
