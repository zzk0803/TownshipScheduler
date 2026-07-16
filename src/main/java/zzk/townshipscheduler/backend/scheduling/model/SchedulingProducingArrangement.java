package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import com.fasterxml.jackson.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import zzk.townshipscheduler.backend.OrderType;
import zzk.townshipscheduler.backend.ProducingStructureType;
import zzk.townshipscheduler.backend.scheduling.ArrangementIdRoller;
import zzk.townshipscheduler.backend.scheduling.algorithm.SchedulingDateTimeStrengthComparator;
import zzk.townshipscheduler.backend.scheduling.algorithm.SchedulingProducingArrangementDifficultyComparator;
import zzk.townshipscheduler.backend.utility.UuidGenerator;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;


@Slf4j
@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@PlanningEntity(comparatorClass = SchedulingProducingArrangementDifficultyComparator.class)
public class SchedulingProducingArrangement
        implements Serializable, Comparable<SchedulingProducingArrangement> {

    public static final String VALUE_RANGE_FOR_SCHEDULING_FACTORY_INSTANCE = "valueRangeForSchedulingFactoryInstance";

    public static final String PLANNING_FACTORY_INSTANCE = "planningFactoryInstance";

    public static final String PLANNING_DATE_TIME_SLOT = "planningDateTimeSlot";

    public static final String SHADOW_ARRANGE_DATE_TIME = "arrangeDateTime";

    public static final String SHADOW_PRODUCING_DATE_TIME = "producingDateTime";

    public static final String SHADOW_COMPLETED_DATE_TIME = "completedDateTime";

    @Serial
    private static final long serialVersionUID = 7524280731369623680L;

    @ToString.Include
    @EqualsAndHashCode.Include
    private int id;

    @PlanningId
    @EqualsAndHashCode.Include
    private UUID uuid;

    @JsonIdentityReference
    private SchedulingOrder schedulingOrder;

    @JsonIdentityReference
    private SchedulingProduct schedulingOrderProduct;

    @JsonIdentityReference
    private IGameArrangeObject targetActionObject;

    @JsonIdentityReference
    @ToString.Include
    private IGameArrangeObject currentActionObject;

    @JsonIgnore
    private SchedulingProducingArrangement supportProducingArrangement;

    @JsonIgnore
    private SchedulingProducingArrangement supportOrderProducingArrangement;

    @JsonBackReference
    private SequencedSet<SchedulingProducingArrangement> prerequisiteProducingArrangements = new LinkedHashSet<>();

    //@DeepPlanningClone
    @JsonBackReference
    @JsonIgnore
    private SequencedSet<SchedulingProducingArrangement> deepPrerequisiteProducingArrangements = new LinkedHashSet<>();

    @EqualsAndHashCode.Include
    private int prerequisiteProducingArrangementsSize;

    @EqualsAndHashCode.Include
    private int deepPrerequisiteProducingArrangementsSize;

    //    @JsonIgnore
//    @ShadowVariable(supplierName = "supplierForShadowPrerequisiteProducingArrangementsFinishedDateTime")
//    private LocalDateTime shadowPrerequisiteProducingArrangementsFinishedDateTime;

    private Duration staticDeepPrerequisiteProducingDuration;

    private Duration staticDeepProducingDuration;

    @JsonIgnore
    private SchedulingPlayer schedulingPlayer;

    @JsonIgnore
    private SchedulingWorkCalendar schedulingWorkCalendar;

    @JsonIgnore
    private SchedulingProducingExecutionMode producingExecutionMode;

    @PlanningVariable(
            valueRangeProviderRefs = VALUE_RANGE_FOR_SCHEDULING_FACTORY_INSTANCE
    )
    private SchedulingFactoryInstance planningFactoryInstance;

    @PlanningVariable(comparatorClass = SchedulingDateTimeStrengthComparator.class)
    private SchedulingDateTimeSlot planningDateTimeSlot;

    @ShadowVariable(supplierName = "supplierForFactoryProcessSequence")
    private FactoryProcessSequence factoryProcessSequence;

//    private FactoryProcessSequence _factoryProcessSequence;

    @ShadowVariable(supplierName = "supplierForComputedDateTimePair")
    private ComputedDateTimePair computedDateTimePair;

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

    @JsonProperty("producingDateTime")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ToString.Include
    public LocalDateTime getProducingDateTime() {
        return computedDateTimePair != null
                ? computedDateTimePair.producingDateTime()
                : null;
    }

    @ValueRangeProvider(id = VALUE_RANGE_FOR_SCHEDULING_FACTORY_INSTANCE)
    public List<SchedulingFactoryInstance> valueRangeForSchedulingFactoryInstance(TownshipSchedulingProblem townshipSchedulingProblem) {
        return townshipSchedulingProblem.getSchedulingFactoryInstanceList()
                .stream()
                .filter(schedulingFactoryInstance -> schedulingFactoryInstance.getSchedulingFactoryInfo()
                        .typeEqual(getRequiredFactoryInfo()))
                .toList();
    }

    @JsonIgnore
    public SchedulingFactoryInfo getRequiredFactoryInfo() {
        return getSchedulingProduct().getRequireFactory();
    }

    @JsonProperty("schedulingProduct")
    public SchedulingProduct getSchedulingProduct() {
        return (SchedulingProduct) getCurrentActionObject();
    }

    public FactoryReadableIdentifier getPlanningFactoryInstanceReadableIdentifier() {
        if (getPlanningFactoryInstance() == null) {
            return null;
        }
        return getPlanningFactoryInstance().getFactoryReadableIdentifier();
    }

//    @ShadowSources(value = {"schedulingPlayer.shadowComputedMap", "factoryProcessSequence"})
//    public LocalDateTime supplierForProducingDateTime() {
//        return schedulingPlayer.queryProducingDateTime(this);
//    }
//
//    @ShadowSources(value = {"schedulingPlayer.shadowComputedMap", "factoryProcessSequence"})
//    public LocalDateTime supplierForCompletedDateTime() {
//        return schedulingPlayer.queryCompletedDateTime(this);
//    }

    @ShadowSources({"planningFactoryInstance", "planningDateTimeSlot"})
    public FactoryProcessSequence supplierForFactoryProcessSequence() {
        if (planningFactoryInstance == null || planningDateTimeSlot == null) {
            return null;
        }
//        this._factoryProcessSequence = this.factoryProcessSequence;
        return toFactoryProcessSequence();
    }

    public FactoryProcessSequence toFactoryProcessSequence() {
        return new FactoryProcessSequence(this);
    }

//    @ShadowSources(value = {"prerequisiteProducingArrangements[].computedDateTimePair"})
//    public LocalDateTime supplierForShadowPrerequisiteProducingArrangementsFinishedDateTime(
//            TownshipSchedulingProblem townshipSchedulingProblem
//    ) {
//        if (this.prerequisiteProducingArrangements.stream()
//                .anyMatch(schedulingProducingArrangement -> schedulingProducingArrangement.getCompletedDateTime() == null)) {
//            return null;
//        }
//
//        return this.prerequisiteProducingArrangements.stream()
//                .map(SchedulingProducingArrangement::getCompletedDateTime)
//                .max(LocalDateTime::compareTo)
//                .orElse(townshipSchedulingProblem.getSchedulingWorkCalendar().getStartDateTime());
//    }

    @JsonProperty("completedDateTime")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ToString.Include
    public LocalDateTime getCompletedDateTime() {
        return computedDateTimePair != null
                ? computedDateTimePair.completedDateTime()
                : null;
    }

//    public Duration calcArrangeDateTimeToPrerequisiteDuration() {
//        if (getShadowPrerequisiteProducingArrangementsFinishedDateTime() == null) {
//            return getWorkCalendarSpan();
//        }
//
//        return Duration.between(getShadowPrerequisiteProducingArrangementsFinishedDateTime(), getArrangeDateTime());
//    }

    public Duration getWorkCalendarSpan() {
        return Duration.between(getWorkCalendarStart(), getWorkCalendarEnd());
    }

    public LocalDateTime getWorkCalendarEnd() {
        return getSchedulingWorkCalendar().getEndDateTime();
    }

    public LocalDateTime getWorkCalendarStart() {
        return getSchedulingWorkCalendar().getStartDateTime();
    }

    @ShadowSources(value = {"schedulingPlayer.shadowComputedMap", "factoryProcessSequence"})
    public ComputedDateTimePair supplierForComputedDateTimePair() {
        return schedulingPlayer.query(this);
    }

    public boolean weatherFactoryProducingTypeIsSlot() {
        return getFactoryProducingType() == ProducingStructureType.SLOT;
    }

    public boolean weatherFactoryProducingTypeIsQueue() {
        return getFactoryProducingType() == ProducingStructureType.QUEUE;
    }

    public ProducingStructureType getFactoryProducingType() {
        return getRequiredFactoryInfo().getProducingStructureType();
    }

    @JsonProperty("producingDuration")
    public Duration getProducingDuration() {
        return getProducingExecutionMode().getExecuteDuration();
    }

    @JsonIgnore
    public boolean isFactoryMatch() {
        return Objects.nonNull(getPlanningFactoryInstance()) && getPlanningFactoryInstance().getSchedulingFactoryInfo()
                .typeEqual(getSchedulingProduct().getRequireFactory());
    }

    public boolean boolPlanningAssigned() {
        return getPlanningFactoryInstance() != null && getPlanningDateTimeSlot() != null;
    }

    public boolean boolPlanningShadowVariableComputed() {
        return getFactoryProcessSequence() != null && getComputedDateTimePair() != null;
    }

    public void elementarySetup(
            ArrangementIdRoller idRoller,
            SchedulingPlayer schedulingPlayer
    ) {
        idRoller.setup(this);
        this.schedulingPlayer = schedulingPlayer;
    }

    public void advancedSetupOrThrow() {
        Objects.requireNonNull(this.getCurrentActionObject());
        Objects.requireNonNull(getId());
        Objects.requireNonNull(getUuid());
        Objects.requireNonNull(getSchedulingPlayer());
        setDeepPrerequisiteProducingArrangements(calcDeepPrerequisiteProducingArrangements());
        setDeepPrerequisiteProducingArrangementsSize(getDeepPrerequisiteProducingArrangements().size());
        setStaticDeepProducingDuration(calcStaticProducingDuration());
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

            Set<SchedulingProducingArrangement> prerequisites = current.getPrerequisiteProducingArrangements();
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

    public LocalDateTime calcStaticIdealArrangeDateTime() {
        return getSchedulingWorkCalendar().getStartDateTime().plus(getStaticDeepPrerequisiteProducingDuration());
    }

    public LocalDateTime calcStaticIdealArrangeDateTime(LocalDateTime argDateTime) {
        return argDateTime.plus(getStaticDeepPrerequisiteProducingDuration());
    }

    public LocalDateTime calcStaticIdealCompleteDateTime() {
        return getSchedulingWorkCalendar().getStartDateTime().plus(getStaticDeepProducingDuration());
    }

    public LocalDateTime calcStaticIdealCompleteDateTime(LocalDateTime argDateTime) {
        return argDateTime.plus(getStaticDeepProducingDuration());
    }

    protected <T extends SchedulingProducingArrangement> void appendPrerequisiteArrangements(
            List<T> prerequisiteArrangements
    ) {
        this.prerequisiteProducingArrangements.addAll(prerequisiteArrangements);
        this.prerequisiteProducingArrangementsSize = this.prerequisiteProducingArrangements.size();
        this.prerequisiteProducingArrangements.forEach(
                schedulingProducingArrangement -> schedulingProducingArrangement.setSupportProducingArrangement(this)
        );
    }

    public boolean boolBearSuccessorProducingArrangement() {
        return getSupportProducingArrangement() != null;
    }

    public boolean boolOrderDirect() {
        return getTargetActionObject() instanceof SchedulingOrder;
    }

    @Override
    public int compareTo(SchedulingProducingArrangement that) {
        return SchedulingProducingArrangementDifficultyComparator.INSTANCE.compare(this, that);
    }

    public boolean boolCompletedAfterCalendarEnd() {
        return getCompletedDateTime().isAfter(getWorkCalendarEnd());
    }

    public boolean boolHasDeadline() {
        return getSchedulingOrder().boolHasDeadline();
    }

    public boolean boolCompletedAfterDeadline() {
        return boolHasDeadline() && (getCompletedDateTime().isAfter(getDeadline()));
    }

    public LocalDateTime getDeadline() {
        return getSchedulingOrder().getDeadline();
    }

    public int getEvaluateFactor() {
        int factor = getDeadline() != null
                ? 100
                : 1;

        if (getPrerequisiteProducingArrangements().isEmpty()) {
            factor *= 5;
        }

        if (getRequiredFactoryInfo().getFactoryInstances().size() > 1) {
            factor *= 5;
        }

        if (weatherFactoryProducingTypeIsSlot()) {
            factor *= 5;
        }

        if (this.getSchedulingOrder() != null) {
            OrderType orderType = this.getSchedulingOrder().getOrderType();
            switch (orderType) {
                case TRAIN -> {
                    factor *= 10;
                }
                case AIRPLANE -> {
                    factor *= 100;
                }
            }
        }
        return factor;
    }

    public Duration calcDeadlineToCompletedDuration() {
        return Duration.between(
                getSchedulingOrder().getDeadline(),
                boolCompleted()
                        ? getCompletedDateTime()
                        : getWorkCalendarEnd()
        );
    }

    public boolean boolCompleted() {
        return getCompletedDateTime() != null;
    }

    public boolean boolCompletedInWorkCalendarOrDeadline() {
        if (!boolCompleted()) {
            return false;
        }

        LocalDateTime completedDateTime = getCompletedDateTime();
        LocalDateTime workCalendarEnd = getWorkCalendarEnd();
        LocalDateTime deadline = getDeadline();
        if (boolHasDeadline()) {
            return completedDateTime.isBefore(deadline);
        } else {
            return completedDateTime.isBefore(workCalendarEnd);
        }
    }

    public Duration calcCalendarEndToCompletedDuration() {
        LocalDateTime workCalendarEnd = getWorkCalendarEnd();
        return Duration.between(workCalendarEnd, getCompletedDateTime());
    }

    public Duration calcCalendarStartToArrangedDuration() {
        LocalDateTime workCalendarStart = getSchedulingWorkCalendar().getStartDateTime();
        return Duration.between(workCalendarStart, getArrangeDateTime());
    }

    @JsonProperty("arrangeDateTime")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ToString.Include
    public LocalDateTime getArrangeDateTime() {
        return this.planningDateTimeSlot != null
                ? this.planningDateTimeSlot.getStart()
                : null;
    }

    public Duration calcCalendarStartToCompletedDuration() {
        LocalDateTime workCalendarStart = getSchedulingWorkCalendar().getStartDateTime();
        return Duration.between(workCalendarStart, getCompletedDateTime());
    }

    public Duration calcCompletedDateTimeToCalendarEndDuration() {
        LocalDateTime endDateTime = getSchedulingWorkCalendar().getEndDateTime();
        return Duration.between(getCompletedDateTime(), endDateTime);
    }

    public Duration calcCompletedDateTimeToDeadline() {
        LocalDateTime deadline = getDeadline();
        return Duration.between(getCompletedDateTime(), deadline);
    }

//    public Duration calcPrerequisiteToArrangeDuration() {
//        if (getShadowPrerequisiteProducingArrangementsFinishedDateTime() == null) {
//            return Duration.MAX;
//        }
//        return Duration.between(getShadowPrerequisiteProducingArrangementsFinishedDateTime(), getArrangeDateTime()).abs();
//    }

    public Duration calcSleepArrangeDateTimeToNextAvailableDuration() {
        LocalTime arrangeTime = this.getArrangeDateTime().toLocalTime();
        LocalTime sleepStart = this.getSchedulingPlayer().getSleepStart();
        LocalTime sleepEnd = this.getSchedulingPlayer().getSleepEnd();
        if (arrangeTime.isAfter(sleepStart) && arrangeTime.isBefore(LocalTime.MAX)) {
            return Duration.between(sleepEnd, LocalTime.MAX).plus(Duration.between(LocalTime.MIDNIGHT, sleepEnd)).abs();
        } else if (arrangeTime.isAfter(LocalTime.MIDNIGHT) && arrangeTime.isBefore(sleepEnd)) {
            return Duration.between(sleepStart, sleepEnd).abs();
        } else {
            return Duration.ZERO;
        }
    }

    public boolean boolArrangeDateTimeInPlayerSleepTime() {
        LocalTime arrangeTime = this.getArrangeDateTime().toLocalTime();
        LocalTime sleepStart = this.getSchedulingPlayer().getSleepStart();
        LocalTime sleepEnd = this.getSchedulingPlayer().getSleepEnd();
        return (arrangeTime.isAfter(sleepStart) && arrangeTime.isBefore(LocalTime.MAX))
               || (arrangeTime.isAfter(LocalTime.MIDNIGHT) && arrangeTime.isBefore(sleepEnd));
    }

    public boolean hasMultipleLevelPrerequisiteArrangements() {
        return getDeepPrerequisiteProducingArrangementsSize() > getPrerequisiteProducingArrangementsSize();
    }

}
