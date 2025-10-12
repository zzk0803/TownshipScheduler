package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import com.fasterxml.jackson.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import zzk.townshipscheduler.backend.ProducingStructureType;
import zzk.townshipscheduler.backend.scheduling.model.utility.SchedulingDateTimeSlotStrengthComparator;
import zzk.townshipscheduler.backend.scheduling.model.utility.SchedulingProducingArrangementDifficultyComparator;
import zzk.townshipscheduler.utility.UuidGenerator;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@PlanningEntity(difficultyComparatorClass = SchedulingProducingArrangementDifficultyComparator.class)
public class SchedulingProducingArrangement {

    public static final Comparator<SchedulingProducingArrangement> COMPARATOR
            = Comparator.comparing(SchedulingProducingArrangement::getPlanningDateTimeSlot)
            .thenComparingInt(SchedulingProducingArrangement::getId);

    public static final String PLANNING_DATA_TIME_SLOT = "planningDateTimeSlot";

    public static final String PLANNING_FACTORY_INSTANCE = "planningFactoryInstance";

    @JsonProperty("arrangeDateTime")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ToString.Include
    @ShadowVariable(supplierName = "supplierForArrangeDateTime")
    public LocalDateTime arrangeDateTime;

    @EqualsAndHashCode.Include
    @ToString.Include
    private Integer id;

    @EqualsAndHashCode.Include
    @ToString.Include
    @PlanningId
    private UUID uuid;

    @JsonIdentityReference
    private SchedulingOrder schedulingOrder;

    @JsonIdentityReference
    private SchedulingProduct schedulingOrderProduct;

    private Integer schedulingOrderProductArrangementId;

    @JsonIdentityReference
    private IGameArrangeObject targetActionObject;

    @JsonIdentityReference
    @ToString.Include
    private IGameArrangeObject currentActionObject;

    @JsonBackReference
    private Set<SchedulingProducingArrangement> prerequisiteProducingArrangements = new LinkedHashSet<>();

    @JsonBackReference
    @JsonIgnore
    private Set<SchedulingProducingArrangement> deepPrerequisiteProducingArrangements = new LinkedHashSet<>();

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

    @ShadowVariable(supplierName = "supplierForFactoryProcessSequence")
    private FactoryProcessSequence factoryProcessSequence;

    @JsonProperty("producingDateTime")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ToString.Include
    @ShadowVariable(supplierName = "supplierForProducingDateTime")
    private LocalDateTime producingDateTime;

    @JsonProperty("completedDateTime")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ToString.Include
    @ShadowVariable(supplierName = "supplierForCompletedDateTime")
    private LocalDateTime completedDateTime;

    private SchedulingArrangementsGlobalState schedulingArrangementsGlobalState;

    private SchedulingProducingArrangement(
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
        producingArrangement.setUuid(UuidGenerator.timeOrderedV6());
        return producingArrangement;
    }

    @ShadowSources({"planningDateTimeSlot"})
    public LocalDateTime supplierForArrangeDateTime() {
        return this.planningDateTimeSlot != null ? this.planningDateTimeSlot.getStart() : null;
    }

    @ShadowSources({"planningFactoryInstance", "planningDateTimeSlot"})
    public FactoryProcessSequence supplierForFactoryProcessSequence() {
        if (this.planningFactoryInstance == null | this.planningDateTimeSlot == null) {
            return null;
        }

        return new FactoryProcessSequence(this);
    }

    @ShadowSources({"schedulingArrangementsGlobalState.map", "factoryProcessSequence"})
    public LocalDateTime supplierForProducingDateTime() {
        if (this.factoryProcessSequence == null) {
            return this.producingDateTime;
        }

        FactoryComputedDateTimePair computedDateTimePair = schedulingArrangementsGlobalState.query(this.factoryProcessSequence);
        if (computedDateTimePair == null) {
            return this.producingDateTime;
        }
        return computedDateTimePair.producingDateTime();
    }

    public boolean weatherFactoryProducingTypeIsSlot() {
        return getFactoryProducingType() == ProducingStructureType.SLOT;
    }

    public ProducingStructureType getFactoryProducingType() {
        return getRequiredFactoryInfo().getProducingStructureType();
    }

    @JsonIgnore
    public SchedulingFactoryInfo getRequiredFactoryInfo() {
        return getSchedulingProduct().getRequireFactory();
    }

    @JsonProperty("schedulingProduct")
    public SchedulingProduct getSchedulingProduct() {
        return (SchedulingProduct) getCurrentActionObject();
    }

