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
import org.javatuples.Pair;
import zzk.townshipscheduler.backend.ProducingStructureType;
import zzk.townshipscheduler.backend.scheduling.model.utility.SchedulingDateTimeSlotStrengthComparator;
import zzk.townshipscheduler.backend.scheduling.model.utility.SchedulingProducingArrangementDifficultyComparator;
import zzk.townshipscheduler.utility.UuidGenerator;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Gatherer;


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

    //<editor-fold desc="SLOT_GATHERER">
    public static final Gatherer<SchedulingProducingArrangement, Void, Pair<SchedulingProducingArrangement, FactoryComputedDateTimeTuple>>
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
                                new FactoryComputedDateTimeTuple(
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
    public static final Gatherer<SchedulingProducingArrangement, FormerCompletedDateTimeRef, Pair<SchedulingProducingArrangement, FactoryComputedDateTimeTuple>>
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
                                new FactoryComputedDateTimeTuple(
                                        start,
                                        formerCompletedDateTimeRef.value = end
                                )
                        )
                ) && !downstream.isRejecting();
            }
    );
    //</editor-fold>

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

    @JsonIgnore
    private List<SchedulingProducingArrangement> arrangementCompetitors = new ArrayList<>();

    @JsonIgnore
    @ShadowVariable(supplierName = "supplierForArrangementCompetitorsComputedTupleMap")
    private LinkedHashMap<UUID, FactoryComputedDateTimeTuple> arrangementCompetitorsComputedTupleMap
            = new LinkedHashMap<>();

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

    @ShadowSources(
            value = {
                    "arrangementCompetitors[].planningFactoryInstance",
                    "arrangementCompetitors[].planningDateTimeSlot"
            },
            alignmentKey = "arrangementCompetitors"
    )
    private Map<UUID, FactoryComputedDateTimeTuple> supplierForArrangementCompetitorsComputedTupleMap() {
        return this.arrangementCompetitors.stream()
                .filter(SchedulingProducingArrangement::isPlanningAssigned)
                .sorted(Comparator.comparing(SchedulingProducingArrangement::getPlanningDateTimeSlot)
                        .thenComparingInt(SchedulingProducingArrangement::getId))
                .gather(weatherFactoryProducingTypeIsQueue()
                        ? QUEUE_GATHERER
                        : SLOT_GATHERER)
                .collect(
                        LinkedHashMap::new,
                        (linkedHashMap, tuple) -> linkedHashMap.put(
                                tuple.getValue0()
                                        .getUuid(),
                                tuple.getValue1()
                        ),
                        LinkedHashMap::putAll
                );
    }

    public boolean isPlanningAssigned() {
        return getPlanningDateTimeSlot() != null && getPlanningFactoryInstance() != null;
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

    @ShadowSources(value = {"arrangementCompetitorsComputedTupleMap"})
    private LocalDateTime supplierForProducingDateTime() {
        FactoryComputedDateTimeTuple computedDateTimeTuple = getArrangementCompetitorsComputedTupleMap().getOrDefault(
                this.uuid,
                null
        );
        if (computedDateTimeTuple == null) {
            return null;
        }
        return computedDateTimeTuple.producingDateTime();
    }

    @ShadowSources(value = {"arrangementCompetitorsComputedTupleMap"})
    private LocalDateTime supplierForCompletedDateTime() {
        FactoryComputedDateTimeTuple computedDateTimeTuple = getArrangementCompetitorsComputedTupleMap().getOrDefault(
                this.uuid,
                null
        );
        if (computedDateTimeTuple == null) {
            return null;
        }
        return computedDateTimeTuple.completedDateTime();
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

    public boolean weatherPrerequisiteRequire() {
        return !getPrerequisiteProducingArrangements().isEmpty();
    }

    public static final class FormerCompletedDateTimeRef {

        public LocalDateTime value = null;

    }

}
