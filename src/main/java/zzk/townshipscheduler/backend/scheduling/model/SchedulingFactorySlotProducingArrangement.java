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

    public static final String PLANNING_FACTORY = "planningFactory";

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
    public List<ArrangeConsequence> calcConsequence() {
        if (
                getPlanningDateTimeSlot() == null
                || getPlanningFactoryInstance() == null
                || getCompletedDateTime() == null
        ) {
            return List.of();
        }

        SchedulingProducingExecutionMode executionMode = getProducingExecutionMode();
        List<ArrangeConsequence> arrangeConsequenceList = new ArrayList<>(5);
        //when arrange,materials was consumed
        if (!executionMode.boolAtomicProduct()) {
            ProductAmountBill materials = executionMode.getMaterials();
            materials.forEach((material, amount) -> {
                ArrangeConsequence consequence = ArrangeConsequence.builder()
                        .producingArrangement(this)
                        .localDateTime(getPlanningDateTimeSlotStartAsLocalDateTime())
                        .resource(ArrangeConsequence.SchedulingResource.productStock(material))
                        .resourceChange(ArrangeConsequence.SchedulingResourceChange.decrease(amount))
                        .build();
                arrangeConsequenceList.add(consequence);
            });
        }

        //when arrange,factory wait queue was consumed
        arrangeConsequenceList.add(
                ArrangeConsequence.builder()
                        .producingArrangement(this)
                        .localDateTime(getPlanningDateTimeSlotStartAsLocalDateTime())
                        .resource(
                                ArrangeConsequence.SchedulingResource.factoryWaitQueue(
                                        getPlanningFactoryInstance()
                                )
                        )
                        .resourceChange(ArrangeConsequence.SchedulingResourceChange.decrease())
                        .build()
        );

        //when completed ,factory wait queue was release
        arrangeConsequenceList.add(
                ArrangeConsequence.builder()
                        .producingArrangement(this)
                        .localDateTime(getCompletedDateTime())
                        .resource(ArrangeConsequence.SchedulingResource.factoryWaitQueue(
                                        getPlanningFactoryInstance()
                                )
                        )
                        .resourceChange(ArrangeConsequence.SchedulingResourceChange.increase())
                        .build()
        );

        //when completed ,product stock was increase
        arrangeConsequenceList.add(
                ArrangeConsequence.builder()
                        .producingArrangement(this)
                        .localDateTime(getCompletedDateTime())
                        .resource(ArrangeConsequence.SchedulingResource.productStock(getSchedulingProduct()))
                        .resourceChange(ArrangeConsequence.SchedulingResourceChange.increase())
                        .build()
        );


        return arrangeConsequenceList;
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
