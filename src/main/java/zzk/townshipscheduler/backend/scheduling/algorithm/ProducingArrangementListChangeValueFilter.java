//package zzk.townshipscheduler.backend.scheduling.algorithm;
//
//import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
//import ai.timefold.solver.core.impl.score.director.ScoreDirector;
//import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;
//import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;
//
//public class ProducingArrangementListChangeValueFilter implements SelectionFilter<TownshipSchedulingProblem, SchedulingProducingArrangement> {
//
//    @Override
//    public boolean accept(
//            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
//            SchedulingProducingArrangement selection
//    ) {
//        return selection.getShadowPrerequisiteProducingArrangementsFinishedDateTime() != null && selection.getShadowDateTimeSlot() != null;
//    }
//
//}
