package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import com.fasterxml.jackson.annotation.*;
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
    protected IGameActionObject targetActionObject;

    @JsonIgnore
    @ToString.Include
    protected IGameActionObject currentActionObject;

    @JsonIgnore
    protected boolean orderDirect;

    @JsonIgnore
    protected List<BaseSchedulingProducingArrangement> prerequisiteProducingArrangements = new ArrayList<>();

    @JsonIgnore
    protected List<BaseSchedulingProducingArrangement> supportProducingArrangements = new ArrayList<>();

    @JsonIgnore
    protected SchedulingPlayer schedulingPlayer;

    @JsonIgnore
    protected SchedulingWorkTimeLimit schedulingWorkTimeLimit;

    @JsonIgnore
    protected SchedulingProducingExecutionMode producingExecutionMode;

    public BaseSchedulingProducingArrangement(
            IGameActionObject targetActionObject,
            IGameActionObject currentActionObject
    ) {
        this.targetActionObject = targetActionObject;
        this.currentActionObject = currentActionObject;
    }

    public static SchedulingProducingArrangementFactoryTypeQueue createProducingArrangementFactoryQueue(
            IGameActionObject targetActionObject,
            IGameActionObject currentActionObject
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
            IGameActionObject targetActionObject,
            IGameActionObject currentActionObject
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
    public SchedulingOrder getSchedulingOrder() {
        return ((SchedulingOrder) getTargetActionObject());
    }

    public <T extends IGameActionObject> T asGameObject(Class<T> gameObjectClass) {
        return gameObjectClass.cast(this);
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
                this.getPlanningDateTimeSlot() == null
                || this.getPlanningFactoryInstance() == null
                || this.getCompletedDateTime() == null
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

    @JsonIgnore
    public abstract SchedulingDateTimeSlot getPlanningDateTimeSlot();

    @JsonProperty("schedulingFactory")
    @JsonManagedReference
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public abstract BaseSchedulingFactoryInstance getPlanningFactoryInstance();

    @JsonProperty("completedDateTime")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ToString.Include
    public abstract LocalDateTime getCompletedDateTime();

    @JsonProperty("arrangeDateTime")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ToString.Include
    public abstract LocalDateTime getArrangeDateTime();

    @JsonProperty("producingDuration")
    public Duration getProducingDuration() {
        return getProducingExecutionMode().getExecuteDuration();
    }

    public <T extends BaseSchedulingProducingArrangement> void appendPrerequisiteArrangements(List<T> prerequisiteArrangements) {
        this.prerequisiteProducingArrangements.addAll(prerequisiteArrangements);
    }

    public <T extends BaseSchedulingProducingArrangement> void appendSupportArrangements(List<T> supportProducingArrangements) {
        this.supportProducingArrangements.addAll(supportProducingArrangements);
    }

    @JsonProperty("producingDateTime")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ToString.Include
    public abstract LocalDateTime getProducingDateTime();

}
