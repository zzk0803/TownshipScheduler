package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeFactory;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.*;
import com.fasterxml.jackson.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import zzk.townshipscheduler.backend.ProducingStructureType;
import zzk.townshipscheduler.backend.scheduling.model.utility.SchedulingProducingArrangementDifficultyComparator;
import zzk.townshipscheduler.backend.utility.UuidGenerator;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;


@Slf4j
@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@PlanningEntity(comparatorClass = SchedulingProducingArrangementDifficultyComparator.class)
public class SchedulingProducingArrangement
        implements Serializable {

    public static final String VALUE_RANGE_FOR_DATE_TIME_SLOT_DELAY = "valueRangeForDateTimeSlotDelay";

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
    private static final long serialVersionUID = 7524280731369623680L;


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
    private SequencedSet<SchedulingProducingArrangement> prerequisiteProducingArrangements = new LinkedHashSet<>();

    //@DeepPlanningClone
    @JsonBackReference
    @JsonIgnore
    private SequencedSet<SchedulingProducingArrangement> deepPrerequisiteProducingArrangements = new LinkedHashSet<>();

    @EqualsAndHashCode.Include
    private int deepPrerequisiteProducingArrangementsSize;

    @JsonIgnore
    @ShadowVariable(supplierName = "supplierForShadowDeepPrerequisiteProducingArrangementsFinishedDateTime")
    private LocalDateTime shadowDeepPrerequisiteProducingArrangementsFinishedDateTime;

    @JsonIgnore
    private SchedulingProducingArrangement successorProducingArrangement;

    private Duration staticDeepPrerequisiteProducingDuration;

    @EqualsAndHashCode.Include
    private Duration staticDeepProducingDuration;

    @JsonIgnore
    private SchedulingPlayer schedulingPlayer;

    @JsonIgnore
    private SchedulingProducingExecutionMode producingExecutionMode;

    @JsonIgnore
    @InverseRelationShadowVariable(
            sourceVariableName = SchedulingFactoryInstance.PLANNING_ARRANGEMENTS_SEQUENCE
    )
    private SchedulingFactoryInstance planningFactoryInstance;

    @JsonIgnore
    @PlanningVariable(valueRangeProviderRefs = VALUE_RANGE_FOR_DATE_TIME_SLOT_DELAY)
    private Integer planningDelaySlot;

    @JsonIgnore
    @ShadowVariable(supplierName = "supplierForShadowDateTimeSlot")
    private SchedulingDateTimeSlot shadowDateTimeSlot;

    @PreviousElementShadowVariable(
            sourceVariableName = SchedulingFactoryInstance.PLANNING_ARRANGEMENTS_SEQUENCE
    )
    private SchedulingProducingArrangement previousProducingArrangement;

    @NextElementShadowVariable(
            sourceVariableName = SchedulingFactoryInstance.PLANNING_ARRANGEMENTS_SEQUENCE
    )
    private SchedulingProducingArrangement nextProducingArrangement;

    @IndexShadowVariable(
            sourceVariableName = SchedulingFactoryInstance.PLANNING_ARRANGEMENTS_SEQUENCE
    )
    private Integer indexInFactory;

    @ShadowVariablesInconsistent
    private Boolean shadowVariablesInconsistent;

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

    @ShadowSources(value = {"deepPrerequisiteProducingArrangements[].completedDateTime"})
    public LocalDateTime supplierForShadowDeepPrerequisiteProducingArrangementsFinishedDateTime(TownshipSchedulingProblem townshipSchedulingProblem) {
        if (this.deepPrerequisiteProducingArrangements.stream()
                .anyMatch(schedulingProducingArrangement -> schedulingProducingArrangement.completedDateTime == null)) {
            return null;
        }

        return this.deepPrerequisiteProducingArrangements.stream()
                .map(SchedulingProducingArrangement::getCompletedDateTime)
                .max(LocalDateTime::compareTo)
                .orElse(
                        townshipSchedulingProblem.getSchedulingWorkCalendar()
                                .getStartDateTime()
                );
    }

    @ShadowSources(
            value = {
                    "planningDelaySlot",
                    "shadowDeepPrerequisiteProducingArrangementsFinishedDateTime",
                    "previousProducingArrangement.planningDelaySlot",
                    "previousProducingArrangement.shadowDateTimeSlot"
            }
    )
    public SchedulingDateTimeSlot supplierForShadowDateTimeSlot(TownshipSchedulingProblem townshipSchedulingProblem) {
        if (this.shadowDeepPrerequisiteProducingArrangementsFinishedDateTime == null) {
            return null;
        }

        if (this.weatherFactoryProducingTypeIsQueue()) {
            if (previousProducingArrangement == null) {
                return townshipSchedulingProblem.getDateTimeSlotWithMinDateTimeAndDelayAmount(
                        this.shadowDeepPrerequisiteProducingArrangementsFinishedDateTime,
                        this.planningDelaySlot
                );
            }
            else {
                SchedulingDateTimeSlot previousArrangementShadowDateTimeSlot = previousProducingArrangement.shadowDateTimeSlot;
                if (previousArrangementShadowDateTimeSlot == null) {
                    return null;
                }
                return townshipSchedulingProblem.getDateTimeSlotWithMinDateTimeAndDelayAmount(
                        ObjectUtils.max(
                                this.shadowDeepPrerequisiteProducingArrangementsFinishedDateTime,
                                previousArrangementShadowDateTimeSlot.getStart()
                        ),
                        this.planningDelaySlot
                );
            }
        }

        return townshipSchedulingProblem.getDateTimeSlotWithMinDateTimeAndDelayAmount(
                this.shadowDeepPrerequisiteProducingArrangementsFinishedDateTime,
                this.planningDelaySlot
        );

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

    @ShadowSources(
            {
                    "shadowDeepPrerequisiteProducingArrangementsFinishedDateTime",
                    "shadowDateTimeSlot",
                    "previousProducingArrangement.completedDateTime",
                    "planningFactoryInstance"
            }
    )
    public LocalDateTime supplierForProducingDateTime() {
        LocalDateTime producingDateTime = null;
        if (shadowDateTimeSlot == null || planningFactoryInstance == null) {
            return null;
        }

        final LocalDateTime arrangeDateTime = shadowDateTimeSlot.getStart();
        switch (getFactoryProducingType()) {
            case QUEUE -> {
                if (previousProducingArrangement != null) {
                    producingDateTime = ObjectUtils.max(
                            arrangeDateTime,
                            shadowDeepPrerequisiteProducingArrangementsFinishedDateTime,
                            previousProducingArrangement.completedDateTime
                    );
                }
                else {
                    producingDateTime = ObjectUtils.max(
                            arrangeDateTime,
                            shadowDeepPrerequisiteProducingArrangementsFinishedDateTime
                    );
                }
            }

            case SLOT -> {
                producingDateTime = ObjectUtils.max(
                        arrangeDateTime,
                        shadowDeepPrerequisiteProducingArrangementsFinishedDateTime
                );
            }
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

    public boolean boolPlanningWellBeing() {
        return Stream.of(
                        getPlanningFactoryInstance(),
                        getShadowDateTimeSlot(),
                        getProducingDateTime(),
                        getCompletedDateTime()
                )
                .allMatch(Objects::nonNull);
    }

    @JsonProperty("arrangeDateTime")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ToString.Include
    public LocalDateTime getArrangeDateTime() {
        return this.shadowDateTimeSlot != null
                ? this.shadowDateTimeSlot.getStart()
                : null;
    }

    public void setArrangeDateTime(LocalDateTime localDateTime) {
        throw new UnsupportedOperationException();
    }

    public void advancedSetupOrThrow() {
        Objects.requireNonNull(this.getCurrentActionObject());
        Objects.requireNonNull(getId());
        Objects.requireNonNull(getUuid());
        Objects.requireNonNull(getSchedulingPlayer());
//        Objects.requireNonNull(getSchedulingWorkCalendar());
        SequencedSet<SchedulingProducingArrangement> calcDeepPrerequisiteProducingArrangements = calcDeepPrerequisiteProducingArrangements();
        setDeepPrerequisiteProducingArrangements(calcDeepPrerequisiteProducingArrangements);
        setDeepPrerequisiteProducingArrangementsSize(calcDeepPrerequisiteProducingArrangements.size());
        setStaticDeepProducingDuration(calcStaticProducingDuration());
    }

    public void elementarySetup(
            ArrangementIdRoller idRoller,
//            SchedulingWorkCalendar workTimeLimit,
            SchedulingPlayer schedulingPlayer
    ) {
        idRoller.setup(this);
//        this.schedulingWorkCalendar = workTimeLimit;
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

    private SequencedSet<SchedulingProducingArrangement> calcDeepPrerequisiteProducingArrangements() {
        LinkedList<SchedulingProducingArrangement> queue = new LinkedList<>(List.of(this));
        Set<SchedulingProducingArrangement> visited = new HashSet<>();
        LinkedHashSet<SchedulingProducingArrangement> result = new LinkedHashSet<>();

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

    public boolean boolCallerDeepPrerequisiteToArg(SchedulingProducingArrangement schedulingProducingArrangement) {
        return schedulingProducingArrangement.getDeepPrerequisiteProducingArrangements()
                .contains(this);
    }

    public boolean boolCallerDirectPrerequisiteToArg(SchedulingProducingArrangement schedulingProducingArrangement) {
        return schedulingProducingArrangement.getPrerequisiteProducingArrangements()
                .contains(this);
    }

    public boolean weatherPrerequisiteRequire() {
        return !getDeepPrerequisiteProducingArrangements().isEmpty();
    }

    public boolean boolHasDeadline() {
        return getSchedulingOrder().boolHasDeadline();
    }

    public LocalDateTime getDeadline() {
        return getSchedulingOrder().getDeadline();
    }

    @ValueRangeProvider(id = VALUE_RANGE_FOR_DATE_TIME_SLOT_DELAY)
    public ValueRange<Integer> valueRangeForDateTimeSlotDelay(TownshipSchedulingProblem townshipSchedulingProblem) {
//        int slotMinutes = townshipSchedulingProblem.getDateTimeSlotSize()
//                .getMinute();
//        var startDateTime = townshipSchedulingProblem.getSchedulingWorkCalendar()
//                .getStartDateTime();
//        var endDateTime = townshipSchedulingProblem.getSchedulingWorkCalendar()
//                .getEndDateTime();
//        int slotCount = SchedulingDateTimeSlot.calcLocalDateTimePairSlotCount(
//                startDateTime.plus(getStaticDeepPrerequisiteProducingDuration())
//                , endDateTime
//                , slotMinutes
//        );
        return ValueRangeFactory.createIntValueRange(0, 5);
    }

}
