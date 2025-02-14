//package zzk.townshipscheduler.backend.scheduling.model;
//
//import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
//import ai.timefold.solver.core.api.domain.solution.cloner.DeepPlanningClone;
//import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
//import zzk.townshipscheduler.backend.scheduling.model.utility.SchedulingWarehouseStockVariableListener;
//
//import java.util.Map;
//
//@PlanningEntity
//public class SchedulingWarehouse {
//
//    @DeepPlanningClone
//    @ShadowVariable(
//            sourceVariableName = "shadowGameProducingDataTime",
//            sourceEntityClass = SchedulingPlayerFactoryAction.class,
//            variableListenerClass = SchedulingWarehouseStockVariableListener.class
//    )
//    @ShadowVariable(
//            sourceVariableName = "shadowGameCompleteDateTime",
//            sourceEntityClass = SchedulingPlayerFactoryAction.class,
//            variableListenerClass = SchedulingWarehouseStockVariableListener.class
//    )
//    private Map<SchedulingProduct, Integer> productAmountMap;
//
//}
