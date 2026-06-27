package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.cloner.DeepPlanningClone;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@PlanningEntity
public class SchedulingFactoryInstance
        implements Serializable {

    @Serial
    private static final long serialVersionUID = -7146926432206516227L;

    @PlanningId
    @EqualsAndHashCode.Include
    private Integer id;

    private Long fieldFactoryId;

    @JsonIgnore
    @EqualsAndHashCode.Include
    private SchedulingFactoryInfo schedulingFactoryInfo;

    private int seqNum;

    private int producingLength;

    private int reapWindowSize;

    @Setter(AccessLevel.PRIVATE)
    private FactoryReadableIdentifier factoryReadableIdentifier;

    @JsonIgnore
    @InverseRelationShadowVariable(sourceVariableName = SchedulingProducingArrangement.PLANNING_FACTORY_INSTANCE)
    private List<SchedulingProducingArrangement> planningArrangementsSequence = new ArrayList<>();

    @DeepPlanningClone
    private TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair> shadowProcessSequenceToComputePairMap = new TreeMap<>();

    public void setupFactoryReadableIdentifier() {
        setFactoryReadableIdentifier(new FactoryReadableIdentifier(
                getCategoryName(),
                getSeqNum()
        ));
    }

    public String getCategoryName() {
        return schedulingFactoryInfo.getCategoryName();
    }

    public SequencedMap<FactoryProcessSequence, FactoryComputedDateTimePair> changeFactoryProcessSequence(
            SchedulingProducingArrangement schedulingProducingArrangement,
            NavigableMap<FactoryProcessSequence, FactoryComputedDateTimePair> statefulContainerAsMap
    ) {
        return changeFactoryProcessSequence(
                new FactoryProcessSequence(schedulingProducingArrangement),
                statefulContainerAsMap
        );
    }

    public SequencedMap<FactoryProcessSequence, FactoryComputedDateTimePair> changeFactoryProcessSequence(
            FactoryProcessSequence factoryProcessSequence,
            NavigableMap<FactoryProcessSequence, FactoryComputedDateTimePair> statefulContainerAsMap
    ) {
        List<FactoryProcessSequence> toRemoveSequences = statefulContainerAsMap.keySet()
                .stream()
                .filter(existProcessSequence -> existProcessSequence.getArrangementId()
                        .equals(factoryProcessSequence.getArrangementId()) && existProcessSequence.compareTo(factoryProcessSequence) != 0)
                .toList()
                ;

        for (FactoryProcessSequence filteredProcessSequence : toRemoveSequences) {
            this.removeFactoryProcessSequence(
                    filteredProcessSequence,
                    statefulContainerAsMap
            );
        }

        return this.addFactoryProcessSequence(
                factoryProcessSequence,
                statefulContainerAsMap
        );
    }

    public SequencedMap<FactoryProcessSequence, FactoryComputedDateTimePair> addFactoryProcessSequence(
            FactoryProcessSequence factoryProcessSequence,
            NavigableMap<FactoryProcessSequence, FactoryComputedDateTimePair> statefulContainerAsMap
    ) {
        SequencedMap<FactoryProcessSequence, FactoryComputedDateTimePair> resultContainerAsMap = new LinkedHashMap<>();

        if (weatherFactoryProducingTypeIsSlot()) {
            statefulContainerAsMap.put(
                    factoryProcessSequence,
                    new FactoryComputedDateTimePair(
                            factoryProcessSequence.getArrangeDateTime(),
                            factoryProcessSequence.getArrangeDateTime().plus(factoryProcessSequence.getProducingDuration())
                    )
            );
            resultContainerAsMap.put(
                    factoryProcessSequence,
                    new FactoryComputedDateTimePair(
                            factoryProcessSequence.getArrangeDateTime(),
                            factoryProcessSequence.getArrangeDateTime().plus(factoryProcessSequence.getProducingDuration())
                    )
            );
            return resultContainerAsMap;
        }

        FactoryComputedDateTimePair computedDateTimePair = computeDateTimePair(
                factoryProcessSequence,
                statefulContainerAsMap
        );

        statefulContainerAsMap.put(
                factoryProcessSequence,
                computedDateTimePair
        );
        resultContainerAsMap.put(
                factoryProcessSequence,
                computedDateTimePair
        );

        return cascadeUpdatesAfter(
                factoryProcessSequence,
                resultContainerAsMap
        );
    }

    private FactoryComputedDateTimePair computeDateTimePair(
            FactoryProcessSequence factoryProcessSequence,
            NavigableMap<FactoryProcessSequence, FactoryComputedDateTimePair> statefulContainerAsMap
    ) {
        Map.Entry<FactoryProcessSequence, FactoryComputedDateTimePair> prevEntry =
                statefulContainerAsMap.lowerEntry(factoryProcessSequence);

        LocalDateTime producingDateTime = calcProducingDateTime(
                factoryProcessSequence,
                prevEntry
        );
        LocalDateTime completedDateTime = calcCompletedDateTime(
                factoryProcessSequence,
                producingDateTime
        );
        return new FactoryComputedDateTimePair(
                producingDateTime,
                completedDateTime
        );
    }

    private LocalDateTime calcCompletedDateTime(
            FactoryProcessSequence factoryProcessSequence,
            LocalDateTime producingDateTime
    ) {
        return producingDateTime.plus(factoryProcessSequence.getProducingDuration());
    }

    private LocalDateTime calcProducingDateTime(
            FactoryProcessSequence factoryProcessSequence,
            Map.Entry<FactoryProcessSequence, FactoryComputedDateTimePair> previousEntry
    ) {
        LocalDateTime previousCompleted = (previousEntry == null)
                ? null
                : previousEntry.getValue().completedDateTime();

        LocalDateTime arrangeDateTime = factoryProcessSequence.getArrangeDateTime();
        return (previousCompleted == null || arrangeDateTime.isAfter(previousCompleted))
                ? arrangeDateTime
                : previousCompleted;
    }

    private SequencedMap<FactoryProcessSequence, FactoryComputedDateTimePair> cascadeUpdatesAfter(
            FactoryProcessSequence fromSequence,
            SequencedMap<FactoryProcessSequence, FactoryComputedDateTimePair> resultContainerAsMap
    ) {
        return cascadeUpdatesAfter(
                fromSequence,
                resultContainerAsMap,
                this.shadowProcessSequenceToComputePairMap
        );
    }

    public boolean weatherFactoryProducingTypeIsSlot() {
        return getSchedulingFactoryInfo().weatherFactoryProducingTypeIsSlot();
    }

    public SequencedMap<FactoryProcessSequence, FactoryComputedDateTimePair> removeFactoryProcessSequence(
            FactoryProcessSequence factoryProcessSequence,
            NavigableMap<FactoryProcessSequence, FactoryComputedDateTimePair> statefulContainerAsMap
    ) {
        SequencedMap<FactoryProcessSequence, FactoryComputedDateTimePair> resultContainerAsMap = new LinkedHashMap<>();

        if (!statefulContainerAsMap.containsKey(factoryProcessSequence)) {
            return resultContainerAsMap;
        }
        statefulContainerAsMap.remove(factoryProcessSequence);
        resultContainerAsMap.put(
                factoryProcessSequence,
                null
        );

        if (weatherFactoryProducingTypeIsQueue()) {
            return cascadeUpdatesAfter(
                    factoryProcessSequence,
                    resultContainerAsMap,
                    statefulContainerAsMap
            );
        }

        return resultContainerAsMap;
    }

    private SequencedMap<FactoryProcessSequence, FactoryComputedDateTimePair> cascadeUpdatesAfter(
            FactoryProcessSequence fromSequence,
            SequencedMap<FactoryProcessSequence, FactoryComputedDateTimePair> resultContainerAsMap,
            NavigableMap<FactoryProcessSequence, FactoryComputedDateTimePair> statefulContainerAsMap
    ) {
        NavigableMap<FactoryProcessSequence, FactoryComputedDateTimePair> tailMap = statefulContainerAsMap.tailMap(
                fromSequence,
                false
        );

        if (tailMap.isEmpty()) {
            return resultContainerAsMap;
        }

        Map.Entry<FactoryProcessSequence, FactoryComputedDateTimePair> startPrevEntry =
                statefulContainerAsMap.lowerEntry(tailMap.firstKey());

        LocalDateTime currentCompleted = (startPrevEntry != null)
                ? startPrevEntry.getValue().completedDateTime()
                : null;

        Set<FactoryProcessSequence> iteratingSet = tailMap.keySet();
        for (FactoryProcessSequence current : iteratingSet) {
            FactoryComputedDateTimePair existingPair = statefulContainerAsMap.get(current);
            LocalDateTime arrangeDateTime = current.getArrangeDateTime();

            LocalDateTime newProducing = (currentCompleted == null || arrangeDateTime.isAfter(currentCompleted))
                    ? arrangeDateTime
                    : currentCompleted;
            LocalDateTime newCompleted = newProducing.plus(current.getProducingDuration());

            if (existingPair != null && existingPair.producingDateTime().equals(newProducing) && existingPair.completedDateTime()
                    .equals(newCompleted)) {
                break;
            }

            statefulContainerAsMap.put(
                    current,
                    new FactoryComputedDateTimePair(
                            newProducing,
                            newCompleted
                    )
            );
            resultContainerAsMap.put(
                    current,
                    new FactoryComputedDateTimePair(
                            newProducing,
                            newCompleted
                    )
            );
            currentCompleted = newCompleted;
        }
        return resultContainerAsMap;
    }

    public boolean weatherFactoryProducingTypeIsQueue() {
        return this.getSchedulingFactoryInfo()
                .weatherFactoryProducingTypeIsQueue();
    }

    public SequencedMap<FactoryProcessSequence, FactoryComputedDateTimePair> addFactoryProcessSequence(
            SchedulingProducingArrangement schedulingProducingArrangement
    ) {
        return addFactoryProcessSequence(new FactoryProcessSequence(schedulingProducingArrangement));
    }

    public SequencedMap<FactoryProcessSequence, FactoryComputedDateTimePair> addFactoryProcessSequence(
            FactoryProcessSequence factoryProcessSequence
    ) {
        return addFactoryProcessSequence(
                factoryProcessSequence,
                this.shadowProcessSequenceToComputePairMap
        );
    }

    public SequencedMap<FactoryProcessSequence, FactoryComputedDateTimePair> addFactoryProcessSequence(
            SchedulingProducingArrangement schedulingProducingArrangement,
            NavigableMap<FactoryProcessSequence, FactoryComputedDateTimePair> statefulContainerAsMap
    ) {
        return addFactoryProcessSequence(
                new FactoryProcessSequence(schedulingProducingArrangement),
                statefulContainerAsMap
        );
    }

    private @NonNull FactoryComputedDateTimePair computeDateTimePair(FactoryProcessSequence factoryProcessSequence) {
        return this.computeDateTimePair(
                factoryProcessSequence,
                this.shadowProcessSequenceToComputePairMap
        );
    }

    private LocalDateTime calcProducingDateTime(
            FactoryProcessSequence factoryProcessSequence,
            LocalDateTime previousCompletedDateTime
    ) {
        LocalDateTime arrangeDateTime = factoryProcessSequence.getArrangeDateTime();
        return (previousCompletedDateTime == null || arrangeDateTime.isAfter(previousCompletedDateTime))
                ? arrangeDateTime
                : previousCompletedDateTime;
    }

    public SequencedMap<FactoryProcessSequence, FactoryComputedDateTimePair> removeFactoryProcessSequence(
            SchedulingProducingArrangement schedulingProducingArrangement
    ) {
        return removeFactoryProcessSequence(new FactoryProcessSequence(schedulingProducingArrangement));
    }

    public SequencedMap<FactoryProcessSequence, FactoryComputedDateTimePair> removeFactoryProcessSequence(
            FactoryProcessSequence factoryProcessSequence
    ) {
        return removeFactoryProcessSequence(
                factoryProcessSequence,
                this.shadowProcessSequenceToComputePairMap
        );
    }

    public SequencedMap<FactoryProcessSequence, FactoryComputedDateTimePair> removeFactoryProcessSequence(
            SchedulingProducingArrangement schedulingProducingArrangement,
            NavigableMap<FactoryProcessSequence, FactoryComputedDateTimePair> statefulContainerAsMap
    ) {
        return removeFactoryProcessSequence(
                new FactoryProcessSequence(schedulingProducingArrangement),
                statefulContainerAsMap
        );
    }

    private LocalDateTime calcProducingDateTime(
            FactoryProcessSequence factoryProcessSequence,
            FactoryComputedDateTimePair previousComputedPair
    ) {
        LocalDateTime previousCompleted = (previousComputedPair == null)
                ? null
                : previousComputedPair.completedDateTime();

        LocalDateTime arrangeDateTime = factoryProcessSequence.getArrangeDateTime();
        return (previousCompleted == null || arrangeDateTime.isAfter(previousCompleted))
                ? arrangeDateTime
                : previousCompleted;
    }

    public NavigableMap<FactoryProcessSequence, FactoryComputedDateTimePair> prepareProducingAndCompletedMap() {
        return Collections.unmodifiableNavigableMap(this.shadowProcessSequenceToComputePairMap);
    }

    public FactoryComputedDateTimePair query(FactoryProcessSequence factoryProcessSequence) {
        return this.shadowProcessSequenceToComputePairMap.get(factoryProcessSequence);
    }

    @Override
    public String toString() {
        return "SchedulingFactoryInstance{" + "readableIdentifier='" + factoryReadableIdentifier + '\'' + ", producingLength=" + producingLength +
                ", " +
                "reapWindowSize=" + reapWindowSize + '}';
    }

    public boolean typeEqual(SchedulingFactoryInstance that) {
        return this.getSchedulingFactoryInfo()
                .typeEqual(that.getSchedulingFactoryInfo());
    }

    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @Value
    public static class FactoryProcessSequence
            implements Comparable<FactoryProcessSequence>, Serializable {

        public static final Function<SchedulingProducingArrangement, Integer> DEFAULT_SEQUENTIAL_ID_FUNCTION =
                SchedulingProducingArrangement::getId;

        public static final Comparator<FactoryProcessSequence> COMPARATOR =
                Comparator.comparing(FactoryProcessSequence::getSchedulingFactoryInstanceReadableIdentifier)
                        .thenComparing(FactoryProcessSequence::getArrangeDateTime)
                        .thenComparing(FactoryProcessSequence::getSequentialId)
                        .thenComparing(FactoryProcessSequence::getArrangementId)
                ;

        @Serial
        private static final long serialVersionUID = -2296858586337992130L;

        @EqualsAndHashCode.Include
        LocalDateTime arrangeDateTime;

        @EqualsAndHashCode.Include
        Integer arrangementId;

        @EqualsAndHashCode.Include
        Integer sequentialId;

        @EqualsAndHashCode.Include
        FactoryReadableIdentifier schedulingFactoryInstanceReadableIdentifier;

        Duration producingDuration;

        public FactoryProcessSequence(
                SchedulingProducingArrangement schedulingProducingArrangement
        ) {
            this(
                    schedulingProducingArrangement,
                    DEFAULT_SEQUENTIAL_ID_FUNCTION
            );
        }

        public FactoryProcessSequence(
                SchedulingProducingArrangement schedulingProducingArrangement,
                Function<SchedulingProducingArrangement, Integer> sequentialIdFunction
        ) {
            this.arrangementId = schedulingProducingArrangement.getId();
            this.arrangeDateTime = schedulingProducingArrangement.getArrangeDateTime();
            this.schedulingFactoryInstanceReadableIdentifier = schedulingProducingArrangement.getPlanningFactoryInstance()
                    .getFactoryReadableIdentifier();
            this.producingDuration = schedulingProducingArrangement.getProducingDuration();
            this.sequentialId = sequentialIdFunction.apply(schedulingProducingArrangement);
        }

        @Override
        public int compareTo(FactoryProcessSequence that) {
            return COMPARATOR.compare(
                    this,
                    that
            );
        }

    }

    public static record FactoryComputedDateTimePair(
            LocalDateTime producingDateTime,
            LocalDateTime completedDateTime
    )
            implements Serializable, Comparable<FactoryComputedDateTimePair> {

        @Override
        public int compareTo(FactoryComputedDateTimePair that) {
            return Comparator.comparing(FactoryComputedDateTimePair::producingDateTime)
                    .thenComparing(FactoryComputedDateTimePair::completedDateTime)
                    .compare(
                            this,
                            that
                    )
                    ;
        }

    }

}
