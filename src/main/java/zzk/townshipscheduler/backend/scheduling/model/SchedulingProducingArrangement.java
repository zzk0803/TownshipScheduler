package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.cloner.DeepPlanningClone;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import com.fasterxml.jackson.annotation.*;
import io.arxila.javatuples.Pair;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import zzk.townshipscheduler.backend.ProducingStructureType;
import zzk.townshipscheduler.backend.scheduling.model.utility.SchedulingProducingArrangementDifficultyComparator;
import zzk.townshipscheduler.backend.utility.UuidGenerator;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Gatherer;


@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@PlanningEntity(comparatorClass = SchedulingProducingArrangementDifficultyComparator.class)
public class SchedulingProducingArrangement implements Serializable {

    public static final Comparator<SchedulingProducingArrangement> COMPARATOR
            = Comparator.comparing(
                    SchedulingProducingArrangement::getPlanningDateTimeSlot,
                    Comparator.nullsFirst(Comparator.naturalOrder())
            )
            .thenComparingInt(SchedulingProducingArrangement::getId);

    //<editor-fold desc="SLOT_GATHERER">
    public static final Gatherer<SchedulingProducingArrangement, Void, Pair<SchedulingProducingArrangement, FactoryComputedDateTimePair>>
            SLOT_GATHERER
            = Gatherer.of(
            () -> null,
            (_, schedulingProducingArrangement, downstream) -> {
                SchedulingDateTimeSlot schedulingDateTimeSlot = schedulingProducingArrangement.getPlanningDateTimeSlot();
                if (schedulingDateTimeSlot == null) {
                    return true;
                }

                LocalDateTime start = schedulingDateTimeSlot.getStart();
                return downstream.push(
                        new Pair<>(
                                schedulingProducingArrangement,
                                new FactoryComputedDateTimePair(
                                        start,
                                        start.plus(schedulingProducingArrangement.getProducingDuration())
                                )
                        )
                ) && !downstream.isRejecting();
            },
            Gatherer.defaultCombiner(),
            Gatherer.defaultFinisher()
    );
    //</editor-fold>

    //<editor-fold desc="QUEUE_GATHERER">
    public static final Gatherer<SchedulingProducingArrangement, FormerCompletedDateTimeRef, Pair<SchedulingProducingArrangement, FactoryComputedDateTimePair>>
            QUEUE_GATHERER
            = Gatherer.ofSequential(
            FormerCompletedDateTimeRef::new,
            (formerCompletedDateTimeRef, schedulingProducingArrangement, downstream) -> {

                LocalDateTime arrangeDateTime = schedulingProducingArrangement.getPlanningDateTimeSlot()
                        .getStart();
                LocalDateTime start = (formerCompletedDateTimeRef.value == null)
                        ? arrangeDateTime
                        : formerCompletedDateTimeRef.value.isAfter(arrangeDateTime)
                                ? formerCompletedDateTimeRef.value
                                : arrangeDateTime;
                LocalDateTime end = start.plus(schedulingProducingArrangement.getProducingDuration());
                return downstream.push(
                        new Pair<>(
                                schedulingProducingArrangement,
                                new FactoryComputedDateTimePair(
                                        start,
                                        formerCompletedDateTimeRef.value = end
                                )
                        )
                ) && !downstream.isRejecting();
            }
    );
    //</editor-fold>

    public static final String VALUE_RANGE_FOR_FACTORIES = "valueRangeForFactories";

    public static final String VALUE_RANGE_FOR_DATE_TIME_SLOT = "valueRangeForDateTimeSlot";

    public static final String PLANNING_DATA_TIME_SLOT = "planningDateTimeSlot";

    public static final String PLANNING_FACTORY_INSTANCE = "planningFactoryInstance";

    public static final String SHADOW_FACTORY_PROCESS_SEQUENCE = "shadowFactoryProcessSequence";

    public static final String SHADOW_PRODUCING_DATE_TIME = "producingDateTime";

    public static final String SHADOW_COMPLETED_DATE_TIME = "completedDateTime";

