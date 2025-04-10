package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.AnchorShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariableGraphType;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import zzk.townshipscheduler.backend.scheduling.model.utility.ProducingArrangementDifficultyComparator;
import zzk.townshipscheduler.backend.scheduling.model.utility.ProducingArrangementFactorySequenceVariableListener;
import zzk.townshipscheduler.backend.scheduling.model.utility.SchedulingDateTimeSlotStrengthComparator;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@PlanningEntity(difficultyComparatorClass = ProducingArrangementDifficultyComparator.class)
@JsonIgnoreProperties(allowGetters = true, allowSetters = true)
public class SchedulingProducingArrangementFactoryTypeQueue
        extends BaseSchedulingProducingArrangement
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
    private SchedulingFactoryInstanceTypeQueue planningAnchorFactory;

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
    private SchedulingProducingArrangementFactoryTypeQueue nextQueueProducingArrangement;

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

    public SchedulingProducingArrangementFactoryTypeQueue(
            IGameActionObject targetActionObject,
            IGameActionObject currentActionObject
    ) {
        super(targetActionObject, currentActionObject);
    }

    @JsonIgnore
    @Override
    public SchedulingFactoryInfo getFactoryInfo() {
        return getPlanningFactoryInstance().getSchedulingFactoryInfo();
    }

    @Override
    public SchedulingFactoryInstanceTypeQueue getPlanningFactoryInstance() {
        return this.planningAnchorFactory;
    }

    @Override
    public LocalDateTime getCompletedDateTime() {
        return this.getShadowCompletedDateTime();
    }

    public LocalDateTime getShadowCompletedDateTime() {
        if (shadowProducingDateTime == null) {
            return null;
        }
        return shadowProducingDateTime.plus(getProducingDuration());
    }

    @Override
    public LocalDateTime getArrangeDateTime() {
        SchedulingDateTimeSlot dateTimeSlot = this.getPlanningDateTimeSlot();
        return dateTimeSlot != null
                ? dateTimeSlot.getStart()
                : null;
    }

    @Override
    public LocalDateTime getProducingDateTime() {
        return this.shadowProducingDateTime;
    }

}