    public boolean weatherFactoryProducingTypeIsQueue() {
        return getFactoryProducingType() == ProducingStructureType.QUEUE;
    }

    @ShadowSources({"schedulingArrangementsGlobalState.map", "factoryProcessSequence"})
    public LocalDateTime supplierForCompletedDateTime() {
        if (this.factoryProcessSequence == null) {
            return this.completedDateTime;
        }

        FactoryComputedDateTimePair computedDateTimePair = schedulingArrangementsGlobalState.query(this.factoryProcessSequence);
        if (computedDateTimePair == null) {
            return this.completedDateTime;
        }
        return computedDateTimePair.completedDateTime();
    }

    @JsonProperty("producingDuration")
    public Duration getProducingDuration() {
        return getProducingExecutionMode().getExecuteDuration();
    }

    @JsonIgnore
    public boolean isFactoryMatch() {
        return Objects.nonNull(getPlanningFactoryInstance())
               && getPlanningFactoryInstance().getSchedulingFactoryInfo()
                       .typeEqual(getSchedulingProduct().getRequireFactory());
    }

    public boolean isPlanningAssigned() {
        return getPlanningDateTimeSlot() != null && getPlanningFactoryInstance() != null;
    }

    public void readyElseThrow() {
        Objects.requireNonNull(this.getCurrentActionObject());
        Objects.requireNonNull(getId());
        Objects.requireNonNull(getUuid());
        Objects.requireNonNull(getSchedulingPlayer());
        Objects.requireNonNull(getSchedulingWorkCalendar());
        setDeepPrerequisiteProducingArrangements(calcDeepPrerequisiteProducingArrangements());
    }

    public Set<SchedulingProducingArrangement> calcDeepPrerequisiteProducingArrangements() {
        LinkedList<SchedulingProducingArrangement> queue = new LinkedList<>(List.of(this));
        Set<SchedulingProducingArrangement> visited = new HashSet<>();
        Set<SchedulingProducingArrangement> result = new LinkedHashSet<>();

        while (!queue.isEmpty()) {
            SchedulingProducingArrangement current = queue.removeFirst();
            if (!visited.add(current)) {
                continue;
            }

            Set<SchedulingProducingArrangement> prerequisites =
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

    public Duration calcStaticProducingDuration() {
        Duration selfDuration = getProducingDuration();
        Duration prerequisiteStaticProducingDuration = getPrerequisiteProducingArrangements().stream()
                .map(SchedulingProducingArrangement::calcStaticProducingDuration)
                .filter(Objects::nonNull)
                .max(Duration::compareTo)
                .orElse(Duration.ZERO);
        return selfDuration.plus(prerequisiteStaticProducingDuration);

    }

    public <T extends SchedulingProducingArrangement> void appendPrerequisiteArrangements(List<T> prerequisiteArrangements) {
        this.prerequisiteProducingArrangements.addAll(prerequisiteArrangements);
    }

    public boolean isOrderDirect() {
        return getTargetActionObject() instanceof SchedulingOrder;
    }

    public boolean isDeepPrerequisiteArrangement(SchedulingProducingArrangement schedulingProducingArrangement) {
        return getDeepPrerequisiteProducingArrangements().contains(schedulingProducingArrangement);
    }

    public boolean isPrerequisiteArrangement(SchedulingProducingArrangement schedulingProducingArrangement) {
        return getPrerequisiteProducingArrangements().contains(schedulingProducingArrangement);
    }

    public List<SchedulingArrangementHierarchies> toPrerequisiteHierarchies() {
        return this.prerequisiteProducingArrangements.stream()
                .map(schedulingProducingArrangement -> SchedulingArrangementHierarchies.builder()
                        .uuid(UuidGenerator.timeOrderedV6().toString())
                        .whole(this)
                        .partial(schedulingProducingArrangement)
                        .build()
                )
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public List<SchedulingArrangementHierarchies> toDeepPrerequisiteHierarchies() {
        return this.deepPrerequisiteProducingArrangements.stream()
                .map(schedulingProducingArrangement -> SchedulingArrangementHierarchies.builder()
                        .uuid(UuidGenerator.timeOrderedV6().toString())
                        .whole(this)
                        .partial(schedulingProducingArrangement)
                        .build()
                )
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public FactoryProcessSequence toFactoryProcessSequence() {
        return new FactoryProcessSequence(this);
    }

    public Long completedDateTimeBetweenWorkCalendarEnd() {
        if (this.completedDateTime == null) {
            return null;
        }

        return Duration.between(this.completedDateTime, this.schedulingWorkCalendar.getEndDateTime()).toMinutes();
    }

}
