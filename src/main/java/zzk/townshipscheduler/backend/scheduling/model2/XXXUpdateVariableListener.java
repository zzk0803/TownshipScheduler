//package zzk.townshipscheduler.backend.scheduling.model2;
//
//import ai.timefold.solver.core.api.domain.variable.VariableListener;
//import ai.timefold.solver.core.api.score.director.ScoreDirector;
//import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;
//
//import java.time.LocalDateTime;
//
//public class XXXUpdateVariableListener
//        implements VariableListener<TownshipSchedulingProblem, SchedulingProducing> {
//
//    @Override
//    public void beforeVariableChanged(
//            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            SchedulingProducing schedulingProducing
//    ) {
//
//    }
//
//    @Override
//    public void afterVariableChanged(
//            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            SchedulingProducing schedulingProducing
//    ) {
//
//    }
//
//    @Override
//    public void beforeEntityAdded(
//            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            SchedulingProducing schedulingProducing
//    ) {
//
//    }
//
//    @Override
//    public void afterEntityAdded(
//            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            SchedulingProducing schedulingProducing
//    ) {
//
//    }
//
//    @Override
//    public void beforeEntityRemoved(
//            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            SchedulingProducing schedulingProducing
//    ) {
//
//    }
//
//    @Override
//    public void afterEntityRemoved(
//            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            SchedulingProducing schedulingProducing
//    ) {
//
//    }
//
//    public void schedulingProducingShadowUpdate() {
//        getSchedulingProduct().getMaterialAmountMap().forEach((sp, i) -> {
//            warehouse.compute(sp, ((spInMap, iInMap) -> warehouse.getOrDefault(spInMap, 0) - i));
//        });
//
//        //deduce arrange datetime,producing datetime and complete datetime
//        if (getPreviousProducing() == null) {
//            setArrangeDateTime(LocalDateTime.now());
//            setProducingInGameDateTime(getArrangeDateTime());
//            setCompletedInGameDateTime(this.getArrangeDateTime().plus(getSchedulingProduct().getProducingDuration()));
//        } else {
//            warehouse.compute(getPreviousProducing().getSchedulingProduct(), (sp, i) -> i + sp.getGainWhenCompleted());
//            setArrangeDateTime(getPreviousProducing().getCompletedInGameDateTime());
//            setProducingInGameDateTime(getPreviousProducing().getCompletedInGameDateTime());
//            setCompletedInGameDateTime(
//                    getPreviousProducing().getProducingInGameDateTime()
//                            .plus(getSchedulingProduct().getProducingDuration())
//            );
//        }
//    }
//
//}
