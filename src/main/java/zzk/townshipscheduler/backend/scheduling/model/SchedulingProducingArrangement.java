package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.*;
import com.fasterxml.jackson.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.ObjectUtils;
import zzk.townshipscheduler.backend.ProducingStructureType;
import zzk.townshipscheduler.backend.scheduling.model.utility.SchedulingProducingArrangementDifficultyComparator;
import zzk.townshipscheduler.backend.utility.UuidGenerator;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;


@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@PlanningEntity(comparatorClass = SchedulingProducingArrangementDifficultyComparator.class)
public class SchedulingProducingArrangement
        implements Serializable {

    public static final String PLANNING_FACTORY_INSTANCE = "planningFactoryInstance";

    public static final String PLANNING_DELAY_SLOT = "planningDelaySlot";

    public static final String SHADOW_DATE_TIME_SLOT = "shadowDateTimeSlot";

    public static final String SHADOW_PRODUCING_DATE_TIME = "producingDateTime";

    public static final String SHADOW_COMPLETED_DATE_TIME = "completedDateTime";

    public static final String SHADOW_DEEP_PREREQUISITE_PRODUCING_ARRANGEMENTS_FINISHED_DATE_TIME
            = "shadowDeepPrerequisiteProducingArrangementsFinishedDateTime";

    public static final String PREVIOUS_PRODUCING_ARRANGEMENT = "previousProducingArrangement";

    public static final String SHADOW_ARRANGE_DATE_TIME = "arrangeDateTime";

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
    @ShadowVariable(supplierName = "supplierForShadowDeepPrerequisiteProducingArrangementsFinishedDateTime")
    private LocalDateTime shadowDeepPrerequisiteProducingArrangementsFinishedDateTime;

    @JsonIgnore
    private TreeSet<SchedulingDateTimeSlot> schedulingDateTimeSlots;

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
    @InverseRelationShadowVariable(
            sourceVariableName = SchedulingFactoryInstance.PLANNING_FACTORY_INSTANCE_PRODUCING_ARRANGEMENTS
    )
    private SchedulingFactoryInstance planningFactoryInstance;

    @JsonIgnore
    @PlanningVariable(valueRangeProviderRefs = TownshipSchedulingProblem.VALUE_RANGE_FOR_DATE_TIME_SLOT_DELAY)
    private Integer planningDelaySlot;

    @JsonIgnore
    @ShadowVariable(supplierName = "supplierForShadowDateTimeSlot")
    private SchedulingDateTimeSlot shadowDateTimeSlot;

    @PreviousElementShadowVariable(
            sourceVariableName = SchedulingFactoryInstance.PLANNING_FACTORY_INSTANCE_PRODUCING_ARRANGEMENTS
    )
    private SchedulingProducingArrangement previousProducingArrangement;

    @ShadowVariablesInconsistent
    private Boolean shadowVariablesInconsistent;

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
        producingArrangement.setUuid(UuidGenerator.timeOrderedV6()
                                             .toString());
        return producingArrangement;
    }

    @ShadowSources({"deepPrerequisiteProducingArrangements[].completedDateTime", "arrangeDateTime"})
    public LocalDateTime supplierForShadowDeepPrerequisiteProducingArrangementsFinishedDateTime() {
        LocalDateTime finishedDateTime = getSchedulingWorkCalendar().getStartDateTime();
        if (this.deepPrerequisiteProducingArrangements != null) {
            for (SchedulingProducingArrangement prerequisiteProducingArrangement : deepPrerequisiteProducingArrangements) {
                if (prerequisiteProducingArrangement.completedDateTime == null) {
                    return null;
                }
                finishedDateTime = ObjectUtils.max(arrangeDateTime, prerequisiteProducingArrangement.completedDateTime);
            }
        }
        return finishedDateTime;
    }

    @ShadowSources({"planningDelaySlot", "shadowDeepPrerequisiteProducingArrangementsFinishedDateTime"})
    public SchedulingDateTimeSlot supplierForShadowDateTimeSlot() {
        return this.schedulingDateTimeSlots.stream()
                .sorted(SchedulingDateTimeSlot.DATE_TIME_SLOT_COMPARATOR)
                .dropWhile(
                        schedulingDateTimeSlot -> schedulingDateTimeSlot.getStart()
                                .isBefore(this.shadowDeepPrerequisiteProducingArrangementsFinishedDateTime)
                )
                .skip(this.planningDelaySlot)
                .findFirst().orElse(null);
    }

    @ShadowSources({"shadowDateTimeSlot"})
    public LocalDateTime supplierForArrangeDateTime() {
        return this.shadowDateTimeSlot != null ? this.shadowDateTimeSlot.getStart() : null;
    }

    @ShadowSources({
            "shadowDeepPrerequisiteProducingArrangementsFinishedDateTime",
            "arrangeDateTime",
            "previousProducingArrangement.completedDateTime",
            "planningFactoryInstance"
    })
    public LocalDateTime supplierForProducingDateTime() {
        LocalDateTime producingDateTime;
        if (planningFactoryInstance != null) {
            producingDateTime = getSchedulingWorkCalendar().getStartDateTime();
        } else {
            return null;
        }

        if (shadowDeepPrerequisiteProducingArrangementsFinishedDateTime != null) {
            producingDateTime = ObjectUtils.max(
                    arrangeDateTime,
                    shadowDeepPrerequisiteProducingArrangementsFinishedDateTime,
                    previousProducingArrangement.completedDateTime
            );
        }
        return producingDateTime;
    }

    @ShadowSources({"producingDateTime"})
    public LocalDateTime supplierForCompletedDateTime() {
        if (producingDateTime == null) {
            return null;
        }

        return producingDateTime.plus(this.getProducingDuration());
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

    @JsonProperty("schedulingProduct")
    public SchedulingProduct getSchedulingProduct() {
        return (SchedulingProduct) getCurrentActionObject();
    }

    public boolean isPlanningAssigned() {
        return getShadowDateTimeSlot() != null && getPlanningFactoryInstance() != null;
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

    public void elementarySetup(
            ArrangementIdRoller idRoller,
            SchedulingWorkCalendar workTimeLimit,
            SchedulingPlayer schedulingPlayer,
            TreeSet<SchedulingDateTimeSlot> schedulingDateTimeSlots
    ) {
        this.schedulingDateTimeSlots = schedulingDateTimeSlots;
        idRoller.setup(this);
        this.schedulingWorkCalendar = workTimeLimit;
        this.schedulingPlayer = schedulingPlayer;
        this.schedulingDateTimeSlots = schedulingDateTimeSlots;
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
                .orElse(Duration.ZERO);
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

}
