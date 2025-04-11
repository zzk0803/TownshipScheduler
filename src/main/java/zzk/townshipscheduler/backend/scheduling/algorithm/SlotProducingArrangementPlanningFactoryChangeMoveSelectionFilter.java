package zzk.townshipscheduler.backend.scheduling.algorithm;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.ChangeMove;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInstanceTypeSlot;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangementFactoryTypeSlot;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

public class SlotProducingArrangementPlanningFactoryChangeMoveSelectionFilter
        implements SelectionFilter<TownshipSchedulingProblem, ChangeMove<TownshipSchedulingProblem>> {

    @Override
    public boolean accept(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            ChangeMove<TownshipSchedulingProblem> selection
    ) {
        var entity = (SchedulingProducingArrangementFactoryTypeSlot) selection.getEntity();
        var toPlanningValue = (SchedulingFactoryInstanceTypeSlot) selection.getToPlanningValue();
        return entity.getRequiredFactoryInfo() == toPlanningValue.getSchedulingFactoryInfo()
               && entity.getPlanningFactory() != toPlanningValue
                ;
    }

}