    public static final Comparator<SchedulingProducingArrangement> ARRANGEMENT_COMPARATOR
            = Comparator.comparing(SchedulingProducingArrangement::getPlanningDateTimeSlot)
            .thenComparingInt(SchedulingProducingArrangement::getId);

    @Serial
    private static final long serialVersionUID = 2922213328551018699L;

    @EqualsAndHashCode.Include
    @ToString.Include
    private Integer id;

    @EqualsAndHashCode.Include
    @ToString.Include
    private String uuid;

    @JsonIdentityReference
    private SchedulingOrder schedulingOrder;

    @JsonIdentityReference
    private SchedulingProduct schedulingOrderProduct;

    private SchedulingFactoryInfo requiredFactoryInfo;

    @JsonIgnore
    @DeepPlanningClone
    private List<SchedulingProducingArrangement> arrangementCompetitors = new ArrayList<>();

    @JsonIgnore
    @ShadowVariable(supplierName = "supplierForShadowArrangementCompetitorsComputedMap")
    private TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair> shadowArrangementCompetitorsComputedMap
            = new TreeMap<>();

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
    private SchedulingProducingArrangement successorProducingArrangement;

    private Duration staticDeepPrerequisiteProducingDuration;

    private Duration staticDeepProducingDuration;

    @JsonIgnore
    private SchedulingPlayer schedulingPlayer;

    @JsonIgnore
    private SchedulingWorkCalendar schedulingWorkCalendar;

    @JsonIgnore
    private SchedulingProducingExecutionMode producingExecutionMode;

    @JsonIgnore
    @PlanningVariable(valueRangeProviderRefs = {VALUE_RANGE_FOR_FACTORIES})
    private SchedulingFactoryInstance planningFactoryInstance;

    @JsonIgnore
    @PlanningVariable(valueRangeProviderRefs = {VALUE_RANGE_FOR_DATE_TIME_SLOT})
    private SchedulingDateTimeSlot planningDateTimeSlot;

    @ShadowVariable(supplierName = "supplierForFactoryProcessSequence")
    private FactoryProcessSequence factoryProcessSequence;

