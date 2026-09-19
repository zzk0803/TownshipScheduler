//package zzk.townshipscheduler.backend.scheduling.algorithm;
//
//import ai.timefold.solver.core.preview.api.move.SolutionView;
//import ai.timefold.solver.core.preview.api.move.builtin.*;
//import ai.timefold.solver.core.preview.api.neighborhood.Neighborhood;
//import ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodBuilder;
//import ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodProvider;
//import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Samplers;
//import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.NeighborhoodsJoiners;
//import zzk.townshipscheduler.backend.scheduling.model.SchedulingDateTimeSlot;
//import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;
//import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;
//
//import java.time.LocalDateTime;
//
//public class TownshipSchedulingNeighborhoodProvider
//        implements NeighborhoodProvider<TownshipSchedulingProblem> {
//
//    @Override
//    public Neighborhood defineNeighborhood(NeighborhoodBuilder<TownshipSchedulingProblem> neighborhoodBuilder) {
//        var dataTimeVariableMeta = neighborhoodBuilder.getSolutionMetaModel()
//                .genuineEntity(SchedulingProducingArrangement.class)
//                .basicVariable(SchedulingProducingArrangement.PLANNING_DATE_TIME_SLOT);
//
//        var factoryVariableMeta = neighborhoodBuilder.getSolutionMetaModel()
//                .genuineEntity(SchedulingProducingArrangement.class)
//                .basicVariable(SchedulingProducingArrangement.PLANNING_FACTORY_INSTANCE);
//
//
//        return neighborhoodBuilder.add(new ChangeMoveProvider<>(dataTimeVariableMeta))
//                .add(new ChangeMoveProvider<>(factoryVariableMeta))
//                .add(new MassChangeMoveProvider<>(dataTimeVariableMeta, Samplers.all()))
//                .add(new PillarChangeMoveProvider<>(dataTimeVariableMeta))
//                .add(new SubPillarChangeMoveProvider<>(dataTimeVariableMeta, Samplers.pillar(Samplers.all())))
//                .build();
//    }
//
//}
