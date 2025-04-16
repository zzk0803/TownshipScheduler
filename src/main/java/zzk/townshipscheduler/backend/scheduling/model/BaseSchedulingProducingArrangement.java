package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.solution.cloner.DeepPlanningClone;
import com.fasterxml.jackson.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;


@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@JsonSubTypes(
        value = {
                @JsonSubTypes.Type(SchedulingProducingArrangementFactoryTypeQueue.class),
                @JsonSubTypes.Type(SchedulingProducingArrangementFactoryTypeSlot.class)
        }
)
public abstract class BaseSchedulingProducingArrangement {

    public static final String PLANNING_DATA_TIME_SLOT = "planningDateTimeSlot";

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
    protected List<BaseSchedulingProducingArrangement> prerequisiteProducingArrangements = new ArrayList<>();

    @JsonIgnore
    protected SchedulingPlayer schedulingPlayer;

    @JsonIgnore
    protected SchedulingWorkTimeLimit schedulingWorkTimeLimit;

    @JsonIgnore
    protected SchedulingProducingExecutionMode producingExecutionMode;

    @JsonIgnore
    private int arrangePathLevel = 1;

    public BaseSchedulingProducingArrangement(
            IGameArrangeObject targetActionObject,
            IGameArrangeObject currentActionObject
    ) {
        this.targetActionObject = targetActionObject;
        this.currentActionObject = currentActionObject;
    }

    public static SchedulingProducingArrangementFactoryTypeQueue createProducingArrangementFactoryQueue(
            IGameArrangeObject targetActionObject,
            IGameArrangeObject currentActionObject
    ) {
        SchedulingProducingArrangementFactoryTypeQueue producingArrangement
                = new SchedulingProducingArrangementFactoryTypeQueue(
                targetActionObject,
                currentActionObject
        );
        producingArrangement.setUuid(UUID.randomUUID().toString());
        return producingArrangement;
    }

    public static SchedulingProducingArrangementFactoryTypeSlot createProducingArrangementFactorySlot(
            IGameArrangeObject targetActionObject,
            IGameArrangeObject currentActionObject
    ) {
        SchedulingProducingArrangementFactoryTypeSlot producingArrangement = new SchedulingProducingArrangementFactoryTypeSlot(
                targetActionObject,
                currentActionObject
        );
        producingArrangement.setUuid(UUID.randomUUID().toString());
        return producingArrangement;
    }

    @JsonIgnore
    public SchedulingFactoryInfo getRequiredFactoryInfo() {
        return getSchedulingProduct().getRequireFactory();
    }

    @JsonProperty("schedulingProduct")
    public SchedulingProduct getSchedulingProduct() {
        return (SchedulingProduct) getCurrentActionObject();
    }

    @JsonIgnore
    public boolean isFactoryMatch() {
        return Objects.nonNull(getPlanningFactoryInstance())
               && getPlanningFactoryInstance().getSchedulingFactoryInfo()
                       .typeEqual(getSchedulingProduct().getRequireFactory());
    }

    @JsonProperty("schedulingFactory")
    @JsonManagedReference
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public abstract BaseSchedulingFactoryInstance getPlanningFactoryInstance();

    @JsonIgnore
    public SchedulingOrder getSchedulingOrder() {
        return ((SchedulingOrder) getTargetActionObject());
    }

    public void readyElseThrow() {
        Objects.requireNonNull(this.getCurrentActionObject());
        Objects.requireNonNull(getId());
        Objects.requireNonNull(getUuid());
        Objects.requireNonNull(getSchedulingPlayer());
        Objects.requireNonNull(getSchedulingWorkTimeLimit());
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

    @JsonIgnore
    public String getHumanReadable() {
        return getCurrentActionObject().readable();
    }

    @JsonIgnore
    public ProductAmountBill getMaterials() {
        return getProducingExecutionMode().getMaterials();
    }

    public List<ArrangeConsequence> calcConsequence() {
        if (
                getArrangeDateTime() == null
                || getPlanningFactoryInstance() == null
                || getCompletedDateTime() == null
        ) {
            return List.of();
        }

        List<ArrangeConsequence> arrangeConsequenceList = new ArrayList<>(5);
        SchedulingProducingExecutionMode executionMode = getProducingExecutionMode();
        //when arrange,materials was consumed
        if (executionMode.boolCompositeProduct()) {
            ProductAmountBill materials = executionMode.getMaterials();
            materials.forEach((material, amount) -> {
                ArrangeConsequence consequence = ArrangeConsequence.builder()
                        .producingArrangement(this)
                        .localDateTime(getArrangeDateTime())
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
                        .localDateTime(getArrangeDateTime())
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

    @JsonProperty("arrangeDateTime")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ToString.Include
    public abstract LocalDateTime getArrangeDateTime();

    @JsonProperty("completedDateTime")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ToString.Include
    public abstract LocalDateTime getCompletedDateTime();

    @JsonIgnore
    public abstract SchedulingDateTimeSlot getPlanningDateTimeSlot();

    public abstract void setPlanningDateTimeSlot(SchedulingDateTimeSlot computedDataTimeSlot);

    public List<BaseSchedulingProducingArrangement> getDeepPrerequisiteProducingArrangements() {
        LinkedList<BaseSchedulingProducingArrangement> queue = new LinkedList<>(List.of(this));
        Set<BaseSchedulingProducingArrangement> visited = new HashSet<>();
        List<BaseSchedulingProducingArrangement> result = new ArrayList<>();

        while (!queue.isEmpty()) {
            BaseSchedulingProducingArrangement current = queue.removeFirst();

            if (!visited.add(current)) {
                continue;
            }

            List<BaseSchedulingProducingArrangement> prerequisites =
                    current.getPrerequisiteProducingArrangements();

            if (prerequisites != null) {
                for (BaseSchedulingProducingArrangement iteratingSingleArrangement : prerequisites) {
                    if (iteratingSingleArrangement != null) {
                        result.add(iteratingSingleArrangement);
                        queue.add(iteratingSingleArrangement);
                    }
                }
            }
        }

        return result;
    }

    @JsonProperty("producingDuration")
    public Duration getProducingDuration() {
        return getProducingExecutionMode().getExecuteDuration();
    }

    public <T extends BaseSchedulingProducingArrangement> void appendPrerequisiteArrangements(List<T> prerequisiteArrangements) {
        this.prerequisiteProducingArrangements.addAll(prerequisiteArrangements);
    }

    @JsonProperty("producingDateTime")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ToString.Include
    public abstract LocalDateTime getProducingDateTime();

    public boolean isOrderDirect() {
        return getTargetActionObject() instanceof SchedulingOrder;
    }

}
