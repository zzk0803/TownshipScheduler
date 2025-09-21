package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.solution.cloner.DeepPlanningClone;
import ai.timefold.solver.core.api.domain.variable.*;
import com.fasterxml.jackson.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import zzk.townshipscheduler.backend.ProducingStructureType;
import zzk.townshipscheduler.backend.scheduling.model.utility.SchedulingProducingArrangementDifficultyComparator;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

@Log4j2
@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@PlanningEntity(difficultyComparatorClass = SchedulingProducingArrangementDifficultyComparator.class)
public class SchedulingProducingArrangement {

    public static final Comparator<SchedulingProducingArrangement> COMPARATOR = Comparator.comparing(
                    SchedulingProducingArrangement::getPlanningFactoryDateTimeSlot)
            .thenComparingInt(SchedulingProducingArrangement::getIndexInFactorySlot);

//    public static final String VALUE_RANGE_FOR_FACTORIES_IN_ARRANGEMENT = "valueRangeForFactoriesInArrangement";

    public static final String PLANNING_DATA_TIME_SLOT = "planningDateTimeSlot";

    public static final String PLANNING_FACTORY_INSTANCE = "planningFactoryInstance";

    public static final String SHADOW_FACTORY_PROCESS_SEQUENCE = "shadowFactoryProcessSequence";

    public static final String SHADOW_PRODUCING_DATE_TIME = "computedShadowProducingDateTime";

    public static final String SHADOW_COMPLETED_DATE_TIME = "computedShadowCompletedDateTime";

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
    @DeepPlanningClone
    private Set<SchedulingProducingArrangement> prerequisiteProducingArrangements = new LinkedHashSet<>();

    @JsonBackReference
    @JsonIgnore
    @DeepPlanningClone
    private Set<SchedulingProducingArrangement> deepPrerequisiteProducingArrangements = new LinkedHashSet<>();

    @JsonIgnore
    private SchedulingPlayer schedulingPlayer;

    @JsonIgnore
    private SchedulingWorkCalendar schedulingWorkCalendar;

    @JsonIgnore
    private SchedulingProducingExecutionMode producingExecutionMode;

    @JsonIgnore
    @InverseRelationShadowVariable(sourceVariableName = SchedulingFactoryInstanceDateTimeSlot.PLANNING_SCHEDULING_PRODUCING_ARRANGEMENTS)
    private SchedulingFactoryInstanceDateTimeSlot planningFactoryDateTimeSlot;

    @JsonIgnore
    @PreviousElementShadowVariable(sourceVariableName = SchedulingFactoryInstanceDateTimeSlot.PLANNING_SCHEDULING_PRODUCING_ARRANGEMENTS)
    private SchedulingProducingArrangement previousSchedulingProducingArrangement;

    @JsonIgnore
    @PreviousElementShadowVariable(sourceVariableName = SchedulingFactoryInstanceDateTimeSlot.PLANNING_SCHEDULING_PRODUCING_ARRANGEMENTS)
    private SchedulingProducingArrangement nextSchedulingProducingArrangement;

    @JsonIgnore
    @IndexShadowVariable(sourceVariableName = SchedulingFactoryInstanceDateTimeSlot.PLANNING_SCHEDULING_PRODUCING_ARRANGEMENTS)
    private Integer indexInFactorySlot;

    @ShadowVariable(supplierName = "shadowFactoryComputedDateTimePairSupplier")
    private FactoryComputedDateTimePair shadowFactoryComputedDateTimePair;

    @JsonIgnore
    @ShadowVariable(supplierName = "shadowMendedFirstArrangementProducingDateTimeSupplier")
    private LocalDateTime shadowMendedFirstArrangementProducingDateTime;

    @JsonIgnore
    @ShadowVariable(supplierName = "schedulingFactoryInstanceSupplier")
    private SchedulingFactoryInstance schedulingFactoryInstance;

    @JsonProperty("arrangeDateTime")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ToString.Include
    @ShadowVariable(supplierName = "arrangeDateTimeSupplier")
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

    @JsonIgnore
    @ShadowVariable(supplierName = "schedulingDateTimeSlotSupplier")
    private SchedulingDateTimeSlot schedulingDateTimeSlot;

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

    @ShadowSources("planningFactoryDateTimeSlot")
    public SchedulingDateTimeSlot schedulingDateTimeSlotSupplier() {
        return getPlanningFactoryDateTimeSlot() != null
                ? getPlanningFactoryDateTimeSlot().getDateTimeSlot()
                : null;
    }

