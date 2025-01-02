package zzk.townshipscheduler.backend.scheduling;


import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingGameActionProductProducing;

public class TownshipSchedulingConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[]{
                batterLessProducing(constraintFactory)
                //hard
//                forbidProducingInIrrelevantFactory(constraintFactory),
//                forbidProducingWithoutMaterialsInsufficientStock(constraintFactory),
//                forbidProducingWithoutSufficientFactorySlot(constraintFactory)
        };
    }

    private Constraint batterLessProducing(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingGameActionProductProducing.class)
                .penalize(HardSoftScore.ofHard(1))
                .asConstraint("batterLessProducing");
    }

//    private Constraint forbidProducingWithoutSufficientFactorySlot(ConstraintFactory constraintFactory) {
//        return constraintFactory.forEach(SchedulingPlayerFactory.class)
//                .penalize(HardMediumSoftLongScore.ofHard(0))
//                .asConstraint("forbidProducingWithoutSufficientFactorySlot");
//    }
//
//    private Constraint forbidProducingWithoutMaterialsInsufficientStock(ConstraintFactory constraintFactory) {
//        return constraintFactory.forEach(SchedulingProducing.class)
//                .filter(schedulingProducing -> schedulingProducing.getWarehouse()
//                        .values()
//                        .stream()
//                        .anyMatch(amount -> amount < 0))
//                .penalize(HardMediumSoftLongScore.ofHard(1))
//                .asConstraint("forbidProducingWithoutMaterialsInsufficientStock");
//    }
//
//    private Constraint forbidProducingInIrrelevantFactory(ConstraintFactory constraintFactory) {
//        return constraintFactory.forEach(SchedulingProducing.class)
//                .filter(schedulingProducing -> !schedulingProducing.getSchedulingPlayerFactory()
//                        .getPortfolioProductList()
//                        .contains(schedulingProducing.getSchedulingProduct()))
//                .penalize(HardMediumSoftLongScore.ofHard(1))
//                .asConstraint("forbidProducingInIrrelevantFactory")
//                ;
//    }

}
