package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public abstract class BaseProducingArrangement {

    public static final String PLANNING_FACTORY = "planningFactory";

    public static final String PLANNING_DATA_TIME_SLOT = "planningDateTimeSlot";

    public static final String SHADOW_PRODUCING_DATE_TIME = "shadowProducingDateTime";

    public static final String SHADOW_COMPLETED_DATE_TIME = "shadowCompletedDateTime";

    @EqualsAndHashCode.Include
    @ToString.Include
    protected Integer actionId;

    @EqualsAndHashCode.Include
    @ToString.Include
    @PlanningId
    protected String actionUuid;

    protected IGameActionObject targetActionObject;

    @ToString.Include
    protected IGameActionObject currentActionObject;

    protected List<BaseProducingArrangement> prerequisiteProducingArrangements = new ArrayList<>();

    protected List<BaseProducingArrangement> supportProducingArrangements = new ArrayList<>();

    protected SchedulingPlayer schedulingPlayer;

    protected SchedulingWorkTimeLimit schedulingWorkTimeLimit;

    protected SchedulingProducingExecutionMode producingExecutionMode;

    public BaseProducingArrangement(
            IGameActionObject targetActionObject,
            IGameActionObject currentActionObject
    ) {
        this.targetActionObject = targetActionObject;
        this.currentActionObject = currentActionObject;
    }

    public static SchedulingFactoryQueueProducingArrangement createProducingArrangementFactoryQueue(
            IGameActionObject targetActionObject,
            IGameActionObject currentActionObject
    ) {
        SchedulingFactoryQueueProducingArrangement producingArrangement = new SchedulingFactoryQueueProducingArrangement(
                targetActionObject,
                currentActionObject
        );
        producingArrangement.setActionUuid(UUID.randomUUID().toString());
        return producingArrangement;
    }

    public static SchedulingFactorySlotProducingArrangement createProducingArrangementFactorySlot(
            IGameActionObject targetActionObject,
            IGameActionObject currentActionObject
    ) {
        SchedulingFactorySlotProducingArrangement producingArrangement = new SchedulingFactorySlotProducingArrangement(
                targetActionObject,
                currentActionObject
        );
        producingArrangement.setActionUuid(UUID.randomUUID().toString());
        return producingArrangement;
    }

    public SchedulingFactoryInfo requiredFactoryInfo() {
        return getSchedulingProduct().getRequireFactory();
    }

    public SchedulingProduct getSchedulingProduct() {
        return (SchedulingProduct) getCurrentActionObject();
    }

    public void readyElseThrow() {
        Objects.requireNonNull(this.getCurrentActionObject());
        Objects.requireNonNull(getActionId());
        Objects.requireNonNull(getActionUuid());
    }

    public void activate(
            ActionIdRoller idRoller,
            SchedulingWorkTimeLimit workTimeLimit,
            SchedulingPlayer schedulingPlayer
    ) {
        idRoller.setup(this);
        this.schedulingWorkTimeLimit = workTimeLimit;
        this.schedulingPlayer = schedulingPlayer;
    }

    public String getHumanReadable() {
        return getCurrentActionObject().readable();
    }

    public ProductAmountBill getMaterials() {
        return getProducingExecutionMode().getMaterials();
    }

    public abstract List<ActionConsequence> calcConsequence();

    public Duration getProducingDuration() {
        return getProducingExecutionMode().getExecuteDuration();
    }

    @ToString.Include
    public abstract LocalDateTime getPlanningDateTimeSlotStartAsLocalDateTime();

    public abstract BaseSchedulingFactoryInstance getPlanningFactoryInstance();

    public boolean boolEquivalent(BaseProducingArrangement that) {
        SchedulingProduct thisProducing = this.getSchedulingProduct();
        SchedulingProduct thatProducing = that.getSchedulingProduct();
        if (thisProducing == null || thatProducing == null) {
            return false;
        } else {
            return thisProducing.equals(thatProducing);
        }
    }

    public boolean boolCompositeProductProducing() {
        return getProducingExecutionMode().boolCompositeProduct();
    }

    public <T extends BaseProducingArrangement> void appendPrerequisiteArrangements(List<T> prerequisiteArrangements) {
        this.prerequisiteProducingArrangements.addAll(prerequisiteArrangements);
    }

    public <T extends BaseProducingArrangement> void appendSupportArrangements(List<T> supportProducingArrangements) {
        this.supportProducingArrangements.addAll(supportProducingArrangements);
    }

    @ToString.Include
    public abstract LocalDateTime getProducingDateTime();

    @ToString.Include
    public abstract LocalDateTime getCompletedDateTime();

}
