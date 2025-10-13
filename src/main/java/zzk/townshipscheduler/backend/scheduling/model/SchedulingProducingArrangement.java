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
import lombok.extern.log4j.Log4j2;
import zzk.townshipscheduler.backend.ProducingStructureType;
import zzk.townshipscheduler.backend.scheduling.model.utility.SchedulingDateTimeSlotStrengthComparator;
import zzk.townshipscheduler.backend.scheduling.model.utility.SchedulingProducingArrangementDifficultyComparator;
import zzk.townshipscheduler.utility.UuidGenerator;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Log4j2
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

    @ShadowVariable(supplierName = "supplierForPrerequisiteProducingArrangementsCompletedDateTime")
    private LocalDateTime prerequisiteProducingArrangementsCompletedDateTime;

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

    @ShadowSources(
            value = {"prerequisiteProducingArrangements[].completedDateTime"},
            alignmentKey = "prerequisiteProducingArrangements"
    )
    public LocalDateTime supplierForPrerequisiteProducingArrangementsCompletedDateTime() {
        if (this.prerequisiteProducingArrangements.isEmpty()) {
            return getSchedulingWorkCalendar().getStartDateTime();
        }

        return this.prerequisiteProducingArrangements.stream()
                .map(SchedulingProducingArrangement::getCompletedDateTime)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    @ShadowSources({"schedulingArrangementsGlobalState.map"})
    public LocalDateTime supplierForProducingDateTime() {
        FactoryComputedDateTimePair computedDateTimePair = schedulingArrangementsGlobalState.query(this);
        if (computedDateTimePair == null) {
            return null;
        }
        return computedDateTimePair.producingDateTime();
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

    @JsonProperty("schedulingProduct")
    public SchedulingProduct getSchedulingProduct() {
        return (SchedulingProduct) getCurrentActionObject();
    }

    @ShadowSources({"schedulingArrangementsGlobalState.map"})
    public LocalDateTime supplierForCompletedDateTime() {
        FactoryComputedDateTimePair computedDateTimePair = schedulingArrangementsGlobalState.query(this);
        if (computedDateTimePair == null) {
            return null;
        }
        return computedDateTimePair.producingDateTime();
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

    public boolean boolArrangeDataTimeBrokenPrerequisite() {
        return Objects.nonNull(this.prerequisiteProducingArrangementsCompletedDateTime)
               && this.arrangeDateTime.isBefore(this.prerequisiteProducingArrangementsCompletedDateTime);
    }

    public Long arrangeDateTimeBetweenPrerequisiteArrangementsCompletedDateTime() {
        if (Objects.isNull(this.prerequisiteProducingArrangementsCompletedDateTime)) {
            return 0L;
        }

        if (this.arrangeDateTime.isAfter(this.prerequisiteProducingArrangementsCompletedDateTime)) {
            return Duration.between(this.prerequisiteProducingArrangementsCompletedDateTime, this.arrangeDateTime)
                    .toMinutes();
        } else if (this.arrangeDateTime.isBefore(this.prerequisiteProducingArrangementsCompletedDateTime)) {
            return Duration.between(this.arrangeDateTime, this.prerequisiteProducingArrangementsCompletedDateTime)
                    .toMinutes();
        } else {
            return 0L;
        }
    }

    public Long completedDateTimeBetweenWorkCalendarEnd() {
        if (this.completedDateTime == null) {
            return null;
        }

        return Duration.between(this.completedDateTime, this.schedulingWorkCalendar.getEndDateTime()).toMinutes();
    }

}
