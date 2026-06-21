package zzk.townshipscheduler.backend.scheduling.algorithm;

import ai.timefold.solver.core.impl.heuristic.move.AbstractSelectorBasedMove;
import ai.timefold.solver.core.impl.heuristic.move.SelectorBasedNoChangeMove;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SelectorBasedListAssignMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SelectorBasedListChangeMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SelectorBasedListUnassignMove;
import ai.timefold.solver.core.impl.score.director.ScoreDirector;
import lombok.extern.slf4j.Slf4j;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInstance;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

@Slf4j
public class ProducingArrangementListChangeMoveFilter
        implements SelectionFilter<
        TownshipSchedulingProblem,
        AbstractSelectorBasedMove<TownshipSchedulingProblem>
        > {

    @Override
    public boolean accept(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            AbstractSelectorBasedMove<TownshipSchedulingProblem> selection
    ) {
        if (selection instanceof SelectorBasedListChangeMove<TownshipSchedulingProblem> selectorBasedListChangeMove) {
            return selectorBasedListChangeMovePredicate(selectorBasedListChangeMove);
        } else if (selection instanceof SelectorBasedListAssignMove<TownshipSchedulingProblem> selectorBasedListAssignMove) {
            return selectorBasedListAssignMovePredicate(
                    selectorBasedListAssignMove.getMovedValue(),
                    selectorBasedListAssignMove.getDestinationIndex(),
                    selectorBasedListAssignMove.getDestinationEntity()
            );
        } else if (selection instanceof SelectorBasedListUnassignMove<TownshipSchedulingProblem> selectorBasedListUnassignMove) {
            return true;
        } else if (selection instanceof SelectorBasedNoChangeMove<TownshipSchedulingProblem> selectorBasedNoChangeMove) {
            return true;
        } else {
            log.info(
                    "else branch,selection class={}",
                    selection.getClass()
            );
            throw new IllegalStateException();
        }
    }

    private boolean selectorBasedListChangeMovePredicate(SelectorBasedListChangeMove<TownshipSchedulingProblem> selection) {
        return selectorBasedListAssignMovePredicate(
                selection.getMovedValue(),
                selection.getDestinationIndex(),
                selection.getDestinationEntity()
        );
    }

    private boolean selectorBasedListAssignMovePredicate(
            Object movedValue,
            int destinationIndex,
            Object destinationEntity
    ) {
        SchedulingProducingArrangement arrangementMoved = (SchedulingProducingArrangement) movedValue;

        if (destinationIndex == 0) {
            return true;
        }

        SchedulingFactoryInstance schedulingFactoryInstance = (SchedulingFactoryInstance) destinationEntity;
        SchedulingProducingArrangement arrangementBeforeDestination =
                schedulingFactoryInstance.getPlanningArrangementsSequence().get(destinationIndex - 1);
        return arrangementBeforeDestination.getShadowDateTimeSlot() != null
                && arrangementBeforeDestination.getShadowDateTimeSlot().compareTo(arrangementMoved.getShadowDateTimeSlot()) <= 0;
    }

}
