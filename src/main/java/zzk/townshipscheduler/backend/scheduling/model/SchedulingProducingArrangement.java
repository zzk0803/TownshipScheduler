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
import zzk.townshipscheduler.utility.UuidGenerator;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@PlanningEntity(comparatorClass = SchedulingProducingArrangementDifficultyComparator.class)
public class SchedulingProducingArrangement {

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

    private Duration staticDeepProducingDuration;

    @JsonIgnore
    private SchedulingPlayer schedulingPlayer;

    @JsonIgnore
    private SchedulingWorkCalendar schedulingWorkCalendar;

    @JsonIgnore
    private SchedulingProducingExecutionMode producingExecutionMode;

    @JsonIgnore
    @InverseRelationShadowVariable(
            sourceVariableName = SchedulingFactoryInstanceDateTimeSlot.PLANNING_SCHEDULING_PRODUCING_ARRANGEMENTS
    )
    private SchedulingFactoryInstanceDateTimeSlot planningFactoryDateTimeSlot;

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

    @JsonIgnore
    @PreviousElementShadowVariable(
            sourceVariableName = SchedulingFactoryInstanceDateTimeSlot.PLANNING_SCHEDULING_PRODUCING_ARRANGEMENTS
    )
    private SchedulingProducingArrangement previousSchedulingProducingArrangement;

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
        producingArrangement.setUuid(UuidGenerator.timeOrderedV6().toString());
        return producingArrangement;
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

    @JsonProperty("producingDuration")
    public Duration getProducingDuration() {
        return getProducingExecutionMode().getExecuteDuration();
    }

    @ShadowSources(
            {
                    "planningFactoryDateTimeSlot",
                    "planningFactoryDateTimeSlot.amendedFirstArrangementProducingDateTime",
                    "previousSchedulingProducingArrangement",
                    "previousSchedulingProducingArrangement.completedDateTime"
            }
    )
    public LocalDateTime producingDateTimeSupplier() {
        if (this.planningFactoryDateTimeSlot == null) {
            return null;
        }

        LocalDateTime thisProducingDateTime;
        if (weatherFactoryProducingTypeIsQueue()) {
            thisProducingDateTime = this.previousSchedulingProducingArrangement != null
                    ? this.previousSchedulingProducingArrangement.getCompletedDateTime()
                    : this.planningFactoryDateTimeSlot.getAmendedFirstArrangementProducingDateTime();

        } else {
            thisProducingDateTime = this.planningFactoryDateTimeSlot.getStart();
        }
        return thisProducingDateTime;
    }

    @ShadowSources({"producingDateTime"})
    public LocalDateTime completedDateTimeSupplier() {
        if (this.producingDateTime == null) {
            return null;
        }

        return this.producingDateTime.plus(this.getProducingDuration());
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
                .orElse(Duration.ZERO);
        return selfDuration.plus(prerequisiteStaticProducingDuration);
    }

    public LocalDateTime calcStaticCompleteDateTime(LocalDateTime argDateTime) {
        return argDateTime.plus(getStaticDeepProducingDuration());
    }

    public <T extends SchedulingProducingArrangement> void appendPrerequisiteArrangements(List<T> prerequisiteArrangements) {
        this.prerequisiteProducingArrangements.addAll(prerequisiteArrangements);
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

    public SchedulingFactoryInstance getPlanningFactoryInstance() {
        if (Objects.isNull(getPlanningFactoryDateTimeSlot())) {
            return null;
        }
        return getPlanningFactoryDateTimeSlot().getFactoryInstance();
    }

}
