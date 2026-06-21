package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.solution.cloner.DeepPlanningClone;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.jspecify.annotations.NonNull;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@PlanningEntity
public class SchedulingFactoryInstance
        implements Serializable {

    @Serial
    private static final long serialVersionUID = -4151844387461751037L;

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
    private List<SchedulingProducingArrangement> planningFactoryInstanceProducingArrangements = new ArrayList<>();

    @DeepPlanningClone
    private TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair> shadowProcessSequenceToComputePairMap = new TreeMap<>();

    public static TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair> createStatefulContainer() {
        return new TreeMap<>();
    }

    public void setupFactoryReadableIdentifier() {
        setFactoryReadableIdentifier(new FactoryReadableIdentifier(getCategoryName(), getSeqNum()));
    }

    public String getCategoryName() {
        return schedulingFactoryInfo.getCategoryName();
    }

    public Map<FactoryProcessSequence, FactoryComputedDateTimePair> changeFactoryProcessSequence(
            SchedulingProducingArrangement schedulingProducingArrangement,
            TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair> statefulContainerAsMap
    ) {
        return changeFactoryProcessSequence(
                new FactoryProcessSequence(schedulingProducingArrangement),
                statefulContainerAsMap
        );
    }

    public Map<FactoryProcessSequence, FactoryComputedDateTimePair> changeFactoryProcessSequence(
            FactoryProcessSequence factoryProcessSequence,
            TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair> statefulContainerAsMap
    ) {
        statefulContainerAsMap.keySet()
                .stream()
                .filter(
                        existProcessSequence -> existProcessSequence.getArrangementId()
                                                        .equals(factoryProcessSequence.getArrangementId())
                                                && existProcessSequence.compareTo(factoryProcessSequence) != 0
                )
                .forEach(filteredProcessSequence -> {
                    this.removeFactoryProcessSequence(filteredProcessSequence, statefulContainerAsMap);
                })
        ;
        return this.addFactoryProcessSequence(factoryProcessSequence, statefulContainerAsMap);
    }

    public Map<FactoryProcessSequence, FactoryComputedDateTimePair> addFactoryProcessSequence(
            SchedulingProducingArrangement schedulingProducingArrangement
    ) {
        return addFactoryProcessSequence(new FactoryProcessSequence(schedulingProducingArrangement));
    }

    public Map<FactoryProcessSequence, FactoryComputedDateTimePair> addFactoryProcessSequence(
            FactoryProcessSequence factoryProcessSequence
    ) {
        return addFactoryProcessSequence(factoryProcessSequence, this.shadowProcessSequenceToComputePairMap);
    }

    public Map<FactoryProcessSequence, FactoryComputedDateTimePair> addFactoryProcessSequence(
            FactoryProcessSequence factoryProcessSequence,
            TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair> statefulContainerAsMap
    ) {
        Map<FactoryProcessSequence, FactoryComputedDateTimePair> result = new LinkedHashMap<>();

        if (!weatherFactoryProducingTypeIsQueue()) {
            statefulContainerAsMap.put(
                    factoryProcessSequence,
                    new FactoryComputedDateTimePair(
                            factoryProcessSequence.getArrangeDateTime(),
                            factoryProcessSequence.getArrangeDateTime()
                                    .plus(factoryProcessSequence.getProducingDuration())
                    )
            );
            result.put(
                    factoryProcessSequence,
                    new FactoryComputedDateTimePair(
                            factoryProcessSequence.getArrangeDateTime(),
                            factoryProcessSequence.getArrangeDateTime()
                                    .plus(factoryProcessSequence.getProducingDuration())
                    )
            );
            return result;
        }

        FactoryComputedDateTimePair computedDateTimePair = computeDateTimePair(
                factoryProcessSequence,
                statefulContainerAsMap
        );

        statefulContainerAsMap.put(factoryProcessSequence, computedDateTimePair);
        result.put(factoryProcessSequence, computedDateTimePair);

        return cascadeUpdatesAfter(factoryProcessSequence, result);
    }

    private FactoryComputedDateTimePair computeDateTimePair(
            FactoryProcessSequence factoryProcessSequence,
            TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair> statefulContainerAsMap
    ) {
        Map.Entry<FactoryProcessSequence, FactoryComputedDateTimePair> prevEntry
                = statefulContainerAsMap.lowerEntry(factoryProcessSequence);

        LocalDateTime producingDateTime = calcProducingDateTime(factoryProcessSequence, prevEntry);
        LocalDateTime completedDateTime = calcCompletedDateTime(factoryProcessSequence, producingDateTime);
        return new FactoryComputedDateTimePair(producingDateTime, completedDateTime);
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
                : previousEntry.getValue()
                  .completedDateTime();

        LocalDateTime arrangeDateTime = factoryProcessSequence.getArrangeDateTime();
        return (previousCompleted == null || arrangeDateTime.isAfter(previousCompleted))
                ? arrangeDateTime
                : previousCompleted;
    }

    private Map<FactoryProcessSequence, FactoryComputedDateTimePair> cascadeUpdatesAfter(
            FactoryProcessSequence fromSequence,
            Map<FactoryProcessSequence, FactoryComputedDateTimePair> resultContainerAsMap
    ) {
        return cascadeUpdatesAfter(
                fromSequence,
                resultContainerAsMap,
                this.shadowProcessSequenceToComputePairMap
        );
    }

    private Map<FactoryProcessSequence, FactoryComputedDateTimePair> cascadeUpdatesAfter(
            FactoryProcessSequence fromSequence,
            Map<FactoryProcessSequence, FactoryComputedDateTimePair> resultContainer,
            TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair> statefulContainerAsMap
    ) {
        NavigableMap<FactoryProcessSequence, FactoryComputedDateTimePair> tailMap =
                statefulContainerAsMap.tailMap(fromSequence, false);

        if (tailMap.isEmpty()) return resultContainer;

        Map.Entry<FactoryProcessSequence, FactoryComputedDateTimePair> startPrevEntry =
                statefulContainerAsMap.lowerEntry(tailMap.firstKey());

        LocalDateTime currentCompleted = (startPrevEntry != null)
                ? startPrevEntry.getValue()
                  .completedDateTime()
                : null;

        Set<FactoryProcessSequence> iteratingSet = tailMap.keySet();
        for (FactoryProcessSequence current : iteratingSet) {
            FactoryComputedDateTimePair existingPair = statefulContainerAsMap.get(current);
            LocalDateTime arrangeDateTime = current.getArrangeDateTime();

            LocalDateTime newProducing
                    = (currentCompleted == null || arrangeDateTime.isAfter(currentCompleted))
                    ? arrangeDateTime
                    : currentCompleted;
            LocalDateTime newCompleted = newProducing.plus(current.getProducingDuration());

            if (existingPair != null
                && existingPair.producingDateTime()
                        .equals(newProducing)
                && existingPair.completedDateTime()
                        .equals(newCompleted)
            ) {
                break;
            }

            statefulContainerAsMap.put(
                    current,
                    new FactoryComputedDateTimePair(newProducing, newCompleted)
            );
            resultContainer.put(
                    current,
                    new FactoryComputedDateTimePair(newProducing, newCompleted)
            );
            currentCompleted = newCompleted;
        }
        return resultContainer;
    }

    public boolean weatherFactoryProducingTypeIsQueue() {
        return this.getSchedulingFactoryInfo()
                .weatherFactoryProducingTypeIsQueue();
    }

    public Map<FactoryProcessSequence, FactoryComputedDateTimePair> addFactoryProcessSequence(
            SchedulingProducingArrangement schedulingProducingArrangement,
            TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair> statefulContainerAsMap
    ) {
        return addFactoryProcessSequence(
                new FactoryProcessSequence(schedulingProducingArrangement),
                statefulContainerAsMap
        );
    }

    private @NonNull FactoryComputedDateTimePair computeDateTimePair(FactoryProcessSequence factoryProcessSequence) {
        return this.computeDateTimePair(factoryProcessSequence, this.shadowProcessSequenceToComputePairMap);
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

    public Map<FactoryProcessSequence, FactoryComputedDateTimePair> removeFactoryProcessSequence(
            SchedulingProducingArrangement schedulingProducingArrangement
    ) {
        return removeFactoryProcessSequence(new FactoryProcessSequence(schedulingProducingArrangement));
    }

    public Map<FactoryProcessSequence, FactoryComputedDateTimePair> removeFactoryProcessSequence(
            FactoryProcessSequence factoryProcessSequence
    ) {
        return removeFactoryProcessSequence(
                factoryProcessSequence,
                this.shadowProcessSequenceToComputePairMap
        );
    }

    public Map<FactoryProcessSequence, FactoryComputedDateTimePair> removeFactoryProcessSequence(
            FactoryProcessSequence factoryProcessSequence,
            TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair> statefulContainerAsMap
    ) {
        Map<FactoryProcessSequence, FactoryComputedDateTimePair> resultContainer = new LinkedHashMap<>();

        if (!statefulContainerAsMap.containsKey(factoryProcessSequence)) {
            return resultContainer;
        }
        statefulContainerAsMap.remove(factoryProcessSequence);
        resultContainer.put(factoryProcessSequence, null);

        if (weatherFactoryProducingTypeIsQueue()) {
            return cascadeUpdatesAfter(factoryProcessSequence, resultContainer, statefulContainerAsMap);
        }

        return resultContainer;
    }

    public Map<FactoryProcessSequence, FactoryComputedDateTimePair> removeFactoryProcessSequence(
            SchedulingProducingArrangement schedulingProducingArrangement,
            TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair> statefulContainerAsMap
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

    public SortedMap<FactoryProcessSequence, FactoryComputedDateTimePair> prepareProducingAndCompletedMap() {
        return Collections.unmodifiableNavigableMap(this.shadowProcessSequenceToComputePairMap);
    }

    public FactoryComputedDateTimePair query(FactoryProcessSequence factoryProcessSequence) {
        return this.shadowProcessSequenceToComputePairMap.get(factoryProcessSequence);
    }

    @Override
    public String toString() {
        return "SchedulingFactoryInstance{" + "readableIdentifier='" + factoryReadableIdentifier + '\'' + ", producingLength=" + producingLength + ", " +
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

        public static final Comparator<FactoryProcessSequence> COMPARATOR
                = Comparator.comparing(FactoryProcessSequence::getSchedulingFactoryInstanceReadableIdentifier)
                .thenComparing(FactoryProcessSequence::getArrangeDateTime)
                .thenComparingInt(FactoryProcessSequence::getArrangementId);

        @Serial
        private static final long serialVersionUID = -264984659974196003L;

        @EqualsAndHashCode.Include
        LocalDateTime arrangeDateTime;

        @EqualsAndHashCode.Include
        Integer arrangementId;

        @EqualsAndHashCode.Include
        FactoryReadableIdentifier schedulingFactoryInstanceReadableIdentifier;

        Duration producingDuration;

        public FactoryProcessSequence(SchedulingProducingArrangement schedulingProducingArrangement) {
            this.arrangeDateTime = schedulingProducingArrangement.getArrangeDateTime();
            this.producingDuration = schedulingProducingArrangement.getProducingDuration();
            this.arrangementId = schedulingProducingArrangement.getId();
            this.schedulingFactoryInstanceReadableIdentifier
                    = schedulingProducingArrangement.getPlanningFactoryInstance()
                    .getFactoryReadableIdentifier();
        }

        @Override
        public int compareTo(FactoryProcessSequence that) {
            return COMPARATOR.compare(this, that);
        }

    }

    public static record FactoryComputedDateTimePair(
            LocalDateTime producingDateTime,
            LocalDateTime completedDateTime
    ) implements Serializable {

    }

}