    @JsonProperty("arrangeDateTime")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ToString.Include
    @ShadowVariable(supplierName = "supplierForArrangeDateTime")
    private LocalDateTime arrangeDateTime;

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
        producingArrangement.setUuid(
                UuidGenerator.timeOrderedV6()
                        .toString()
        );
        return producingArrangement;
    }

    @ShadowSources(value = {"planningFactoryInstance", "planningDateTimeSlot"})
    public FactoryProcessSequence supplierForFactoryProcessSequence() {
        return this.isPlanningAssigned() ? new FactoryProcessSequence(this) : null;
    }

    public boolean isPlanningAssigned() {
        return getPlanningDateTimeSlot() != null && getPlanningFactoryInstance() != null;
    }

    @ShadowSources({"planningDateTimeSlot"})
    public LocalDateTime supplierForArrangeDateTime() {
        return this.planningDateTimeSlot != null
                ? this.planningDateTimeSlot.getStart()
                : null;
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

    public void advancedSetupOrThrow() {
        Objects.requireNonNull(this.getCurrentActionObject());
        Objects.requireNonNull(getId());
        Objects.requireNonNull(getUuid());
        Objects.requireNonNull(getSchedulingPlayer());
        Objects.requireNonNull(getSchedulingWorkCalendar());
        setRequiredFactoryInfo(getSchedulingProduct().getRequireFactory());
        setDeepPrerequisiteProducingArrangements(calcDeepPrerequisiteProducingArrangements());
        setStaticDeepProducingDuration(calcStaticProducingDuration());
    }

    public void elementarySetup(
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

    public boolean weatherFactoryProducingTypeIsQueue() {
        return getFactoryProducingType() == ProducingStructureType.QUEUE;
    }

    public ProducingStructureType getFactoryProducingType() {
        return getRequiredFactoryInfo().getProducingStructureType();
    }

    public boolean weatherFactoryProducingTypeIsSlot() {
        return getFactoryProducingType() == ProducingStructureType.SLOT;
    }

    @ShadowSources(
            value = {
                    "arrangementCompetitors[].factoryProcessSequence"
            }
    )
    public TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair> supplierForShadowArrangementCompetitorsComputedMap() {
        TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair> statefulContainer = new TreeMap<>();
        for (SchedulingProducingArrangement schedulingProducingArrangement : this.arrangementCompetitors) {
            if (Objects.isNull(schedulingProducingArrangement.getFactoryProcessSequence())) {
                continue;
            }

            if (schedulingProducingArrangement.getPlanningFactoryInstance() == this.getPlanningFactoryInstance()) {
                this.getPlanningFactoryInstance()
                        .addFactoryProcessSequence(
                                schedulingProducingArrangement.getFactoryProcessSequence(),
                                statefulContainer
                        );
            }
        }

        return statefulContainer;
    }

    @ShadowSources(
            value = {
                    "shadowArrangementCompetitorsComputedMap"
            }
    )
    public LocalDateTime supplierForProducingDateTime() {
        if (Objects.isNull(this.factoryProcessSequence)) {
            return null;
        }
        var computedDateTimePair =
                this.shadowArrangementCompetitorsComputedMap.getOrDefault(
                        this.factoryProcessSequence,
                null
        );
        if (computedDateTimePair == null) {
            return null;
        }
        return computedDateTimePair.producingDateTime();
    }

    @ShadowSources(
            value = {
                    "shadowArrangementCompetitorsComputedMap"
            }
    )
    public LocalDateTime supplierForCompletedDateTime() {
        if (Objects.isNull(this.factoryProcessSequence)) {
            return null;
        }
        var computedDateTimePair =
                this.shadowArrangementCompetitorsComputedMap.getOrDefault(
                        this.factoryProcessSequence,
                        null
                );
        if (computedDateTimePair == null) {
            return null;
        }
        return computedDateTimePair.completedDateTime();
    }

    @JsonProperty("producingDuration")
    public Duration getProducingDuration() {
        return getProducingExecutionMode().getExecuteDuration();
    }

    private Set<SchedulingProducingArrangement> calcDeepPrerequisiteProducingArrangements() {
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
                .orElse(Duration.ZERO)
                ;
        setStaticDeepPrerequisiteProducingDuration(prerequisiteStaticProducingDuration);
        return selfDuration.plus(prerequisiteStaticProducingDuration);
    }

    public LocalDateTime calcStaticCompleteDateTime(LocalDateTime argDateTime) {
        return argDateTime.plus(getStaticDeepProducingDuration());
    }

    protected <T extends SchedulingProducingArrangement> void appendPrerequisiteArrangements(
            List<T> prerequisiteArrangements
    ) {
        this.prerequisiteProducingArrangements.addAll(prerequisiteArrangements);
        this.prerequisiteProducingArrangements.forEach(
                schedulingProducingArrangement -> schedulingProducingArrangement.setSuccessorProducingArrangement(this));
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

    public boolean weatherPrerequisiteRequire() {
        return !getDeepPrerequisiteProducingArrangements().isEmpty();
    }

    @ValueRangeProvider(id = VALUE_RANGE_FOR_FACTORIES)
    public List<SchedulingFactoryInstance> valueRangeFactoryInstancesForArrangement(
            TownshipSchedulingProblem townshipSchedulingProblem
    ) {
        return townshipSchedulingProblem.valueRangeFactoryInstancesForArrangement(this);
    }

    @ValueRangeProvider(id = VALUE_RANGE_FOR_DATE_TIME_SLOT)
    public List<SchedulingDateTimeSlot> valueRangeDateTimeSlotsForArrangement(
            TownshipSchedulingProblem townshipSchedulingProblem
    ) {
        return townshipSchedulingProblem.valueRangeDateTimeSlotsForArrangement(this);
    }

    public static final class FormerCompletedDateTimeRef {

        public LocalDateTime value = null;

    }

}
