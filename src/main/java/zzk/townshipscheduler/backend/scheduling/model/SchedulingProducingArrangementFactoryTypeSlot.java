package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import zzk.townshipscheduler.backend.scheduling.model.utility.SchedulingProducingArrangementDifficultyComparator;
import zzk.townshipscheduler.backend.scheduling.model.utility.SchedulingDateTimeSlotStrengthComparator;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@PlanningEntity(difficultyComparatorClass = SchedulingProducingArrangementDifficultyComparator.class)
@JsonIgnoreProperties(allowGetters = true, allowSetters = true)
public class SchedulingProducingArrangementFactoryTypeSlot extends BaseSchedulingProducingArrangement {

    public static final String PLANNING_FACTORY = "planningFactory";

    @PlanningVariable(
            valueRangeProviderRefs = TownshipSchedulingProblem.DATE_TIME_SLOT_VALUE_RANGE,
            strengthComparatorClass = SchedulingDateTimeSlotStrengthComparator.class
    )
    private SchedulingDateTimeSlot planningDateTimeSlot;

    @PlanningVariable(valueRangeProviderRefs = TownshipSchedulingProblem.FACTORY_SLOT_VALUE_RANGE)
    private SchedulingFactoryInstanceTypeSlot planningFactory;

    public SchedulingProducingArrangementFactoryTypeSlot(
            IGameActionObject targetActionObject,
            IGameActionObject currentActionObject
    ) {
        super(targetActionObject, currentActionObject);
    }

    @Override
    public LocalDateTime getArrangeDateTime() {
        SchedulingDateTimeSlot dateTimeSlot = this.getPlanningDateTimeSlot();
        return dateTimeSlot != null
                ? dateTimeSlot.getStart()
                : null;
    }

    @Override
    public SchedulingFactoryInstanceTypeSlot getPlanningFactoryInstance() {
        return this.planningFactory;
    }

    @Override
    public LocalDateTime getProducingDateTime() {
        return getArrangeDateTime();
    }

    @Override
    public LocalDateTime getCompletedDateTime() {
        return getProducingDateTime() != null
                ? getProducingDateTime().plus(getProducingDuration())
                : null;
    }

}
