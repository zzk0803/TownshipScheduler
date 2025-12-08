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


@Log4j2
@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@PlanningEntity(comparatorClass = SchedulingProducingArrangementDifficultyComparator.class)
public class SchedulingProducingArrangement {

    public static final Comparator<SchedulingProducingArrangement> COMPARATOR
            = Comparator.comparing(
                    SchedulingProducingArrangement::getPlanningDateTimeSlot,
                    Comparator.nullsFirst(Comparator.naturalOrder())
            )
            .thenComparingInt(SchedulingProducingArrangement::getId);

    public static final String PLANNING_DATA_TIME_SLOT = "planningDateTimeSlot";

    public static final String PLANNING_FACTORY_INSTANCE = "planningFactoryInstance";

    @JsonProperty("arrangeDateTime")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ToString.Include
    @ShadowVariable(supplierName = "supplierForArrangeDateTime")
    public LocalDateTime arrangeDateTime;

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

    @ShadowVariable(supplierName = "supplierForPrerequisiteArrangementsCompletedDateTime")
    private LocalDateTime prerequisiteArrangementsCompletedDateTime;

    private Duration staticDeepProducingDuration;

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
            comparatorClass = SchedulingDateTimeSlotStrengthComparator.class
    )
    private SchedulingDateTimeSlot planningDateTimeSlot;

    @ShadowVariable(supplierName = "supplierNameAheadArrangementsInFactory")
    private List<SchedulingProducingArrangement> aheadArrangementsInFactory = new ArrayList<>();

//    @ShadowVariable(supplierName = "supplierForFactoryProcessSequence")
//    private FactoryProcessSequence factoryProcessSequence;

//    @ShadowVariable(supplierName = "supplierForFactoryComputedDateTimeTuple")
//    private FactoryComputedDateTimeTuple factoryComputedDateTimeTuple;

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
        return this.planningDateTimeSlot != null
                ? this.planningDateTimeSlot.getStart()
                : null;
    }

    @ShadowSources(value = {"planningFactoryInstance", "planningDateTimeSlot"})
    public FactoryProcessSequence supplierForFactoryProcessSequence() {
        if (this.planningFactoryInstance == null || this.planningDateTimeSlot == null) {
            return null;
        }

        return FactoryProcessSequence.of(this);
    }

    @ShadowSources(
            value = {
                    "planningFactoryInstance",
                    "planningDateTimeSlot",
                    "planningFactoryInstance.planningProducingArrangements"
            }
    )
    private List<SchedulingProducingArrangement> supplierNameAheadArrangementsInFactory() {
        if (this.planningFactoryInstance == null || this.planningDateTimeSlot == null) {
            return List.of();
        }

        return this.planningFactoryInstance.getPlanningProducingArrangements()
                .stream()
                .filter(SchedulingProducingArrangement::isPlanningAssigned)
                .filter(schedulingProducingArrangement -> {
                    boolean b1 = !schedulingProducingArrangement.getArrangeDateTime()
                            .isAfter(this.getArrangeDateTime());
                    boolean b2 = schedulingProducingArrangement.getCompletedDateTime()
                            .isAfter(this.getArrangeDateTime());
                    return b1 && b2;
                })
                .sorted(
                        Comparator.comparing(SchedulingProducingArrangement::getPlanningDateTimeSlot)
                                .thenComparingInt(SchedulingProducingArrangement::getId)
                )
                .toList();

    }

//    @ShadowSources(value = {"factoryProcessSequence"})
//    private FactoryComputedDateTimeTuple supplierForFactoryComputedDateTimeTuple() {
//        if (this.planningFactoryInstance == null || this.planningDateTimeSlot == null) {
//            return null;
//        }
//
//        return this.planningFactoryInstance.query(this);
//    }

    @ShadowSources(value = {"planningFactoryInstance", "planningDateTimeSlot", "aheadArrangementsInFactory"})
    private LocalDateTime supplierForProducingDateTime() {
        if (planningFactoryInstance == null || planningDateTimeSlot == null) {
            return null;
        }

        LocalDateTime arrangeDateTime = getPlanningDateTimeSlot().getStart();
        if (weatherFactoryProducingTypeIsQueue()) {
            Duration producingDelay = this.aheadArrangementsInFactory.stream()
                    .map(SchedulingProducingArrangement::getProducingDuration)
                    .reduce(Duration::plus)
                    .orElse(Duration.ZERO);
            return arrangeDateTime.plus(producingDelay);
        } else {
            return arrangeDateTime;
        }
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

    @JsonProperty("producingDuration")
    public Duration getProducingDuration() {
        return getProducingExecutionMode().getExecuteDuration();
    }

    @ShadowSources(value = {"producingDateTime"})
    private LocalDateTime supplierForCompletedDateTime() {
        if (getProducingDateTime() == null) {
            return null;
        }
        return getProducingDateTime().plus(getProducingDuration());
    }

    @ShadowSources(
            value = {"prerequisiteProducingArrangements[].completedDateTime"},
            alignmentKey = "prerequisiteProducingArrangements"
    )
    public LocalDateTime supplierForPrerequisiteArrangementsCompletedDateTime() {
        if (this.prerequisiteProducingArrangements.isEmpty()) {
            return getSchedulingWorkCalendar().getStartDateTime();
        }

        return this.prerequisiteProducingArrangements.stream()
                .map(SchedulingProducingArrangement::getCompletedDateTime)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
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
        setStaticDeepProducingDuration(calcStaticProducingDuration());
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

    private Duration calcStaticProducingDuration() {
        Duration selfDuration = getProducingDuration();
        Duration prerequisiteStaticProducingDuration = getPrerequisiteProducingArrangements().stream()
                .map(SchedulingProducingArrangement::calcStaticProducingDuration)
                .filter(Objects::nonNull)
                .max(Duration::compareTo)
                .orElse(Duration.ZERO);
        return selfDuration.plus(prerequisiteStaticProducingDuration);
    }

    public LocalDateTime calcStaticCompleteDateTime(LocalDateTime argDateTime) {
        return argDateTime.plus(getStaticDeepProducingDuration());
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

    public boolean boolArrangeDataTimeBrokenPrerequisite() {
        return Objects.nonNull(this.prerequisiteArrangementsCompletedDateTime)
               && this.arrangeDateTime.isBefore(this.prerequisiteArrangementsCompletedDateTime);
    }

    public boolean weatherPrerequisiteRequire() {
        return !getPrerequisiteProducingArrangements().isEmpty();
    }

}
