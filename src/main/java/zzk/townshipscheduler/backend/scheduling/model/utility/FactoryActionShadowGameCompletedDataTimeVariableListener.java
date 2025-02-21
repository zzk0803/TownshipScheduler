//package zzk.townshipscheduler.backend.scheduling.model.utility;
//
//import ai.timefold.solver.core.api.domain.variable.VariableListener;
//import ai.timefold.solver.core.api.score.director.ScoreDirector;
//import lombok.extern.slf4j.Slf4j;
//import org.jspecify.annotations.NonNull;
//import zzk.townshipscheduler.backend.scheduling.model.SchedulingPeriodFactory;
//import zzk.townshipscheduler.backend.scheduling.model.SchedulingPlayerFactoryAction;
//import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingExecutionMode;
//import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;
//
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.List;
//
//@Slf4j
//public class FactoryActionShadowGameCompletedDataTimeVariableListener
//        implements VariableListener<TownshipSchedulingProblem, SchedulingPlayerFactoryAction> {
//
//    @Override
//    public void beforeVariableChanged(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull SchedulingPlayerFactoryAction factoryOrAction
//    ) {
//
//    }
//
//    @Override
//    public void afterVariableChanged(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull SchedulingPlayerFactoryAction factoryOrAction
//    ) {
//        doUpdateShadowVariable(scoreDirector, factoryOrAction);
//    }
//
//    private void doUpdateShadowVariable(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull SchedulingPlayerFactoryAction schedulingPlayerFactoryAction
//    ) {
//
//        SchedulingProducingExecutionMode planningProducingExecutionMode = schedulingPlayerFactoryAction.getProducingExecutionMode();
//        LocalDateTime shadowGameProducingDataTime = schedulingPlayerFactoryAction.getShadowGameProducingDataTime();
//        if (planningProducingExecutionMode == null) {
//            scoreDirector.beforeVariableChanged(schedulingPlayerFactoryAction, "shadowGameCompleteDateTime");
//            schedulingPlayerFactoryAction.setShadowGameCompleteDateTime(null);
//            scoreDirector.afterVariableChanged(schedulingPlayerFactoryAction, "shadowGameCompleteDateTime");
//        } else {
//            if (shadowGameProducingDataTime == null) {
//                scoreDirector.beforeVariableChanged(schedulingPlayerFactoryAction, "shadowGameCompleteDateTime");
//                schedulingPlayerFactoryAction.setShadowGameCompleteDateTime(null);
//                scoreDirector.afterVariableChanged(schedulingPlayerFactoryAction, "shadowGameCompleteDateTime");
//            } else {
//                scoreDirector.beforeVariableChanged(schedulingPlayerFactoryAction, "shadowGameCompleteDateTime");
//                schedulingPlayerFactoryAction.setShadowGameCompleteDateTime(
//                        shadowGameProducingDataTime.plus(planningProducingExecutionMode.getExecuteDuration())
//                );
//                scoreDirector.afterVariableChanged(schedulingPlayerFactoryAction, "shadowGameCompleteDateTime");
//
//                List<SchedulingPeriodFactory> affectFactories = new ArrayList<>();
//                SchedulingPeriodFactory planningFactory = schedulingPlayerFactoryAction.getSchedulingPeriodFactory();
//                while (planningFactory.getNextPeriodOfFactory() != null && planningFactory.boolAffectByAction(schedulingPlayerFactoryAction) ) {
//                    affectFactories.add(planningFactory.getNextPeriodOfFactory());
//                    planningFactory = planningFactory.getNextPeriodOfFactory();
//                }
//                schedulingPlayerFactoryAction.setAffectFollowingFactories(affectFactories);
//            }
//        }
//
//    }
//
//    @Override
//    public void beforeEntityAdded(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull SchedulingPlayerFactoryAction factoryOrAction
//    ) {
//
//    }
//
//    @Override
//    public void afterEntityAdded(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull SchedulingPlayerFactoryAction factoryOrAction
//    ) {
//        doUpdateShadowVariable(scoreDirector, factoryOrAction);
//    }
//
//    @Override
//    public void beforeEntityRemoved(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull SchedulingPlayerFactoryAction factoryOrAction
//    ) {
//
//    }
//
//    @Override
//    public void afterEntityRemoved(
//            @NonNull ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            @NonNull SchedulingPlayerFactoryAction factoryOrAction
//    ) {
//
//    }
//
//}
