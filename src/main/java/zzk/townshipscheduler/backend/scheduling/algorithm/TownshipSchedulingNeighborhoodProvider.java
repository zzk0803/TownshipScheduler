//package zzk.townshipscheduler.backend.scheduling.algorithm;
//
//import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
//import ai.timefold.solver.core.preview.api.neighborhood.Neighborhood;
//import ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodBuilder;
//import ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodProvider;
//import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInstance;
//import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;
//import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;
//
//public class TownshipSchedulingNeighborhoodProvider
//        implements NeighborhoodProvider<TownshipSchedulingProblem> {
//
//    @Override
//    public Neighborhood defineNeighborhood(NeighborhoodBuilder<TownshipSchedulingProblem> builder) {
//        PlanningListVariableMetaModel<TownshipSchedulingProblem, SchedulingFactoryInstance, SchedulingProducingArrangement> listVariableMetaModel
//                =
//                (PlanningListVariableMetaModel<TownshipSchedulingProblem, SchedulingFactoryInstance, SchedulingProducingArrangement>) builder.getSolutionMetaModel()
//                .entity(SchedulingFactoryInstance.class)
//                .<SchedulingProducingArrangement>variable(SchedulingFactoryInstance.PLANNING_ARRANGEMENTS_SEQUENCE);
//        builder.add(new TownshipSchedulingMoveProvider(listVariableMetaModel));
//        return builder.build();
//    }
//
//}
