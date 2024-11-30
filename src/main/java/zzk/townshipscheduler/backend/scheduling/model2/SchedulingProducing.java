//package zzk.townshipscheduler.backend.scheduling.model2;
//
//import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
//import ai.timefold.solver.core.api.domain.lookup.PlanningId;
//import ai.timefold.solver.core.api.domain.solution.cloner.DeepPlanningClone;
//import ai.timefold.solver.core.api.domain.variable.*;
//import zzk.townshipscheduler.backend.scheduling.model.SchedulingProduct;
//
//import java.time.LocalDateTime;
//import java.util.LinkedHashMap;
//
//@PlanningEntity
//public class SchedulingProducing extends SchedulingFactoryOrProducing{
//
//    @PlanningId
//    //order-target-num
//    private String identification;
//
//    private SchedulingProduct product;
//
//    @PlanningVariable(graphType = PlanningVariableGraphType.CHAINED)
//    private SchedulingProducing previous;
//
//    @AnchorShadowVariable(sourceVariableName = "previous")
//    private SchedulingFactory schedulingFactory;
//
//    @PlanningVariable
//    private int dayInMinute;
//
//    @ShadowVariable(
//            sourceVariableName = "previous",
//            variableListenerClass = XXXUpdateVariableListener.class
//    )
//    @ShadowVariable(
//            sourceVariableName = "dayInMinute",
//            variableListenerClass = XXXUpdateVariableListener.class
//    )
//    private LocalDateTime arrangeDateTime;
//
//    @PiggybackShadowVariable(shadowVariableName = "arrangeDateTime")
//    private LocalDateTime producingInGameDateTime;
//
//    @PiggybackShadowVariable(shadowVariableName = "arrangeDateTime")
//    private LocalDateTime completedInGameDateTime;
//
//    @DeepPlanningClone
//    @PiggybackShadowVariable(shadowVariableName = "arrangeDateTime")
//    private LinkedHashMap<SchedulingProduct, Integer> warehouse = new LinkedHashMap<>();
//
//
//
//}
