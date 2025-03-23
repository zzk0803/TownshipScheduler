package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import zzk.townshipscheduler.backend.scheduling.model.utility.ProducingArrangementDifficultyComparator;
import zzk.townshipscheduler.backend.scheduling.model.utility.SchedulingDateTimeSlotStrengthComparator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@PlanningEntity(difficultyComparatorClass = ProducingArrangementDifficultyComparator.class)
public class SchedulingFactorySlotProducingArrangement extends BaseProducingArrangement {

    @PlanningVariable(
            valueRangeProviderRefs = TownshipSchedulingProblem.DATE_TIME_SLOT_VALUE_RANGE,
            strengthComparatorClass = SchedulingDateTimeSlotStrengthComparator.class
    )
    private SchedulingDateTimeSlot planningDateTimeSlot;

    @PlanningVariable(valueRangeProviderRefs = TownshipSchedulingProblem.FACTORY_SLOT_VALUE_RANGE)
    private SchedulingTypeSlotFactoryInstance planningFactory;

    public SchedulingFactorySlotProducingArrangement(
            IGameActionObject targetActionObject,
            IGameActionObject currentActionObject
    ) {
        super(targetActionObject, currentActionObject);
    }

    @Override
    public List<ActionConsequence> calcConsequence() {
        if (
                getPlanningDateTimeSlot() == null
                || getPlanningFactoryInstance() == null
                || getCompletedDateTime() == null
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

    @Override
    public LocalDateTime getPlanningDateTimeSlotStartAsLocalDateTime() {
        SchedulingDateTimeSlot dateTimeSlot = this.getPlanningDateTimeSlot();
        return dateTimeSlot != null
                ? dateTimeSlot.getStart()
                : null;
    }

    @Override
    public SchedulingTypeSlotFactoryInstance getPlanningFactoryInstance() {
        return this.planningFactory;
    }

    @Override
    public LocalDateTime getProducingDateTime() {
        return getPlanningDateTimeSlotStartAsLocalDateTime();
    }

    @Override
    public LocalDateTime getCompletedDateTime() {
        return getProducingDateTime() != null
                ? getProducingDateTime().plus(getProducingDuration())
                : null;
    }

}
