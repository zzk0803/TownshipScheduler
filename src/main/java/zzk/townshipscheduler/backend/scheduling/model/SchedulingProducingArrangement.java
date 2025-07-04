package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.solution.cloner.DeepPlanningClone;
import ai.timefold.solver.core.api.domain.variable.PiggybackShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import zzk.townshipscheduler.backend.ProducingStructureType;
import zzk.townshipscheduler.backend.scheduling.model.utility.SchedulingDateTimeSlotStrengthComparator;
import zzk.townshipscheduler.backend.scheduling.model.utility.SchedulingProducingArrangementDifficultyComparator;
import zzk.townshipscheduler.backend.scheduling.model.utility.SchedulingProducingArrangementFactorySequenceVariableListener;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;


@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@PlanningEntity(difficultyComparatorClass = SchedulingProducingArrangementDifficultyComparator.class)
public class SchedulingProducingArrangement {

//    public static final String VALUE_RANGE_FOR_FACTORIES_IN_ARRANGEMENT = "valueRangeForFactoriesInArrangement";

    public static final String PLANNING_DATA_TIME_SLOT = "planningDateTimeSlot";

    public static final String PLANNING_FACTORY_INSTANCE = "planningFactoryInstance";

    public static final String SHADOW_FACTORY_PROCESS_SEQUENCE = "shadowFactoryProcessSequence";

    public static final String SHADOW_PRODUCING_DATE_TIME = "computedShadowProducingDateTime";

    public static final String SHADOW_COMPLETED_DATE_TIME = "computedShadowCompletedDateTime";

    public static final String SHADOW_COMPUTED_DATE_TIME_PAIR = "factoryComputedDataTimePair";

    @EqualsAndHashCode.Include
    @ToString.Include
    private Integer id;

    @EqualsAndHashCode.Include
    @ToString.Include
    @PlanningId
    private String uuid;

    @JsonIgnore
    private IGameArrangeObject targetActionObject;

    @JsonIgnore
    @ToString.Include
    private IGameArrangeObject currentActionObject;

    @JsonIgnore
    @DeepPlanningClone
    private List<SchedulingProducingArrangement> prerequisiteProducingArrangements = new ArrayList<>();

    @JsonIgnore
    @DeepPlanningClone
    private List<SchedulingProducingArrangement> deepPrerequisiteProducingArrangements = new ArrayList<>();

    @JsonIgnore
    private SchedulingPlayer schedulingPlayer;

    @JsonIgnore
    private SchedulingWorkCalendar schedulingWorkCalendar;

    @JsonIgnore
    private SchedulingProducingExecutionMode producingExecutionMode;

    @JsonIgnore
    @PlanningVariable(valueRangeProviderRefs = {TownshipSchedulingProblem.VALUE_RANGE_FOR_FACTORIES})
    private SchedulingFactoryInstance planningFactoryInstance;

    @JsonIgnore
    @PlanningVariable(
            valueRangeProviderRefs = {TownshipSchedulingProblem.VALUE_RANGE_FOR_DATE_TIME_SLOT},
            strengthComparatorClass = SchedulingDateTimeSlotStrengthComparator.class
    )
    private SchedulingDateTimeSlot planningDateTimeSlot;

    @JsonIgnore
    @ShadowVariable(
            sourceVariableName = PLANNING_FACTORY_INSTANCE,
            variableListenerClass = SchedulingProducingArrangementFactorySequenceVariableListener.class
    )
    @ShadowVariable(
            sourceVariableName = PLANNING_DATA_TIME_SLOT,
            variableListenerClass = SchedulingProducingArrangementFactorySequenceVariableListener.class
    )
    private FactoryProcessSequence shadowFactoryProcessSequence;

    @PiggybackShadowVariable(shadowVariableName = SHADOW_FACTORY_PROCESS_SEQUENCE)
    private FactoryComputedDataTimePair factoryComputedDataTimePair;

    public SchedulingProducingArrangement(
            IGameArrangeObject targetActionObject,
            IGameArrangeObject currentActionObject
    ) {
        this.targetActionObject = targetActionObject;
        this.currentActionObject = currentActionObject;
    }

    public static SchedulingProducingArrangement createProducingArrangement(
            IGameArrangeObject targetActionObject,
            IGameArrangeObject currentActionObject
    ) {
        SchedulingProducingArrangement producingArrangement = new SchedulingProducingArrangement(
                targetActionObject,
                currentActionObject
        );
        producingArrangement.setUuid(UUID.randomUUID().toString());
        return producingArrangement;
    }

    @JsonIgnore
    public boolean isFactoryMatch() {
        return Objects.nonNull(getPlanningFactoryInstance())
               && getPlanningFactoryInstance().getSchedulingFactoryInfo()
                       .typeEqual(getSchedulingProduct().getRequireFactory());
    }

    @JsonProperty("schedulingProduct")
    public SchedulingProduct getSchedulingProduct() {
        return (SchedulingProduct) getCurrentActionObject();
    }

    public boolean isPlanningAssigned() {
        return getPlanningDateTimeSlot() != null && getPlanningFactoryInstance() != null;
    }

    @JsonIgnore
    public SchedulingOrder getSchedulingOrder() {
        IGameArrangeObject iGameArrangeObject = getTargetActionObject();
        if (iGameArrangeObject instanceof SchedulingOrder) {
            return ((SchedulingOrder) iGameArrangeObject);
        } else {
            return null;
        }
    }

    public void readyElseThrow() {
        Objects.requireNonNull(this.getCurrentActionObject());
        Objects.requireNonNull(getId());
        Objects.requireNonNull(getUuid());
        Objects.requireNonNull(getSchedulingPlayer());
        Objects.requireNonNull(getSchedulingWorkCalendar());
        setDeepPrerequisiteProducingArrangements(calcDeepPrerequisiteProducingArrangements());
    }

