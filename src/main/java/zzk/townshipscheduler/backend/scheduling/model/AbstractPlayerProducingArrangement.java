package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.context.ApplicationEventPublisherAware;
import zzk.townshipscheduler.backend.ProducingStructureType;
import zzk.townshipscheduler.backend.scheduling.model.utility.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@PlanningEntity(difficultyComparatorClass = PlayerArrangementDifficultyComparator.class)
public abstract class AbstractPlayerProducingArrangement {

    public static final String PLANNING_DATA_TIME_SLOT = "planningDateTimeSlot";

    public static final String PLANNING_SEQUENCE = "planningSequence";

    public static final String SHADOW_PREVIOUS = "shadowPreviousArrangements";

    public static final String SHADOW_DELAY_DURATION = "shadowDelayFromPlanningSlotDuration";

    @EqualsAndHashCode.Include
    @ToString.Include
    @PlanningId
    protected Integer actionId;

    @EqualsAndHashCode.Include
    protected String actionUuid;

    protected IGameActionObject targetActionObject;

    @ToString.Include
    protected IGameActionObject currentActionObject;

    protected List<AbstractPlayerProducingArrangement> prerequisiteProducingArrangements = new ArrayList<>();

    protected List<AbstractPlayerProducingArrangement> supportProducingArrangements = new ArrayList<>();

    protected SchedulingWorkTimeLimit workTimeLimit;

    protected SchedulingPlayer schedulingPlayer;

    protected SchedulingProducingExecutionMode producingExecutionMode;

    @PlanningVariable(
            valueRangeProviderRefs = TownshipSchedulingProblem.SEQUENCE_VALUE_RANGE,
            strengthComparatorClass = ArrangeSequenceStrengthComparatorClass.class
    )
    protected SchedulingFactoryInfo.ArrangeSequence planningSequence;

    @PlanningVariable(
            valueRangeProviderRefs = TownshipSchedulingProblem.DATE_TIME_SLOT_VALUE_RANGE,
            strengthComparatorClass = SchedulingDateTimeSlotStrengthComparator.class
    )
    protected SchedulingDateTimeSlot planningDateTimeSlot;

    @ShadowVariable(
            variableListenerClass = ProducingArrangementPreviousArrangementsVariableListener.class,
            sourceVariableName = PLANNING_SEQUENCE
    )
    @ShadowVariable(
            variableListenerClass = ProducingArrangementPreviousArrangementsVariableListener.class,
            sourceVariableName = PLANNING_DATA_TIME_SLOT
    )
    protected List<AbstractPlayerProducingArrangement> shadowPreviousArrangements;

    @ShadowVariable(
            variableListenerClass = ProducingArrangementComputedDateTimeVariableListener.class,
            sourceVariableName = SHADOW_PREVIOUS
    )
    protected Duration shadowDelayFromPlanningSlotDuration = Duration.ZERO;

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

    @ValueRangeProvider(id = TownshipSchedulingProblem.SEQUENCE_VALUE_RANGE)
    public List<SchedulingFactoryInfo.ArrangeSequence> arrangeSequencesValueRange() {
        return getRequiredFactoryInfo().toArrangeSequenceValueRange();
    }

    public <T extends AbstractPlayerProducingArrangement> void setupPreviousArrangements(List<T> arrangements) {
        shadowPreviousArrangements = new ArrayList<>(arrangements);
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
        this.workTimeLimit = workTimeLimit;
        this.schedulingPlayer = schedulingPlayer;
    }

    public String getHumanReadable() {
        return getCurrentActionObject().readable();
    }

    public ProductAmountBill getMaterials() {
        return getProducingExecutionMode().getMaterials();
    }

    public List<ActionConsequence> calcConsequence() {
        if (
                getPlanningDateTimeSlot() == null
                || getFactory() == null
                || getPlanningSequence() == null
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

    public abstract AbstractFactoryInstance getFactory();

    public LocalDateTime getPlanningDateTimeSlotStartAsLocalDateTime() {
        SchedulingDateTimeSlot dateTimeSlot = this.getPlanningDateTimeSlot();
        return dateTimeSlot != null
                ? dateTimeSlot.getStart()
                : null;
    }

    public final LocalDateTime getShadowGameCompleteDateTime() {
        return this.getShadowGameProducingDateTime() == null
                ? null
                : this.getShadowGameProducingDateTime().plus(getProducingDuration());
    }

    public final LocalDateTime getShadowGameProducingDateTime() {
        LocalDateTime localDateTime = null;
        if (this.getPlanningDateTimeSlot() != null && this.getPlanningSequence() != null) {
            localDateTime
                    = this.getFactory().getProducingStructureType() == ProducingStructureType.SLOT
                    ? this.getPlanningDateTimeSlotStartAsLocalDateTime()
                    : this.getPlanningDateTimeSlotStartAsLocalDateTime()
                            .plus(
                                    getShadowDelayFromPlanningSlotDuration() == null
                                            ? Duration.ZERO
                                            : getShadowDelayFromPlanningSlotDuration()
                            );
        }
        return localDateTime;
    }

    public Duration getProducingDuration() {
        return getProducingExecutionMode().getExecuteDuration();
    }

    public SchedulingProduct getSchedulingProduct() {
        return (SchedulingProduct) getCurrentActionObject();
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

    public SchedulingFactoryInfo getRequiredFactoryInfo() {
        return getSchedulingProduct().getRequireFactory();
    }

    public boolean boolCompositeProductProducing() {
        return getProducingExecutionMode().boolCompositeProduct();
    }

    public void appendPrerequisiteArrangements(List<AbstractPlayerProducingArrangement> prerequisiteArrangements) {
        this.prerequisiteProducingArrangements.addAll(prerequisiteArrangements);
    }

    public void appendSupportArrangements(List<AbstractPlayerProducingArrangement> supportProducingArrangements) {
        this.supportProducingArrangements.addAll(supportProducingArrangements);
    }

}
