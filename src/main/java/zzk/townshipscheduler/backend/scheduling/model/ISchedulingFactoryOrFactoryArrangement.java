package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@PlanningEntity
public interface ISchedulingFactoryOrFactoryArrangement {

    String PLANING_NEXT_FACTORY_SEQUENCE = "nextQueueProducingArrangement";

    LocalDateTime getCompletedDateTime();

    SchedulingFactoryInfo getFactoryInfo();

    default List<SchedulingProducingArrangementFactoryTypeQueue> getBackwardArrangements(
            SchedulingProducingArrangementFactoryTypeQueue queueProducingArrangement
    ) {
        List<SchedulingProducingArrangementFactoryTypeQueue> result = new ArrayList<>();
        SchedulingProducingArrangementFactoryTypeQueue iterating
                = getPreviousQueueProducingArrangement(queueProducingArrangement).orElse(null);
        while (iterating != null) {
            result.add(queueProducingArrangement);
            iterating = getPreviousQueueProducingArrangement(iterating).orElse(null);
        }

        return result;
    }

    default Optional<SchedulingProducingArrangementFactoryTypeQueue> getPreviousQueueProducingArrangement(
            SchedulingProducingArrangementFactoryTypeQueue queueProducingArrangement
    ) {
        SchedulingProducingArrangementFactoryTypeQueue previousQueueProducingArrangement = null;
        ISchedulingFactoryOrFactoryArrangement planningPreviousProducingArrangementOrFactory
                = queueProducingArrangement.getPlanningPreviousProducingArrangementOrFactory();
        if (planningPreviousProducingArrangementOrFactory instanceof SchedulingProducingArrangementFactoryTypeQueue) {
            previousQueueProducingArrangement = ((SchedulingProducingArrangementFactoryTypeQueue) planningPreviousProducingArrangementOrFactory);
        }
        return Optional.ofNullable(previousQueueProducingArrangement);
    }

    default List<SchedulingProducingArrangementFactoryTypeQueue> getForwardArrangements() {
        SchedulingProducingArrangementFactoryTypeQueue firstArrangement
                = this.getNextQueueProducingArrangement();

        if (Objects.isNull(firstArrangement)) {
            return List.of();
        }

        return Stream.iterate(
                firstArrangement,
                (arrangement) -> arrangement.getNextQueueProducingArrangement() != null,
                SchedulingProducingArrangementFactoryTypeQueue::getNextQueueProducingArrangement
        ).toList();
    }

    @InverseRelationShadowVariable(sourceVariableName = SchedulingProducingArrangementFactoryTypeQueue.PLANNING_PREVIOUS)
    SchedulingProducingArrangementFactoryTypeQueue getNextQueueProducingArrangement();

    void setNextQueueProducingArrangement(SchedulingProducingArrangementFactoryTypeQueue queueProducingArrangement);

}
