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
import zzk.townshipscheduler.backend.ProducingStructureType;
import zzk.townshipscheduler.backend.scheduling.algorithm.SchedulingDateTimeStrengthComparator;
import zzk.townshipscheduler.backend.scheduling.algorithm.SchedulingProducingArrangementDifficultyComparator;
import zzk.townshipscheduler.backend.scheduling.ArrangementIdRoller;
import zzk.townshipscheduler.backend.utility.UuidGenerator;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

import static zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInstance.FactoryProcessSequence;


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

    public static final String SHADOW_DEEP_PREREQUISITE_PRODUCING_ARRANGEMENTS_FINISHED_DATE_TIME =
            "shadowDeepPrerequisiteProducingArrangementsFinishedDateTime";

    @Serial
    private static final long serialVersionUID = 7524280731369623680L;

    @ToString.Include
    @EqualsAndHashCode.Include
    private Integer id;

    @PlanningId
    @EqualsAndHashCode.Include
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
    private SequencedSet<SchedulingProducingArrangement> prerequisiteProducingArrangements = new LinkedHashSet<>();

    //@DeepPlanningClone
    @JsonBackReference
    @JsonIgnore
    private SequencedSet<SchedulingProducingArrangement> deepPrerequisiteProducingArrangements = new LinkedHashSet<>();

    private int prerequisiteProducingArrangementsSize;

    @EqualsAndHashCode.Include
    private int deepPrerequisiteProducingArrangementsSize;

//    @JsonIgnore
//    @ShadowVariable(supplierName = "supplierForShadowPrerequisiteProducingArrangementsFinishedDateTime")
//    private LocalDateTime shadowPrerequisiteProducingArrangementsFinishedDateTime;

    @JsonIgnore
    private SchedulingProducingArrangement successorProducingArrangement;

    private Duration staticDeepPrerequisiteProducingDuration;

    private Duration staticDeepProducingDuration;

    @JsonIgnore
    private SchedulingPlayer schedulingPlayer;

    @JsonIgnore
    private SchedulingProducingExecutionMode producingExecutionMode;

    @PlanningVariable(valueRangeProviderRefs = VALUE_RANGE_FOR_SCHEDULING_FACTORY_INSTANCE)
    private SchedulingFactoryInstance planningFactoryInstance;

    @PlanningVariable(comparatorClass = SchedulingDateTimeStrengthComparator.class)
    private SchedulingDateTimeSlot planningDateTimeSlot;

    @ShadowVariable(supplierName = "supplierForFactoryProcessSequence")
    private FactoryProcessSequence factoryProcessSequence;

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

    @ValueRangeProvider(id = VALUE_RANGE_FOR_SCHEDULING_FACTORY_INSTANCE)
    public List<SchedulingFactoryInstance> valueRangeForSchedulingFactoryInstance(TownshipSchedulingProblem townshipSchedulingProblem) {
        return townshipSchedulingProblem.getSchedulingFactoryInstanceList().stream()
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

    @ShadowSources(value = {"schedulingPlayer.shadowComputedMap", "factoryProcessSequence"})
    public LocalDateTime supplierForProducingDateTime() {
        return schedulingPlayer.queryProducingDateTime(this);
    }

    @ShadowSources(value = {"schedulingPlayer.shadowComputedMap", "factoryProcessSequence"})
    public LocalDateTime supplierForCompletedDateTime() {
        return schedulingPlayer.queryCompletedDateTime(this);
    }

//    @ShadowSources(value = {"prerequisiteProducingArrangements[].completedDateTime"})
//    public LocalDateTime supplierForShadowPrerequisiteProducingArrangementsFinishedDateTime(TownshipSchedulingProblem
//    townshipSchedulingProblem) {
//        if (this.prerequisiteProducingArrangements.stream()
//                .anyMatch(schedulingProducingArrangement -> schedulingProducingArrangement.completedDateTime == null)) {
//            return null;
//        }
//
//        return this.prerequisiteProducingArrangements.stream()
//                .map(SchedulingProducingArrangement::getCompletedDateTime)
//                .max(LocalDateTime::compareTo)
//                .orElse(townshipSchedulingProblem.getSchedulingWorkCalendar().getStartDateTime());
//    }

    @ShadowSources({"planningFactoryInstance", "planningDateTimeSlot"})
    public FactoryProcessSequence supplierForFactoryProcessSequence() {
        if (planningFactoryInstance == null || planningDateTimeSlot == null) {
            return factoryProcessSequence;
        }
        return toFactoryProcessSequence();
    }

    public FactoryProcessSequence toFactoryProcessSequence() {
        return new FactoryProcessSequence(this);
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

    public boolean boolPlanningWellBeing() {
        return Stream.of(
                        getPlanningFactoryInstance(),
                        getPlanningDateTimeSlot()
                )
                .allMatch(Objects::nonNull);
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

    public void setArrangeDateTime(LocalDateTime localDateTime) {
        throw new UnsupportedOperationException();
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

    public void advancedSetupOrThrow() {
        Objects.requireNonNull(this.getCurrentActionObject());
        Objects.requireNonNull(getId());
        Objects.requireNonNull(getUuid());
        Objects.requireNonNull(getSchedulingPlayer());
//        Objects.requireNonNull(getSchedulingWorkCalendar());
        setDeepPrerequisiteProducingArrangements(calcDeepPrerequisiteProducingArrangements());
        setDeepPrerequisiteProducingArrangementsSize(getDeepPrerequisiteProducingArrangements().size());
        setStaticDeepProducingDuration(calcStaticProducingDuration());
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
                .orElse(Duration.ZERO)
                ;
        setStaticDeepPrerequisiteProducingDuration(prerequisiteStaticProducingDuration);
        return selfDuration.plus(prerequisiteStaticProducingDuration);
    }

    public LocalDateTime calcStaticArrangeDateTime(LocalDateTime argDateTime) {
        return argDateTime.plus(getStaticDeepPrerequisiteProducingDuration());
    }

    public LocalDateTime calcStaticCompleteDateTime(LocalDateTime argDateTime) {
        return argDateTime.plus(getStaticDeepProducingDuration());
    }

    protected <T extends SchedulingProducingArrangement> void appendPrerequisiteArrangements(
            List<T> prerequisiteArrangements
    ) {
        this.prerequisiteProducingArrangements.addAll(prerequisiteArrangements);
        this.prerequisiteProducingArrangementsSize = this.prerequisiteProducingArrangements.size();
        this.prerequisiteProducingArrangements.forEach(
                schedulingProducingArrangement -> schedulingProducingArrangement.setSuccessorProducingArrangement(this)
        );
    }

    public boolean boolBearSuccessorProducingArrangement() {
        return getSuccessorProducingArrangement() != null;
    }

    public boolean boolOrderDirect() {
        return getTargetActionObject() instanceof SchedulingOrder;
    }

    public boolean boolCallerDeepPrerequisiteToArg(SchedulingProducingArrangement schedulingProducingArrangement) {
        return schedulingProducingArrangement.getDeepPrerequisiteProducingArrangements().contains(this);
    }

    public boolean boolCallerDirectPrerequisiteToArg(SchedulingProducingArrangement schedulingProducingArrangement) {
        return schedulingProducingArrangement.getPrerequisiteProducingArrangements().contains(this);
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

    @Override
    public int compareTo(SchedulingProducingArrangement that) {
        return SchedulingProducingArrangementDifficultyComparator.INSTANCE.compare(
                this,
                that
        );
    }

}
