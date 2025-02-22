//package zzk.townshipscheduler.backend.scheduling.model;
//
//import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
//import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
//import lombok.Data;
//
//import java.time.Duration;
//import java.time.LocalDateTime;
//import java.util.List;
//
//@Data
//@PlanningEntity
//public abstract class BasePlanningChainSupportFactoryOrAction {
//
//    @InverseRelationShadowVariable(sourceVariableName = "planningPrevious")
//    private SchedulingPlayerFactoryAction planningNext;
//
//    public Duration nextAvailableAsDuration(LocalDateTime dateTime) {
//        if (planningNext == null) {
//            return Duration.ZERO;
//        } else {
//            LocalDateTime shadowGameCompleteDateTime
//                    = planningNext.getShadowGameCompleteDateTime();
//            return shadowGameCompleteDateTime == null
//                    ? null
//                    : Duration.between(dateTime, shadowGameCompleteDateTime);
//        }
//    }
//
//    public abstract List<PlayerFactoryActionConsequence> calcTimeSlotFactoryConsequence();
//
//}
