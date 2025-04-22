package zzk.townshipscheduler.backend.scheduling.algorithm;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.ChangeMove;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingDateTimeSlot;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class ProducingArrangementPlanningDataTimeSlotChangeMoveSelectionFilter
        implements SelectionFilter<TownshipSchedulingProblem, ChangeMove<TownshipSchedulingProblem>> {

    @Override
    public boolean accept(
            ScoreDirector<TownshipSchedulingProblem> scoreDirector,
            ChangeMove<TownshipSchedulingProblem> selection
    ) {
        var entity = (SchedulingProducingArrangement) selection.getEntity();
        var toPlanningValue = (SchedulingDateTimeSlot) selection.getToPlanningValue();
        LocalDateTime toPlanningValueStart = toPlanningValue.getStart();
        List<SchedulingProducingArrangement> prerequisiteProducingArrangements = entity.getPrerequisiteProducingArrangements();
        if (prerequisiteProducingArrangements.isEmpty()) {
            return true;
        } else {
            LocalDateTime workCalendarStart = entity.getSchedulingWorkCalendar().getStartDateTime();
            Duration duration = prerequisiteProducingArrangements.stream()
                    .map(SchedulingProducingArrangement::getProducingDuration)
                    .filter(Objects::nonNull)
                    .max(Duration::compareTo)
                    .orElse(Duration.ZERO);
            LocalDateTime approximatedMinStartDateTime = workCalendarStart.plus(duration);
            return !toPlanningValueStart.isBefore(approximatedMinStartDateTime);
        }
    }

}
