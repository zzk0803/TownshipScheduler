package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.AnchorShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariableGraphType;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import lombok.*;
import zzk.townshipscheduler.backend.scheduling.model.utility.ProducingArrangementDifficultyComparator;
import zzk.townshipscheduler.backend.scheduling.model.utility.ProducingArrangementFactorySequenceVariableListener;
import zzk.townshipscheduler.backend.scheduling.model.utility.SchedulingDateTimeSlotStrengthComparator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@PlanningEntity(difficultyComparatorClass = ProducingArrangementDifficultyComparator.class)
public class SchedulingFactoryQueueProducingArrangement
        extends BaseProducingArrangement
        implements ISchedulingFactoryOrFactoryArrangement {

    public static final String PLANNING_ANCHOR_FACTORY = "planningAnchorFactory";

    public static final String PLANNING_PREVIOUS = "planningPreviousProducingArrangementOrFactory";

    public static final String SHADOW_PRODUCING_DATE_TIME = "shadowProducingDateTime";

    @PlanningVariable(
            valueRangeProviderRefs = TownshipSchedulingProblem.DATE_TIME_SLOT_VALUE_RANGE,
            strengthComparatorClass = SchedulingDateTimeSlotStrengthComparator.class
    )
    private SchedulingDateTimeSlot planningDateTimeSlot;

    @AnchorShadowVariable(
            sourceVariableName = PLANNING_PREVIOUS
    )
    private SchedulingTypeQueueFactoryInstance planningAnchorFactory;

    @PlanningVariable(
            graphType = PlanningVariableGraphType.CHAINED,
            valueRangeProviderRefs = {
                    TownshipSchedulingProblem.FACTORY_QUEUE_VALUE_RANGE,
                    TownshipSchedulingProblem.PRODUCING_ARRANGEMENTS_FACTORY_QUEUE_VALUE_RANGE
            }
    )
    private ISchedulingFactoryOrFactoryArrangement planningPreviousProducingArrangementOrFactory;

    @Getter
    @Setter
    private SchedulingFactoryQueueProducingArrangement nextQueueProducingArrangement;

    @ShadowVariable(
            sourceVariableName = PLANNING_ANCHOR_FACTORY,
            variableListenerClass = ProducingArrangementFactorySequenceVariableListener.class
    )
    @ShadowVariable(
            sourceVariableName = PLANNING_PREVIOUS,
            variableListenerClass = ProducingArrangementFactorySequenceVariableListener.class
    )
    @ShadowVariable(
            sourceVariableName = PLANNING_DATA_TIME_SLOT,
            variableListenerClass = ProducingArrangementFactorySequenceVariableListener.class
    )
    private LocalDateTime shadowProducingDateTime;

//    @PiggybackShadowVariable(shadowVariableName = SHADOW_PRODUCING_DATE_TIME)
//    private LocalDateTime shadowCompletedDateTime;

    public SchedulingFactoryQueueProducingArrangement(
            IGameActionObject targetActionObject,
            IGameActionObject currentActionObject
    ) {
        super(targetActionObject, currentActionObject);
    }

    public List<ActionConsequence> calcConsequence() {
        if (
                getPlanningDateTimeSlot() == null
                || getPlanningFactoryInstance() == null
                || this.getCompletedDateTime() == null
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
                                        getPlanningFactoryInstance()
                                )
                        )
                        .resourceChange(ActionConsequence.SchedulingResourceChange.decrease())
                        .build()
        );

        //when completed ,factory wait queue was release
        actionConsequenceList.add(
                ActionConsequence.builder()
                        .actionId(getActionId())
                        .localDateTime(getCompletedDateTime())
                        .resource(ActionConsequence.SchedulingResource.factoryWaitQueue(
                                        getPlanningFactoryInstance()
                                )
                        )
                        .resourceChange(ActionConsequence.SchedulingResourceChange.increase())
                        .build()
        );

        //when completed ,product stock was increase
        actionConsequenceList.add(
                ActionConsequence.builder()
                        .actionId(getActionId())
                        .localDateTime(getCompletedDateTime())
                        .resource(ActionConsequence.SchedulingResource.productStock(getSchedulingProduct()))
                        .resourceChange(ActionConsequence.SchedulingResourceChange.increase())
                        .build()
        );


        return actionConsequenceList;
    }

    public LocalDateTime getShadowCompletedDateTime() {
        if (shadowProducingDateTime == null) {
            return null;
        }
        return shadowProducingDateTime.plus(getProducingDuration());
    }

    @Override
    public LocalDateTime getPlanningDateTimeSlotStartAsLocalDateTime() {
        SchedulingDateTimeSlot dateTimeSlot = this.getPlanningDateTimeSlot();
        return dateTimeSlot != null
                ? dateTimeSlot.getStart()
                : null;
    }

    @Override
    public SchedulingTypeQueueFactoryInstance getPlanningFactoryInstance() {
        return this.planningAnchorFactory;
    }

    @Override
    public LocalDateTime getProducingDateTime() {
        return this.shadowProducingDateTime;
    }

    @Override
    public LocalDateTime getCompletedDateTime() {
        return this.getShadowCompletedDateTime();
    }

    @Override
    public SchedulingFactoryInfo getFactoryInfo() {
        return getPlanningFactoryInstance().getSchedulingFactoryInfo();
    }

}
