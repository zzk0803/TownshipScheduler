//package zzk.townshipscheduler.backend.scheduling.algorithm;
//
//import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
//import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
//import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
//import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
//import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInstance;
//import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;
//import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;
//
//public class TownshipSchedulingMoveProvider implements MoveProvider<TownshipSchedulingProblem> {
//
//    PlanningListVariableMetaModel<TownshipSchedulingProblem, SchedulingFactoryInstance, SchedulingProducingArrangement> planningListVariableMetaModel;
//
//    public TownshipSchedulingMoveProvider(PlanningListVariableMetaModel<TownshipSchedulingProblem, SchedulingFactoryInstance, SchedulingProducingArrangement> planningListVariableMetaModel) {
//        this.planningListVariableMetaModel = planningListVariableMetaModel;
//    }
//
//    @Override
//    public MoveStream<TownshipSchedulingProblem> build(MoveStreamFactory<TownshipSchedulingProblem> moveStreamFactory) {
//        moveStreamFactory.forEach(SchedulingProducingArrangement.class, true);
//        moveStreamFactory.forEachUnfiltered(SchedulingProducingArrangement.class, false);
//        moveStreamFactory.forEachAssignedValue(planningListVariableMetaModel)
//                .filter((solutionView, schedulingProducingArrangement) -> true);
//        moveStreamFactory.forEachAssignedValueUnfiltered(planningListVariableMetaModel);
//        moveStreamFactory.forEachDestination(planningListVariableMetaModel)
//                .filter((solutionView, positionInList) -> true);
//        moveStreamFactory.forEachDestinationIncludingUnassigned(planningListVariableMetaModel);
//        moveStreamFactory.forEachUnassignedValue(planningListVariableMetaModel);
//        return null;
//    }
//
//}
