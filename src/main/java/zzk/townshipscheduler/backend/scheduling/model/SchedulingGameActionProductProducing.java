package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
@PlanningEntity
public class SchedulingGameActionProductProducing extends SchedulingGameAction{

    @PlanningVariable
    private SchedulingProducingExecutionMode planningProducingExecutionMode;

    @ValueRangeProvider
    private Set<SchedulingProducingExecutionMode> valueRangeProducingExecutionModeSet;

    @PlanningVariable
    private SchedulingFactoryInstance planningFactoryInstance;

//    @ShadowVariable(
//            variableListenerClass = SchedulingGameActionProductProducingVariableListener.class,
//            sourceVariableName = "executionMode",
//            sourceEntityClass = SchedulingGameAction.class
//    )
//    @ShadowVariable(
//            variableListenerClass = SchedulingGameActionProductProducingVariableListener.class,
//            sourceVariableName = "dateTimeSlot",
//            sourceEntityClass = SchedulingGameAction.class
//    )
//    @ShadowVariable(
//            variableListenerClass = SchedulingGameActionProductProducingVariableListener.class,
//            sourceVariableName = "schedulingFactoryInstance"
//    )
    private LocalDateTime shadowGameProducingDataTime;

//    @PiggybackShadowVariable(shadowVariableName = "shadowGameProducingDataTime")
    private LocalDateTime shadowGameCompleteDateTime;

    public SchedulingGameActionProductProducing() {
    }

    public SchedulingGameActionProductProducing(Set<SchedulingProducingExecutionMode> valueRangeProducingExecutionModeSet) {
        this.valueRangeProducingExecutionModeSet = valueRangeProducingExecutionModeSet;
    }

    public SchedulingGameActionProductProducing(
            SchedulingGameActionObject schedulingGameActionObject,
            Set<SchedulingProducingExecutionMode> valueRangeProducingExecutionModeSet
    ) {
        super(schedulingGameActionObject);
        this.valueRangeProducingExecutionModeSet = valueRangeProducingExecutionModeSet;
    }

    public void setExecutionModeMandatory(SchedulingProducingExecutionMode executionMode) {
        if (getValueRangeProducingExecutionModeSet().contains(executionMode)) {
            setPlanningProducingExecutionMode(executionMode);
            executionMode.setBoolWeatherExecutionModeSingle(true);
        }else {
            throw new IllegalArgumentException();
        }
    }

    public void cleanShadowVariable() {
        setShadowGameProducingDataTime(null);
        setShadowGameCompleteDateTime(null);
    }

    @Override
    public String getHumanReadable() {
        SchedulingGameActionObject gameActionObject = this.getSchedulingGameActionObject();
        Assert.isInstanceOf(SchedulingProduct.class, gameActionObject);
        SchedulingProduct schedulingProduct = (SchedulingProduct) gameActionObject;
        return "Arrange Producing::" + schedulingProduct.getName();
    }

}
