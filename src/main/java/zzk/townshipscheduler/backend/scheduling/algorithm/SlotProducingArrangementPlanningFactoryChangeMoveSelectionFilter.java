package zzk.townshipscheduler.backend.scheduling.algorithm;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.ChangeMove;
import zzk.townshipscheduler.backend.scheduling.model.*;

public class SlotProducingArrangementPlanningFactoryChangeMoveSelectionFilter
        implements SelectionFilter<TownshipSchedulingProblem, ChangeMove<TownshipSchedulingProblem>> {

    @Override
    public boolean accept(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            ChangeMove<TownshipSchedulingProblem> selection
    ) {
        var entity = (SchedulingFactorySlotProducingArrangement) selection.getEntity();
        var toPlanningValue = (SchedulingTypeSlotFactoryInstance ) selection.getToPlanningValue();
        return entity.getRequiredFactoryInfo() == toPlanningValue.getSchedulingFactoryInfo();
    }

}
