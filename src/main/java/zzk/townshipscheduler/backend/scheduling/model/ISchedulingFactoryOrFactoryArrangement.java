package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;

import java.time.LocalDateTime;

@PlanningEntity
public interface ISchedulingFactoryOrFactoryArrangement {

    String PLANING_NEXT_FACTORY_SEQUENCE = "nextQueueProducingArrangement";

    @InverseRelationShadowVariable(sourceVariableName = SchedulingFactoryQueueProducingArrangement.PLANNING_PREVIOUS)
    SchedulingFactoryQueueProducingArrangement getNextQueueProducingArrangement();

    void setNextQueueProducingArrangement(SchedulingFactoryQueueProducingArrangement schedulingFactoryQueueProducingArrangement);

    LocalDateTime getCompletedDateTime();

}
