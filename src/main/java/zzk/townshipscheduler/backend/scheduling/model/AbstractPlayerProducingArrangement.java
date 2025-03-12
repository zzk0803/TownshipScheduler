package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import zzk.townshipscheduler.backend.scheduling.model.utility.FactoryActionDifficultyComparator;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@PlanningEntity(difficultyComparatorClass = FactoryActionDifficultyComparator.class)
public abstract class AbstractPlayerProducingArrangement implements IActionSensitive {

    public static final String PLANNING_DATA_TIME_SLOT = "planningDateTimeSlot";

    public static final String PLANNING_SEQUENCE = "planningSequence";

    public static final String SHADOW_PRODUCING_DATE_TIME = "shadowGameProducingDataTime";

    public static final String SHADOW_COMPLETED_DATE_TIME = "shadowGameCompleteDateTime";

    @EqualsAndHashCode.Include
    @ToString.Include
    @PlanningId
    protected Integer actionId;

    @EqualsAndHashCode.Include
    protected String actionUuid;

    protected IGameActionObject targetActionObject;

    @ToString.Include
    protected IGameActionObject currentActionObject;

    protected SchedulingWorkTimeLimit workTimeLimit;

    protected SchedulingWarehouse schedulingWarehouse;

    protected SchedulingProducingExecutionMode producingExecutionMode;

    @PlanningVariable(
            valueRangeProviderRefs = TownshipSchedulingProblem.SEQUENCE_VALUE_RANGE
    )
    protected Integer planningSequence;

    @PlanningVariable(
            valueRangeProviderRefs = TownshipSchedulingProblem.DATE_TIME_SLOT_VALUE_RANGE
    )
    protected SchedulingDateTimeSlot planningDateTimeSlot;

    protected LocalDateTime shadowGameProducingDataTime;

    protected LocalDateTime shadowGameCompleteDateTime;

    public AbstractPlayerProducingArrangement(
            IGameActionObject targetActionObject,
            IGameActionObject currentActionObject
    ) {
        this();
        this.targetActionObject = targetActionObject;
        this.currentActionObject = currentActionObject;
    }

    public AbstractPlayerProducingArrangement() {
        this.actionUuid = UUID.randomUUID().toString();
    }

    public static SchedulingPlayerProducingArrangement schedulingPlayerProducingArrangement(
            IGameActionObject targetActionObject,
            IGameActionObject currentActionObject,
            SchedulingFactoryInstanceSingle factoryInstance
    ) {
        return new SchedulingPlayerProducingArrangement(targetActionObject, currentActionObject, factoryInstance);
    }

    public static SchedulingPlayerFactoryProducingArrangement schedulingPlayerProducingFactoryArrangement(
            IGameActionObject targetActionObject,
            IGameActionObject currentActionObject
    ) {
        return new SchedulingPlayerFactoryProducingArrangement(targetActionObject, currentActionObject);
    }

    public final LocalDateTime getShadowGameCompleteDateTime() {
        return this.shadowGameProducingDataTime == null
                ? null
                : this.shadowGameProducingDataTime.plus(getProducingDuration());
    }

    public Duration getProducingDuration() {
        return getProducingExecutionMode().getExecuteDuration();
    }

    public void readyElseThrow() {
        Objects.requireNonNull(this.getCurrentActionObject());
        Objects.requireNonNull(getActionId());
    }

    public void activate(
            ActionIdRoller idRoller,
            SchedulingWorkTimeLimit workTimeLimit,
            SchedulingWarehouse schedulingWarehouse
    ) {
        idRoller.setup(this);
        this.workTimeLimit = workTimeLimit;
        this.schedulingWarehouse = schedulingWarehouse;
    }

    public String getHumanReadable() {
        return getCurrentActionObject().readable();
    }

    public ProductAmountBill getMaterials() {
        return getProducingExecutionMode().getMaterials();
    }

    public abstract List<ActionConsequence> calcConsequence();

    public LocalDateTime getPlanningDateTimeSlotStartAsLocalDateTime() {
        SchedulingDateTimeSlot dateTimeSlot = this.getPlanningDateTimeSlot();
        return dateTimeSlot != null
                ? dateTimeSlot.getStart()
                : null;
    }

    public boolean boolEquivalent(SchedulingPlayerProducingArrangement that) {
        SchedulingProduct thisProducing = this.getSchedulingProduct();
        SchedulingProduct thatProducing = that.getSchedulingProduct();
        if (thisProducing == null || thatProducing == null) {
            return false;
        } else {
            return thisProducing.equals(thatProducing);
        }
    }

    public SchedulingProduct getSchedulingProduct() {
        return (SchedulingProduct) getCurrentActionObject();
    }

    public abstract AbstractFactoryInstance getFactory();

}
