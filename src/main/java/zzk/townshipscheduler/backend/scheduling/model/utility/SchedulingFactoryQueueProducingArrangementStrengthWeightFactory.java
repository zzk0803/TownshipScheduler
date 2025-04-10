//package zzk.townshipscheduler.backend.scheduling.model.utility;
//
//import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorterWeightFactory;
//import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryQueueProducingArrangement;
//import zzk.townshipscheduler.backend.scheduling.model.SchedulingTypeQueueFactoryInstance;
//import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;
//
//public class SchedulingFactoryQueueProducingArrangementStrengthWeightFactory
//        implements SelectionSorterWeightFactory<TownshipSchedulingProblem, SchedulingFactoryQueueProducingArrangement> {
//
//    @Override
//    public Comparable createSorterWeight(
//            TownshipSchedulingProblem townshipSchedulingProblem,
//            SchedulingFactoryQueueProducingArrangement queueProducingArrangement
//    ) {
//        SchedulingTypeQueueFactoryInstance maySingleInstancePlanningFactory
//                = queueProducingArrangement.getMaySingleInstancePlanningFactory();
//        return maySingleInstancePlanningFactory != null ? 10.0 : 1.0;
//    }
//
//}
