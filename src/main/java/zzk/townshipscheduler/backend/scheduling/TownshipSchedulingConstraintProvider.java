package zzk.townshipscheduler.backend.scheduling;


import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactorySlot;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducing;

public class TownshipSchedulingConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[]{
                //hard
                forbidProducingInIrrelevantFactory(constraintFactory),
                forbidProducingWithoutMaterialsInsufficientStock(constraintFactory),
                forbidProducingWithoutSufficientFactorySlot(constraintFactory)
        };
    }

    private Constraint forbidProducingWithoutSufficientFactorySlot(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingFactorySlot.class)
                .penalize(HardMediumSoftLongScore.ofHard(0))
                .asConstraint("forbidProducingWithoutSufficientFactorySlot");
    }

    private Constraint forbidProducingWithoutMaterialsInsufficientStock(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducing.class)
                .filter(schedulingProducing -> schedulingProducing.getWarehouse()
                        .values()
                        .stream()
                        .anyMatch(amount -> amount < 0))
                .penalize(HardMediumSoftLongScore.ofHard(1))
                .asConstraint("forbidProducingWithoutMaterialsInsufficientStock");
    }

    private Constraint forbidProducingInIrrelevantFactory(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingProducing.class)
                .filter(schedulingProducing -> !schedulingProducing.getSchedulingFactorySlot()
                        .getPortfolioProductList()
                        .contains(schedulingProducing.getSchedulingProduct()))
                .penalize(HardMediumSoftLongScore.ofHard(1))
                .asConstraint("forbidProducingInIrrelevantFactory")
                ;
    }

}