    @ShadowSources(
            value = {
                    "planningFactoryDateTimeSlot",
                    "shadowMendedFirstArrangementProducingDateTime",
                    "previousSchedulingProducingArrangement.shadowFactoryComputedDateTimePair"
            }
    )
    private FactoryComputedDateTimePair shadowFactoryComputedDateTimePairSupplier() {
        if (this.getPlanningFactoryDateTimeSlot() == null) {
            return null;
        }

        if (!weatherFactoryProducingTypeIsQueue()) {
            LocalDateTime producingDateTime = getArrangeDateTime();
            return new FactoryComputedDateTimePair(
                    producingDateTime,
                    producingDateTime.plus(getProducingDuration())
            );
        }

        if (getPreviousSchedulingProducingArrangement() != null) {
            FactoryComputedDateTimePair previousDateTimePair
                    = getPreviousSchedulingProducingArrangement().getShadowFactoryComputedDateTimePair();
            return new FactoryComputedDateTimePair(
                    previousDateTimePair.completedDateTime(),
                    previousDateTimePair.completedDateTime().plus(getProducingDuration())
            );
        } else {
            LocalDateTime producingDateTime = getShadowMendedFirstArrangementProducingDateTime();
            LocalDateTime completedDateTime = producingDateTime.plus(getProducingDuration());
            return new FactoryComputedDateTimePair(producingDateTime, completedDateTime);
        }
    }

    public boolean weatherFactoryProducingTypeIsQueue() {
        return getFactoryProducingType() == ProducingStructureType.QUEUE;
    }

    @JsonProperty("producingDuration")
    public Duration getProducingDuration() {
        return getProducingExecutionMode().getExecuteDuration();
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

    @ShadowSources(
            value = {
                    "planningFactoryDateTimeSlot",
                    "arrangeDateTime",
                    "previousSchedulingProducingArrangement",
                    "nextSchedulingProducingArrangement",
                    "indexInFactorySlot"
            }
    )
    public LocalDateTime shadowMendedFirstArrangementProducingDateTimeSupplier() {
        if (this.getPlanningFactoryDateTimeSlot() == null) {
            return null;
        }

        if (!weatherFactoryProducingTypeIsQueue()) {
            return getArrangeDateTime();
        }

        SchedulingFactoryInstanceDateTimeSlot start = this.getPlanningFactoryDateTimeSlot();
        SchedulingFactoryInstanceDateTimeSlot firstPrevious
                = start != null ? start.getPrevious() : null;

        Optional<LocalDateTime> formerCompletedDateTimeOptional = Stream.iterate(
                        firstPrevious,
                        Objects::nonNull,
                        SchedulingFactoryInstanceDateTimeSlot::getPrevious
                )
                .flatMap(schedulingFactoryInstanceDateTimeSlot ->
                        schedulingFactoryInstanceDateTimeSlot.getPlanningSchedulingProducingArrangements()
                                .stream()
                                .filter(schedulingProducingArrangement -> Objects.isNull(schedulingProducingArrangement.getNextSchedulingProducingArrangement()))
                )
                .map(SchedulingProducingArrangement::getShadowFactoryComputedDateTimePair)
                .map(FactoryComputedDateTimePair::completedDateTime)
                .max(Comparator.nullsFirst(Comparator.naturalOrder()));

        if (formerCompletedDateTimeOptional.isPresent()) {
            LocalDateTime formerSlotCompletedDateTime = formerCompletedDateTimeOptional.get();
            return formerSlotCompletedDateTime.isAfter(getArrangeDateTime())
                    ? formerSlotCompletedDateTime
                    : getArrangeDateTime();
        } else {
            return getArrangeDateTime();
        }

    }

    @ShadowSources("planningFactoryDateTimeSlot")
    public LocalDateTime arrangeDateTimeSupplier() {
        SchedulingDateTimeSlot dateTimeSlot = getSchedulingDateTimeSlot();
        return dateTimeSlot != null ? dateTimeSlot.getStart() : null;
    }

    @JsonIgnore
    public boolean isFactoryMatch() {
        return Objects.nonNull(getSchedulingFactoryInstance())
               && getSchedulingFactoryInstance().getSchedulingFactoryInfo()
                       .typeEqual(getSchedulingProduct().getRequireFactory());
    }

    public boolean isPlanningAssigned() {
        return getPlanningFactoryDateTimeSlot() != null;
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

    @ShadowSources({"shadowFactoryComputedDateTimePair"})
    public LocalDateTime producingDateTimeSupplier() {
        FactoryComputedDateTimePair computedDataTimePair = getShadowFactoryComputedDateTimePair();
        if (computedDataTimePair == null) {
            return null;
        } else {
            return computedDataTimePair.producingDateTime();
        }
    }

    @ShadowSources({"shadowFactoryComputedDateTimePair"})
    public LocalDateTime completedDateTimeSupplier() {
        FactoryComputedDateTimePair computedDataTimePair = getShadowFactoryComputedDateTimePair();
        if (computedDataTimePair == null) {
            return null;
        } else {
            return computedDataTimePair.completedDateTime();
        }
    }

    @ShadowSources({"planningFactoryDateTimeSlot"})
    public SchedulingFactoryInstance schedulingFactoryInstanceSupplier() {
        SchedulingFactoryInstanceDateTimeSlot factoryDateTimeSlot = getPlanningFactoryDateTimeSlot();
        if (factoryDateTimeSlot == null) {
            return null;
        }
        return factoryDateTimeSlot.getFactoryInstance();
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

}
