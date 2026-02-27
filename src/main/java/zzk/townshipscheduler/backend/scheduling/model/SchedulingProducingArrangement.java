package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.*;
import com.fasterxml.jackson.annotation.*;
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
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@PlanningEntity(comparatorClass = SchedulingProducingArrangementDifficultyComparator.class)
public class SchedulingProducingArrangement implements Serializable {

    @Serial
    private static final long serialVersionUID = -5639537319806308851L;

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
    @JsonIgnore
    private Set<SchedulingProducingArrangement> prerequisiteProducingArrangements
            = new LinkedHashSet<>();

    @JsonBackReference
    @JsonIgnore
    private Set<SchedulingProducingArrangement> deepPrerequisiteProducingArrangements
            = new LinkedHashSet<>();

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
    @ShadowVariable(supplierName = "prerequisiteProducingArrangementsCompletedDateTimeSupplier")
    private LocalDateTime prerequisiteProducingArrangementsCompletedDateTime;

    @JsonIgnore
    @InverseRelationShadowVariable(
            sourceVariableName = SchedulingFactoryInstanceDateTimeSlot.PLANNING_SCHEDULING_PRODUCING_ARRANGEMENTS
    )
    private SchedulingFactoryInstanceDateTimeSlot planningFactoryDateTimeSlot;

    @JsonIgnore
    @PreviousElementShadowVariable(
            sourceVariableName = SchedulingFactoryInstanceDateTimeSlot.PLANNING_SCHEDULING_PRODUCING_ARRANGEMENTS
    )
    private SchedulingProducingArrangement previousSchedulingProducingArrangement;

    @JsonIgnore
    @IndexShadowVariable(sourceVariableName = SchedulingFactoryInstanceDateTimeSlot.PLANNING_SCHEDULING_PRODUCING_ARRANGEMENTS)
    private Integer indexInSlot;

    @JsonIgnore
    @ShadowVariable(supplierName = "schedulingDateTimeSlotSupplier")
    private SchedulingDateTimeSlot schedulingDateTimeSlot;

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
        producingArrangement.setUuid(UuidGenerator.timeOrderedV6()
                .toString());
        return producingArrangement;
    }

    @ShadowSources(value = {"prerequisiteProducingArrangements[].completedDateTime"})
    private LocalDateTime prerequisiteProducingArrangementsCompletedDateTimeSupplier() {
        if (prerequisiteProducingArrangements == null || prerequisiteProducingArrangements.isEmpty()) {
            return getSchedulingWorkCalendar().getStartDateTime();
        }

        var prerequisiteCompletedDateTimeList = this.getPrerequisiteProducingArrangements()
                .stream()
                .map(SchedulingProducingArrangement::getCompletedDateTime)
                .toList()
                ;
        if (prerequisiteCompletedDateTimeList.stream()
                .anyMatch(Objects::isNull)) {
            return null;
        }

        return prerequisiteCompletedDateTimeList.stream()
                .max(Comparator.naturalOrder())
                .orElseThrow();
    }

    @ShadowSources({"planningFactoryDateTimeSlot"})
    public SchedulingDateTimeSlot schedulingDateTimeSlotSupplier() {
        return getPlanningFactoryDateTimeSlot() != null
                ? getPlanningFactoryDateTimeSlot().getDateTimeSlot()
                : null;
    }

    @ShadowSources({"schedulingDateTimeSlot"})
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

    @JsonProperty("schedulingProduct")
    public SchedulingProduct getSchedulingProduct() {
        return (SchedulingProduct) getCurrentActionObject();
    }

    public boolean isPlanningAssigned() {
        return getPlanningFactoryDateTimeSlot() != null;
    }

    public void advancedSetupOrThrow() {
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

    @ShadowSources(
            value = {
                    "planningFactoryDateTimeSlot",
                    "previousSchedulingProducingArrangement",
                    "previousSchedulingProducingArrangement.completedDateTime"
            }
    )
    public LocalDateTime producingDateTimeSupplier(TownshipSchedulingProblem townshipSchedulingProblem) {
        if (this.planningFactoryDateTimeSlot == null) {
            return null;
        }

        LocalDateTime thisProducingDateTime;
        if (weatherFactoryProducingTypeIsQueue()) {
            thisProducingDateTime = this.previousSchedulingProducingArrangement != null
                    ? this.previousSchedulingProducingArrangement.getCompletedDateTime()
                    : decideProducingDateTime(townshipSchedulingProblem);

        } else {
            thisProducingDateTime = this.planningFactoryDateTimeSlot.getStart();
        }
        return thisProducingDateTime;
    }

    private LocalDateTime decideProducingDateTime(TownshipSchedulingProblem townshipSchedulingProblem) {
        SchedulingFactoryInstanceDateTimeSlot thisPlanningSlot = getPlanningFactoryDateTimeSlot();
        if (thisPlanningSlot.getPrevious() == null) {
            return thisPlanningSlot.getStart();
        } else {
            SchedulingProducingArrangement previousProducingArrangement = townshipSchedulingProblem.getSchedulingProducingArrangementList()
                    .stream()
                    .filter(schedulingProducingArrangement -> schedulingProducingArrangement.getPlanningFactoryDateTimeSlot() == thisPlanningSlot)
                    .collect(Collectors.toCollection(() -> new TreeSet<>(
                            Comparator.comparing(SchedulingProducingArrangement::getPlanningFactoryDateTimeSlot)
                                    .thenComparingInt(SchedulingProducingArrangement::getIndexInSlot))
                    ))
                    .lower(this)
                    ;
            return previousProducingArrangement == null ? thisPlanningSlot.getStart() : previousProducingArrangement.getCompletedDateTime();
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

    @ShadowSources({"producingDateTime"})
    public LocalDateTime completedDateTimeSupplier() {
        if (this.producingDateTime == null) {
            return null;
        }

        return this.producingDateTime.plus(this.getProducingDuration());
    }

    @JsonProperty("producingDuration")
    public Duration getProducingDuration() {
        return getProducingExecutionMode().getExecuteDuration();
    }

    @ShadowSources({"planningFactoryDateTimeSlot"})
    public SchedulingFactoryInstance schedulingFactoryInstanceSupplier() {
        SchedulingFactoryInstanceDateTimeSlot factoryDateTimeSlot = getPlanningFactoryDateTimeSlot();
        if (factoryDateTimeSlot == null) {
            return null;
        }
        return factoryDateTimeSlot.getFactoryInstance();
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

    public boolean haveDeepPrerequisiteArrangement(SchedulingProducingArrangement schedulingProducingArrangement) {
        return getDeepPrerequisiteProducingArrangements().contains(schedulingProducingArrangement);
    }

    public boolean havePrerequisiteArrangement(SchedulingProducingArrangement schedulingProducingArrangement) {
        return getPrerequisiteProducingArrangements().contains(schedulingProducingArrangement);
    }


    public boolean weatherPrerequisiteRequire() {
        return !getPrerequisiteProducingArrangements().isEmpty();
    }


}
