package zzk.townshipscheduler.backend.scheduling.algorithm;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.SwapMove;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInfo;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactorySlotProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingTypeSlotFactoryInstance;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

public class SlotProducingArrangementPlanningFactorySwapMoveSelectionFilter
        implements SelectionFilter<TownshipSchedulingProblem, SwapMove<TownshipSchedulingProblem>> {

    @Override
    public boolean accept(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            SwapMove<TownshipSchedulingProblem> selection
    ) {
        SchedulingFactorySlotProducingArrangement leftEntity
                = (SchedulingFactorySlotProducingArrangement) selection.getLeftEntity();
        SchedulingFactoryInfo leftProducingRequiredFactoryInfo = leftEntity.getRequiredFactoryInfo();
        SchedulingTypeSlotFactoryInstance leftEntityPlanningFactory = leftEntity.getPlanningFactory();

        SchedulingFactorySlotProducingArrangement rightEntity
                = (SchedulingFactorySlotProducingArrangement) selection.getRightEntity();
        SchedulingFactoryInfo rightProducingRequiredFactoryInfo = rightEntity.getRequiredFactoryInfo();
        SchedulingTypeSlotFactoryInstance rightEntityPlanningFactory = rightEntity.getPlanningFactory();
        return leftProducingRequiredFactoryInfo == rightEntityPlanningFactory.getSchedulingFactoryInfo()
                || rightProducingRequiredFactoryInfo == leftEntityPlanningFactory.getSchedulingFactoryInfo();
    }

}
