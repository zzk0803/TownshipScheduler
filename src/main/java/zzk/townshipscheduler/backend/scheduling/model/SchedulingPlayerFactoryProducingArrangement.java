package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import zzk.townshipscheduler.backend.scheduling.model.utility.ProducingFactoryArrangementComputedDateTimeVariableListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true,callSuper = true)
@PlanningEntity
public class SchedulingPlayerFactoryProducingArrangement
        extends AbstractPlayerProducingArrangement {

    public static final String PLANNING_FACTORY = "planningFactory";

    @PlanningVariable(
            valueRangeProviderRefs = TownshipSchedulingProblem.FACTORY_VALUE_RANGE
    )
    private SchedulingFactoryInstanceMultiple planningFactory;

    @ToString.Include
    private List<SchedulingFactoryInstanceMultiple> ValueRangeForPlanningFactory;

    public SchedulingPlayerFactoryProducingArrangement(
            IGameActionObject targetActionObject,
            IGameActionObject currentActionObject
    ) {
        super(targetActionObject, currentActionObject);
    }

//    @PlanningVariable(
//            valueRangeProviderRefs = TownshipSchedulingProblem.SEQUENCE_VALUE_RANGE
//    )
//    protected Integer planningSequence;

    @ShadowVariable(
            variableListenerClass = ProducingFactoryArrangementComputedDateTimeVariableListener.class,
            sourceVariableName = PLANNING_SEQUENCE,
            sourceEntityClass = AbstractPlayerProducingArrangement.class
    )
    @ShadowVariable(
            variableListenerClass = ProducingFactoryArrangementComputedDateTimeVariableListener.class,
            sourceVariableName = PLANNING_DATA_TIME_SLOT,
            sourceEntityClass = AbstractPlayerProducingArrangement.class
    )
    @ShadowVariable(
            variableListenerClass = ProducingFactoryArrangementComputedDateTimeVariableListener.class,
            sourceVariableName = PLANNING_FACTORY
    )
    @ShadowVariable(
            variableListenerClass = ProducingFactoryArrangementComputedDateTimeVariableListener.class,
            sourceVariableName = SchedulingFactoryInstanceMultiple.PLANNING_ACTIONS,
            sourceEntityClass = SchedulingFactoryInstanceMultiple.class
    )
    public LocalDateTime getShadowGameProducingDataTime() {
        return super.shadowGameProducingDataTime;
    }

    @Override
    public void activate(
            ActionIdRoller idRoller,
            SchedulingWorkTimeLimit workTimeLimit,
            SchedulingWarehouse schedulingWarehouse
    ) {
        super.activate(idRoller, workTimeLimit, schedulingWarehouse);
        prepareValueRangeForSchedulingFactoryInstance();
    }

    public void prepareValueRangeForSchedulingFactoryInstance() {
        SchedulingProduct schedulingProduct = getSchedulingProduct();
        SchedulingFactoryInfo requireFactory = schedulingProduct.getRequireFactory();
        ValueRangeForPlanningFactory = new ArrayList<>(requireFactory.getFactoryInstances());
    }

    @Override
    public List<ActionConsequence> calcConsequence() {
        if (getPlanningDateTimeSlotStartAsLocalDateTime() == null || getFactory() == null) {
            return List.of();
        }

        SchedulingProducingExecutionMode executionMode = getProducingExecutionMode();
        List<ActionConsequence> actionConsequenceList = new ArrayList<>(5);
        //when arrange,materials was consumed
        if (!executionMode.boolAtomicProduct()) {
            ProductAmountBill materials = executionMode.getMaterials();
            materials.forEach((material, amount) -> {
                ActionConsequence consequence = ActionConsequence.builder()
                        .actionId(getActionId())
                        .localDateTime(getPlanningDateTimeSlotStartAsLocalDateTime())
                        .resource(ActionConsequence.SchedulingResource.productStock(material))
                        .resourceChange(ActionConsequence.SchedulingResourceChange.decrease(amount))
                        .build();
                actionConsequenceList.add(consequence);
            });
        }

        //when arrange,factory wait queue was consumed
        actionConsequenceList.add(ActionConsequence.builder()
                .actionId(getActionId())
                .localDateTime(getPlanningDateTimeSlotStartAsLocalDateTime())
                .resource(ActionConsequence.SchedulingResource.factoryWaitQueue(getFactory()))
                .resourceChange(ActionConsequence.SchedulingResourceChange.decrease())
                .build());

        //when completed ,factory wait queue was release
        actionConsequenceList.add(ActionConsequence.builder()
                .actionId(getActionId())
                .localDateTime(getShadowGameCompleteDateTime())
                .resource(ActionConsequence.SchedulingResource.factoryWaitQueue(getFactory()))
                .resourceChange(ActionConsequence.SchedulingResourceChange.increase())
                .build());

        //when completed ,product stock was increase
        actionConsequenceList.add(ActionConsequence.builder()
                .actionId(getActionId())
                .localDateTime(getShadowGameCompleteDateTime())
                .resource(ActionConsequence.SchedulingResource.productStock(getSchedulingProduct()))
                .resourceChange(ActionConsequence.SchedulingResourceChange.increase())
                .build());


        return actionConsequenceList;
    }

    @Override
    public SchedulingFactoryInstanceMultiple getFactory() {
        return getPlanningFactory();
    }

    @ValueRangeProvider(id = TownshipSchedulingProblem.FACTORY_VALUE_RANGE)
    public List<SchedulingFactoryInstanceMultiple> valueRangeForSchedulingFactoryInstance() {
        return Collections.unmodifiableList(ValueRangeForPlanningFactory);
    }

}