    public void activate(
            ArrangementIdRoller idRoller,
            SchedulingWorkCalendar workTimeLimit,
            SchedulingPlayer schedulingPlayer
    ) {
        idRoller.setup(this);
        this.schedulingWorkCalendar = workTimeLimit;
        this.schedulingPlayer = schedulingPlayer;
    }

    @JsonIgnore
    public String getHumanReadable() {
        return getCurrentActionObject().readable();
    }

    @JsonIgnore
    public ProductAmountBill getMaterials() {
        return getProducingExecutionMode().getMaterials();
    }

    //<editor-fold desc="DEPRECATED">
    public List<ProducingArrangementConsequence> calcConsequence() {
        List<ProducingArrangementConsequence> producingArrangementConsequenceList = new ArrayList<>(5);
        SchedulingProducingArrangement producingArrangement = this;
        SchedulingProducingExecutionMode executionMode = producingArrangement.getProducingExecutionMode();
        //when arrange,materials was consumed
        if (executionMode.boolCompositeProduct()) {
            ProductAmountBill materials = executionMode.getMaterials();
            materials.forEach((material, amount) -> {
                ProducingArrangementConsequence consequence = ProducingArrangementConsequence.builder()
                        .producingArrangement(producingArrangement)
                        .resource(ProducingArrangementConsequence.SchedulingResource.productStock(material))
                        .resourceChange(ProducingArrangementConsequence.SchedulingResourceChange.decrease(amount))
                        .build();
                producingArrangementConsequenceList.add(consequence);
            });
        }

        //when arrange,factory wait queue was consumed
        producingArrangementConsequenceList.add(
                ProducingArrangementConsequence.builder()
                        .producingArrangement(producingArrangement)
                        .resource(
                                ProducingArrangementConsequence.SchedulingResource.factoryWaitQueue(
                                        getPlanningFactoryInstance()
                                )
                        )
                        .resourceChange(ProducingArrangementConsequence.SchedulingResourceChange.decrease())
                        .build()
        );

        //when completed ,factory wait queue was release
        producingArrangementConsequenceList.add(
                ProducingArrangementConsequence.builder()
                        .producingArrangement(producingArrangement)
                        .resource(ProducingArrangementConsequence.SchedulingResource.factoryWaitQueue(
                                        getPlanningFactoryInstance()
                                )
                        )
                        .resourceChange(ProducingArrangementConsequence.SchedulingResourceChange.increase())
                        .build()
        );

        //when completed ,product stock was increase
        producingArrangementConsequenceList.add(
                ProducingArrangementConsequence.builder()
                        .producingArrangement(producingArrangement)
                        .resource(ProducingArrangementConsequence.SchedulingResource.productStock(getSchedulingProduct()))
                        .resourceChange(ProducingArrangementConsequence.SchedulingResourceChange.increase())
                        .build()
        );


        return producingArrangementConsequenceList;
    }
    //</editor-fold>

    @JsonProperty("completedDateTime")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ToString.Include
    public LocalDateTime getCompletedDateTime() {
        FactoryComputedDataTimePair computedDataTimePair = getFactoryComputedDataTimePair();
        if (computedDataTimePair == null) {
            return null;
        } else {
            return computedDataTimePair.completedDateTime();
        }
    }

    @JsonProperty("producingDateTime")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ToString.Include
    public LocalDateTime getProducingDateTime() {
        FactoryComputedDataTimePair computedDataTimePair = getFactoryComputedDataTimePair();
        if (computedDataTimePair == null) {
            return null;
        } else {
            return computedDataTimePair.producingDateTime();
        }
    }

    @JsonProperty("producingDuration")
    public Duration getProducingDuration() {
        return getProducingExecutionMode().getExecuteDuration();
    }

    public boolean weatherFactoryProducingTypeIsQueue() {
        return getFactoryProducingType() == ProducingStructureType.QUEUE;
    }

    public ProducingStructureType getFactoryProducingType() {
        return getRequiredFactoryInfo().getProducingStructureType();
    }

    @JsonIgnore
    public SchedulingFactoryInfo getRequiredFactoryInfo() {
        return getSchedulingProduct().getRequireFactory();
    }

    @JsonProperty("arrangeDateTime")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ToString.Include
    public LocalDateTime getArrangeDateTime() {
        SchedulingDateTimeSlot dateTimeSlot = getPlanningDateTimeSlot();
        return dateTimeSlot != null ? dateTimeSlot.getStart() : null;
    }

    public List<SchedulingProducingArrangement> calcDeepPrerequisiteProducingArrangements() {
        LinkedList<SchedulingProducingArrangement> queue = new LinkedList<>(List.of(this));
        Set<SchedulingProducingArrangement> visited = new HashSet<>();
        List<SchedulingProducingArrangement> result = new ArrayList<>();

        while (!queue.isEmpty()) {
            SchedulingProducingArrangement current = queue.removeFirst();
            if (!visited.add(current)) {
                continue;
            }

            List<SchedulingProducingArrangement> prerequisites =
                    current.getPrerequisiteProducingArrangements();
            if (prerequisites != null) {
                for (SchedulingProducingArrangement iteratingSingleArrangement : prerequisites) {
                    if (iteratingSingleArrangement != null) {
                        result.add(iteratingSingleArrangement);
                        queue.add(iteratingSingleArrangement);
                    }
                }
            }
        }

        return result;
    }

    public <T extends SchedulingProducingArrangement> void appendPrerequisiteArrangements(List<T> prerequisiteArrangements) {
        this.prerequisiteProducingArrangements.addAll(prerequisiteArrangements);
    }

    public boolean isOrderDirect() {
        return getTargetActionObject() instanceof SchedulingOrder;
    }

}
