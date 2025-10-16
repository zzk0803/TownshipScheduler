package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.*;
import com.fasterxml.jackson.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import zzk.townshipscheduler.backend.ProducingStructureType;
import zzk.townshipscheduler.backend.scheduling.model.utility.SchedulingDateTimeSlotStrengthComparator;
import zzk.townshipscheduler.backend.scheduling.model.utility.SchedulingProducingArrangementDifficultyComparator;

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
public class SchedulingProducingArrangement implements Comparable<SchedulingProducingArrangement> {

    public static final Comparator<SchedulingProducingArrangement> COMPARATOR
            = Comparator
            .comparing(
                    SchedulingProducingArrangement::getArrangeDateTime,
                    Comparator.nullsFirst(Comparator.naturalOrder())
            )
            .thenComparing(
                    SchedulingProducingArrangement::getIndexInFactoryArrangements,
                    Comparator.nullsFirst(Comparator.naturalOrder())
            );

    public static final String PLANNING_DATA_TIME_SLOT = "planningDateTimeSlot";

    public static final String PLANNING_FACTORY_INSTANCE = "planningFactoryInstance";

    @EqualsAndHashCode.Include
    @ToString.Include
    private Integer id;

    @EqualsAndHashCode.Include
    @ToString.Include
    @PlanningId
    private String uuid;

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
    @ShadowVariable(supplierName = "supplierForDeepPrerequisiteProducingArrangementsCompletedDateTime")
    private LocalDateTime deepPrerequisiteProducingArrangementsCompletedDateTime;

    @JsonIgnore
    private SchedulingPlayer schedulingPlayer;

    @JsonIgnore
    private SchedulingWorkCalendar schedulingWorkCalendar;

    @JsonIgnore
    private SchedulingProducingExecutionMode producingExecutionMode;

    @JsonIgnore
    @InverseRelationShadowVariable(sourceVariableName = SchedulingFactoryInstance.PLANNING_PRODUCING_ARRANGEMENTS)
    private SchedulingFactoryInstance planningFactoryInstance;

    @PreviousElementShadowVariable(sourceVariableName = SchedulingFactoryInstance.PLANNING_PRODUCING_ARRANGEMENTS)
    private SchedulingProducingArrangement previousProducingArrangement;

    @IndexShadowVariable(sourceVariableName = SchedulingFactoryInstance.PLANNING_PRODUCING_ARRANGEMENTS)
    private Integer indexInFactoryArrangements;

    @JsonIgnore
    @PlanningVariable(
            valueRangeProviderRefs = {TownshipSchedulingProblem.VALUE_RANGE_FOR_DATE_TIME_SLOT},
            strengthComparatorClass = SchedulingDateTimeSlotStrengthComparator.class
    )
    private SchedulingDateTimeSlot planningDateTimeSlot;

    @JsonProperty("arrangeDateTime")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ToString.Include
    @ShadowVariable(supplierName = "supplierArrangeDateTime")
    private LocalDateTime arrangeDateTime;

    @JsonProperty("producingDateTime")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ToString.Include
    @ShadowVariable(supplierName = "producingDateTimeSupplier")
    private LocalDateTime producingDateTime;

    @JsonProperty("completedDateTime")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ToString.Include
    @ShadowVariable(supplierName = "completedDateTimeSupplier")
    private LocalDateTime completedDateTime;

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
        producingArrangement.setUuid(UUID.randomUUID().toString());
        return producingArrangement;
    }


    @ShadowSources({"planningDateTimeSlot"})
    public LocalDateTime supplierArrangeDateTime() {
        SchedulingDateTimeSlot schedulingDateTimeSlot = this.getPlanningDateTimeSlot();
        return schedulingDateTimeSlot != null
                ? schedulingDateTimeSlot.getStart()
                : null;
    }

    @ShadowSources(
            {
                    "planningFactoryInstance",
                    "planningDateTimeSlot",
                    "previousProducingArrangement",
                    "previousProducingArrangement.completedDateTime"
            }
    )
    public LocalDateTime producingDateTimeSupplier() {
        if (Objects.isNull(this.planningDateTimeSlot) || Objects.isNull(this.planningFactoryInstance)) {
            return null;
        }

        LocalDateTime arrangeDateTime = this.planningDateTimeSlot.getStart();
        if (weatherFactoryProducingTypeIsQueue()) {
            if (this.previousProducingArrangement == null) {
                return arrangeDateTime;
            } else {
                return calcProducingDateTime(
                        arrangeDateTime,
                        this.previousProducingArrangement.getCompletedDateTime()
                );
            }
        } else {
            return arrangeDateTime;
        }
    }

    private LocalDateTime calcProducingDateTime(
            LocalDateTime currentArrangeDateTime,
            LocalDateTime previousCompletedDateTime
    ) {
        return previousCompletedDateTime == null || currentArrangeDateTime.isAfter(previousCompletedDateTime)
                ? currentArrangeDateTime
                : previousCompletedDateTime;
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

    @ShadowSources({"producingDateTime"})
    public LocalDateTime completedDateTimeSupplier() {
        return Objects.nonNull(this.producingDateTime)
                ? this.producingDateTime.plus(this.getProducingDuration())
                : null;
    }

    @JsonProperty("producingDuration")
    public Duration getProducingDuration() {
        return getProducingExecutionMode().getExecuteDuration();
    }

    @ShadowSources(
            value = {"deepPrerequisiteProducingArrangements[].completedDateTime"}
    )
    public LocalDateTime supplierForDeepPrerequisiteProducingArrangementsCompletedDateTime() {
        List<LocalDateTime> deepPrerequisiteProducingArrangementsCompletedDateTimeList
                = this.deepPrerequisiteProducingArrangements.stream()
                .map(SchedulingProducingArrangement::getCompletedDateTime)
                .toList();
        if (deepPrerequisiteProducingArrangementsCompletedDateTimeList.stream().anyMatch(Objects::isNull)) {
            return this.getSchedulingWorkCalendar().getEndDateTime();
        }

        return deepPrerequisiteProducingArrangementsCompletedDateTimeList.stream()
                .max(Comparator.naturalOrder())
                .orElse(this.getSchedulingWorkCalendar().getEndDateTime());
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

    public List<SchedulingArrangementHierarchies> toPrerequisiteHierarchies() {
        return this.prerequisiteProducingArrangements.stream()
                .map(schedulingProducingArrangement -> SchedulingArrangementHierarchies.builder()
                        .whole(this)
                        .partial(schedulingProducingArrangement)
                        .build()
                )
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public List<SchedulingArrangementHierarchies> toDeepPrerequisiteHierarchies() {
        return this.deepPrerequisiteProducingArrangements.stream()
                .map(schedulingProducingArrangement -> SchedulingArrangementHierarchies.builder()
                        .whole(this)
                        .partial(schedulingProducingArrangement)
                        .build()
                )
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public int compareTo(@NotNull SchedulingProducingArrangement that) {
        return COMPARATOR.compare(this, that);
    }

    public boolean weatherPrerequisiteRequire() {
        return !getPrerequisiteProducingArrangements().isEmpty();
    }

}
