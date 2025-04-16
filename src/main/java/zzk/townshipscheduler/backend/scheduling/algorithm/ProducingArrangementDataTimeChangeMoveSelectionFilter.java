package zzk.townshipscheduler.backend.scheduling.algorithm;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.ChangeMove;
import zzk.townshipscheduler.backend.scheduling.model.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ProducingArrangementDataTimeChangeMoveSelectionFilter
        implements SelectionFilter<TownshipSchedulingProblem, ChangeMove<TownshipSchedulingProblem>> {

    @Override
    public boolean accept(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            ChangeMove<TownshipSchedulingProblem> selection
    ) {
        var entity = (BaseSchedulingProducingArrangement) selection.getEntity();
        var toPlanningValue = (SchedulingDateTimeSlot) selection.getToPlanningValue();
        List<BaseSchedulingProducingArrangement> prerequisiteProducingArrangements = entity.getPrerequisiteProducingArrangements();
        if (!prerequisiteProducingArrangements.isEmpty()) {
            Optional<LocalDateTime> prerequisiteCompletedDateTime = prerequisiteProducingArrangements.stream()
                    .map(BaseSchedulingProducingArrangement::getCompletedDateTime)
                    .filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo);
            if (prerequisiteCompletedDateTime.isEmpty()) {
                return true;
            }else {
                LocalDateTime localDateTime = prerequisiteCompletedDateTime.get();
                return !toPlanningValue.getStart().isBefore(localDateTime);
            }
        }
        return true;
    }

}
