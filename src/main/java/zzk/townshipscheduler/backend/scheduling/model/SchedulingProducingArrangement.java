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
import zzk.townshipscheduler.backend.scheduling.model.utility.SchedulingProducingArrangementDifficultyComparator;
import zzk.townshipscheduler.backend.scheduling.model.utility.SchedulingProducingArrangementLocalDateTimeVariableListener;
import zzk.townshipscheduler.backend.scheduling.model.utility.SchedulingProducingArrangementVariableListener;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;


@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@PlanningEntity(difficultyComparatorClass = SchedulingProducingArrangementDifficultyComparator.class)
public class SchedulingProducingArrangement {

    public static final String PLANNING_DATA_TIME_SLOT = "planningDateTimeSlot";

    public static final String PLANNING_FACTORY_INSTANCE = "planningFactoryInstance";

    public static final String SHADOW_FACTORY_PROCESS_SEQUENCE = "shadowFactoryProcessSequence";

    public static final String SHADOW_PRODUCING_DATE_TIME = "computedShadowProducingDateTime";

    public static final String SHADOW_COMPLETED_DATE_TIME = "computedShadowCompletedDateTime";

    @EqualsAndHashCode.Include
    @ToString.Include
    protected Integer id;

    @EqualsAndHashCode.Include
    @ToString.Include
    @PlanningId
    protected String uuid;

    @JsonIgnore
    protected IGameArrangeObject targetActionObject;

    @JsonIgnore
    @ToString.Include
    protected IGameArrangeObject currentActionObject;

    @JsonIgnore
    @DeepPlanningClone
    protected List<SchedulingProducingArrangement> prerequisiteProducingArrangements = new ArrayList<>();

    @JsonIgnore
    protected SchedulingPlayer schedulingPlayer;

    @JsonIgnore
    protected SchedulingWorkCalendar schedulingWorkCalendar;

    @JsonIgnore
    protected SchedulingProducingExecutionMode producingExecutionMode;

    @JsonIgnore
    @PlanningVariable(valueRangeProviderRefs = {TownshipSchedulingProblem.VALUE_RANGE_FOR_FACTORIES})
    private SchedulingFactoryInstance planningFactoryInstance;

    @JsonIgnore
    @PlanningVariable(valueRangeProviderRefs = {TownshipSchedulingProblem.VALUE_RANGE_FOR_DATE_TIME_SLOT})
    private SchedulingDateTimeSlot planningDateTimeSlot;

    @JsonIgnore
    @ShadowVariable(
            sourceVariableName = PLANNING_FACTORY_INSTANCE,
            variableListenerClass = SchedulingProducingArrangementVariableListener.class
    )
    @ShadowVariable(
            sourceVariableName = PLANNING_DATA_TIME_SLOT,
            variableListenerClass = SchedulingProducingArrangementVariableListener.class
    )
    private SchedulingDateTimeSlot.FactoryProcessSequence shadowFactoryProcessSequence;

    @ShadowVariable(
            sourceVariableName = PLANNING_FACTORY_INSTANCE,
            variableListenerClass = SchedulingProducingArrangementVariableListener.class
    )
    @ShadowVariable(
            sourceVariableName = SHADOW_FACTORY_PROCESS_SEQUENCE,
            variableListenerClass = SchedulingProducingArrangementLocalDateTimeVariableListener.class
    )
    private LocalDateTime computedShadowProducingDateTime;

    @PiggybackShadowVariable(shadowVariableName = SHADOW_PRODUCING_DATE_TIME)
    private LocalDateTime computedShadowCompletedDateTime;

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
        return ((SchedulingOrder) getTargetActionObject());
    }

    public void readyElseThrow() {
        Objects.requireNonNull(this.getCurrentActionObject());
        Objects.requireNonNull(getId());
        Objects.requireNonNull(getUuid());
        Objects.requireNonNull(getSchedulingPlayer());
        Objects.requireNonNull(getSchedulingWorkCalendar());
    }

    public void activate(
            ActionIdRoller idRoller,
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

    public List<ProducingArrangementConsequence> calcConsequence() {
        List<ProducingArrangementConsequence> producingArrangementConsequenceList = new ArrayList<>(5);
        SchedulingProducingExecutionMode executionMode = getProducingExecutionMode();
        //when arrange,materials was consumed
        if (executionMode.boolCompositeProduct()) {
            ProductAmountBill materials = executionMode.getMaterials();
            materials.forEach((material, amount) -> {
                ProducingArrangementConsequence consequence = ProducingArrangementConsequence.builder()
                        .producingArrangement(this)
                        .resource(ProducingArrangementConsequence.SchedulingResource.productStock(material))
                        .resourceChange(ProducingArrangementConsequence.SchedulingResourceChange.decrease(amount))
                        .build();
                producingArrangementConsequenceList.add(consequence);
            });
        }

        //when arrange,factory wait queue was consumed
        producingArrangementConsequenceList.add(
                ProducingArrangementConsequence.builder()
                        .producingArrangement(this)
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
                        .producingArrangement(this)
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
                        .producingArrangement(this)
                        .resource(ProducingArrangementConsequence.SchedulingResource.productStock(getSchedulingProduct()))
                        .resourceChange(ProducingArrangementConsequence.SchedulingResourceChange.increase())
                        .build()
        );


        return producingArrangementConsequenceList;
    }

    @JsonProperty("completedDateTime")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ToString.Include
    public LocalDateTime getCompletedDateTime() {
        return getComputedShadowCompletedDateTime();
//        LocalDateTime producingDateTime = getProducingDateTime();
//
//        return producingDateTime != null
//                ? producingDateTime.plus(getProducingDuration())
//                : null;
    }

    @JsonProperty("producingDateTime")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ToString.Include
    public LocalDateTime getProducingDateTime() {
        return getComputedShadowProducingDateTime();
//        if (weatherFactoryProducingTypeIsQueue()) {
//            SchedulingFactoryInstance factoryInstance
//                    = getPlanningFactoryInstance();
//            return factoryInstance == null
//                    ? null
//                    : factoryInstance.queryProducingAndCompletedPair(this).getValue0();
//        } else {
//            return getArrangeDateTime();
//        }
    }

    @JsonProperty("producingDuration")
    public Duration getProducingDuration() {
        return getProducingExecutionMode().getExecuteDuration();
    }

    public boolean weatherFactoryProducingTypeIsQueue() {
        return getFactoryProducingType() == ProducingStructureType.QUEUE;
    }

    @JsonProperty("arrangeDateTime")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ToString.Include
    public LocalDateTime getArrangeDateTime() {
        SchedulingDateTimeSlot dateTimeSlot = getPlanningDateTimeSlot();
        return dateTimeSlot != null ? dateTimeSlot.getStart() : null;
    }

    public ProducingStructureType getFactoryProducingType() {
        return getRequiredFactoryInfo().getProducingStructureType();
    }

    @JsonIgnore
    public SchedulingFactoryInfo getRequiredFactoryInfo() {
        return getSchedulingProduct().getRequireFactory();
    }

    public List<SchedulingProducingArrangement> getDeepPrerequisiteProducingArrangements() {
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
