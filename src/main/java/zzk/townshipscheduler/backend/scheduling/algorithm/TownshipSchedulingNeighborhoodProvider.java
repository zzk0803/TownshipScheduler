package zzk.townshipscheduler.backend.scheduling.algorithm;

import ai.timefold.solver.core.preview.api.move.builtin.ChangeMoveProvider;
import ai.timefold.solver.core.preview.api.move.builtin.PillarChangeMoveProvider;
import ai.timefold.solver.core.preview.api.move.builtin.SubPillarChangeMoveProvider;
import ai.timefold.solver.core.preview.api.move.builtin.SwapMoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.Neighborhood;
import ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodBuilder;
import ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Samplers;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingDateTimeSlot;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInstance;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

public class TownshipSchedulingNeighborhoodProvider
        implements NeighborhoodProvider<TownshipSchedulingProblem> {

    @Override
    public Neighborhood defineNeighborhood(NeighborhoodBuilder<TownshipSchedulingProblem> builder) {
        var mm = builder.getSolutionMetaModel();
        var arrMM = mm.genuineEntity(SchedulingProducingArrangement.class);
        var slotVar = arrMM.basicVariable(SchedulingProducingArrangement.PLANNING_DATE_TIME_SLOT, SchedulingDateTimeSlot.class);
        var factoryVar = arrMM.basicVariable(SchedulingProducingArrangement.PLANNING_FACTORY_INSTANCE, SchedulingFactoryInstance.class);
        return builder.add(new ChangeMoveProvider<>(slotVar))
                .add(new ChangeMoveProvider<>(factoryVar))
                .add(new SwapMoveProvider<>(arrMM))
                .add(new PillarChangeMoveProvider<>(slotVar))
                .add(new SubPillarChangeMoveProvider<>(slotVar, Samplers.pillar(Samplers.between(2, 4))))
                .build();
    }

}
