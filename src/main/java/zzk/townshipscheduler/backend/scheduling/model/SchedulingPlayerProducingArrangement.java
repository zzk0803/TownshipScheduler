package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import lombok.*;
import zzk.townshipscheduler.backend.scheduling.model.utility.ProducingArrangementComputedDateTimeVariableListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true)
@PlanningEntity
public class SchedulingPlayerProducingArrangement
        extends AbstractPlayerProducingArrangement {

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private SchedulingFactoryInstanceSingle factoryInstance;

    public SchedulingPlayerProducingArrangement(
            IGameActionObject targetActionObject,
            IGameActionObject currentActionObject,
            SchedulingFactoryInstanceSingle factoryInstance
    ) {
        super(targetActionObject, currentActionObject);
        this.factoryInstance = factoryInstance;
    }

    @ShadowVariable(
            variableListenerClass = ProducingArrangementComputedDateTimeVariableListener.class,
            sourceVariableName = PLANNING_SEQUENCE
    )
    @ShadowVariable(
            variableListenerClass = ProducingArrangementComputedDateTimeVariableListener.class,
            sourceVariableName = PLANNING_DATA_TIME_SLOT
    )
    public LocalDateTime getShadowGameProducingDataTime() {
        return super.shadowGameProducingDataTime;
    }

    //    @ShadowVariable(
    //            variableListenerClass = FactoryActionShadowGameCompletedDataTimeVariableListener.class,
    //            sourceVariableName = "planningProducingExecutionMode"
    //    )
    //    @ShadowVariable(
    //            variableListenerClass = FactoryActionShadowGameCompletedDataTimeVariableListener.class,
    //            sourceVariableName = "shadowGameProducingDataTime"
    //    )

    @Override
    public List<ActionConsequence> calcConsequence() {
        if (
                getPlanningDateTimeSlotStartAsLocalDateTime() == null
                || getFactory() == null
        ) {
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
        actionConsequenceList.add(
                ActionConsequence.builder()
                        .actionId(getActionId())
                        .localDateTime(getPlanningDateTimeSlotStartAsLocalDateTime())
                        .resource(
                                ActionConsequence.SchedulingResource.factoryWaitQueue(
                                        getFactory()
                                )
                        )
                        .resourceChange(ActionConsequence.SchedulingResourceChange.decrease())
                        .build()
        );

        //when completed ,factory wait queue was release
        actionConsequenceList.add(
                ActionConsequence.builder()
                        .actionId(getActionId())
                        .localDateTime(getShadowGameCompleteDateTime())
                        .resource(ActionConsequence.SchedulingResource.factoryWaitQueue(
                                        getFactory()
                                )
                        )
                        .resourceChange(ActionConsequence.SchedulingResourceChange.increase())
                        .build()
        );

        //when completed ,product stock was increase
        actionConsequenceList.add(
                ActionConsequence.builder()
                        .actionId(getActionId())
                        .localDateTime(getShadowGameCompleteDateTime())
                        .resource(ActionConsequence.SchedulingResource.productStock(getSchedulingProduct()))
                        .resourceChange(ActionConsequence.SchedulingResourceChange.increase())
                        .build()
        );


        return actionConsequenceList;
    }

    @Override
    public SchedulingFactoryInstanceSingle getFactory() {
        return this.getFactoryInstance();
    }

}
