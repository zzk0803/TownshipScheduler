package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import zzk.townshipscheduler.backend.scheduling.model.utility.SchedulingDateTimeSlotStrengthComparator;
import zzk.townshipscheduler.backend.scheduling.model.utility.SchedulingProducingArrangementDifficultyComparator;
import zzk.townshipscheduler.backend.scheduling.model.utility.SchedulingProducingArrangementFactoryTypeQueueDateTimeVariableListener;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@PlanningEntity(difficultyComparatorClass = SchedulingProducingArrangementDifficultyComparator.class)
@JsonIgnoreProperties(allowGetters = true, allowSetters = true)
public class SchedulingProducingArrangementFactoryTypeQueue
        extends BaseSchedulingProducingArrangement
        implements ISchedulingFactoryOrFactoryArrangement {

    public static final String PLANNING_ANCHOR_FACTORY = "planningAnchorFactory";

    public static final String PLANNING_PREVIOUS = "planningPreviousProducingArrangementOrFactory";

    public static final String SHADOW_PRODUCING_DATE_TIME = "shadowProducingDateTime";

    public static final String SHADOW_FACTORY_ARRANGEMENT_SEQUENCE = "shadowFactoryArrangementSequence";

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
            variableListenerClass = SchedulingProducingArrangementFactoryTypeQueueDateTimeVariableListener.class
    )
    @ShadowVariable(
            sourceVariableName = PLANNING_PREVIOUS,
            variableListenerClass = SchedulingProducingArrangementFactoryTypeQueueDateTimeVariableListener.class
    )
    @ShadowVariable(
            sourceVariableName = PLANNING_DATA_TIME_SLOT,
            variableListenerClass = SchedulingProducingArrangementFactoryTypeQueueDateTimeVariableListener.class
    )
    private LocalDateTime shadowProducingDateTime;

    @PiggybackShadowVariable(shadowVariableName = SHADOW_PRODUCING_DATE_TIME)
    private Integer shadowFactoryArrangementSequence;

    public SchedulingProducingArrangementFactoryTypeQueue(
            IGameArrangeObject targetActionObject,
            IGameArrangeObject currentActionObject
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
    public LocalDateTime getArrangeDateTime() {
        SchedulingDateTimeSlot dateTimeSlot = this.getPlanningDateTimeSlot();
        return dateTimeSlot != null
                ? dateTimeSlot.getStart()
                : null;
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
    public LocalDateTime getProducingDateTime() {
        return this.shadowProducingDateTime;
    }

}
