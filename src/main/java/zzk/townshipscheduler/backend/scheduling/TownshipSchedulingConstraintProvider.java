package zzk.townshipscheduler.backend.scheduling;


import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import zzk.townshipscheduler.backend.scheduling.model.*;

public class TownshipSchedulingConstraintProvider implements ConstraintProvider {

    public static final int BENDABLE_SCORE_HARD_SIZE = 2;

    public static final int BENDABLE_SCORE_SOFT_SIZE = 2;

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[]{
                forbidMismatchProducingWorkplace(constraintFactory),
                forbidMismatchExecutionMode(constraintFactory),
        };
    }
    private Constraint forbidMismatchProducingWorkplace(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingPlayerFactoryAction.class)
                .filter(schedulingPlayerFactoryAction -> {
                    SchedulingProduct schedulingProduct = schedulingPlayerFactoryAction.getSchedulingProduct();
                    SchedulingFactoryInfo productRequireFactoryInfo = schedulingProduct.getRequireFactory();
                    SchedulingFactoryInstance planningFactoryInstance = schedulingPlayerFactoryAction.getPlanningFactory();
                    SchedulingFactoryInfo factoryInstanceFactoryInfo = planningFactoryInstance.getSchedulingFactoryInfo();
                    return !factoryInstanceFactoryInfo.equals(productRequireFactoryInfo);
                }).penalize(
                        BendableScore.ofHard(
                                BENDABLE_SCORE_HARD_SIZE,
                                BENDABLE_SCORE_SOFT_SIZE,
                                0,
                                1
                        )
                ).asConstraint("forbidMismatchProducingWorkplace");
    }

    private Constraint forbidMismatchExecutionMode(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SchedulingPlayerFactoryAction.class)
                .join(
                        SchedulingGameActionExecutionMode.class,
                        Joiners.filtering(
                                (producing, executionMode) -> {
                                    SchedulingProduct schedulingProduct = producing.getSchedulingProduct();
                                    SchedulingGameActionExecutionMode planningProducingExecutionMode = producing.getPlanningProducingExecutionMode();
                                    return executionMode.equals(planningProducingExecutionMode)
                                           && !schedulingProduct.getProducingExecutionModeSet()
                                            .contains(planningProducingExecutionMode);
                                }
                        )
                ).penalize(
                        BendableScore.ofHard(
                                BENDABLE_SCORE_HARD_SIZE,
                                BENDABLE_SCORE_SOFT_SIZE,
                                0,
                                1
                        )
                ).asConstraint("forbidMismatchExecutionMode");
    }

}
