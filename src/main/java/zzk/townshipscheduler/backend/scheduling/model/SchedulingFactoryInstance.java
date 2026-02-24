package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.solution.cloner.DeepPlanningClone;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import org.jspecify.annotations.NonNull;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@PlanningEntity
public class SchedulingFactoryInstance implements Serializable {

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

    public void setupFactoryReadableIdentifier() {
        setFactoryReadableIdentifier(new FactoryReadableIdentifier(getCategoryName(), getSeqNum()));
    }

    public String getCategoryName() {
        return schedulingFactoryInfo.getCategoryName();
    }

    public long calcRemainProducingQueueSize(LocalDateTime argDateTime) {
        long count = 0L;
        for (SchedulingProducingArrangement planningFactoryInstanceProducingArrangement : this.planningFactoryInstanceProducingArrangements) {
            LocalDateTime arrangementArrangeDateTime = planningFactoryInstanceProducingArrangement.getArrangeDateTime();
            LocalDateTime arrangementCompletedDateTime = planningFactoryInstanceProducingArrangement.getCompletedDateTime();
            boolean b1 = !arrangementArrangeDateTime.isAfter(argDateTime);
            boolean b2 = arrangementCompletedDateTime.isAfter(argDateTime);
            if (b1 & b2) {
                count++;
            }
        }
        return count;
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

        FactoryComputedDateTimePair computedDateTimePair = computeDateTimePair(factoryProcessSequence);

        statefulContainerAsMap.put(factoryProcessSequence, computedDateTimePair);
        result.put(factoryProcessSequence, computedDateTimePair);

        return cascadeUpdatesAfter(factoryProcessSequence, result);
    }

    private @NonNull FactoryComputedDateTimePair computeDateTimePair(FactoryProcessSequence factoryProcessSequence) {
        Map.Entry<FactoryProcessSequence, FactoryComputedDateTimePair> prevEntry
                = this.shadowProcessSequenceToComputePairMap.lowerEntry(factoryProcessSequence);

        LocalDateTime producingDateTime = calcProducingDateTime(factoryProcessSequence, prevEntry);
        LocalDateTime completedDateTime = calcCompletedDateTime(factoryProcessSequence, producingDateTime);
        return new FactoryComputedDateTimePair(producingDateTime, completedDateTime);
    }

    private LocalDateTime calcCompletedDateTime(FactoryProcessSequence factoryProcessSequence, LocalDateTime producingDateTime) {
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
        return (previousCompleted == null || arrangeDateTime.isAfter(previousCompleted)) ? arrangeDateTime : previousCompleted;
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

    public boolean weatherFactoryProducingTypeIsQueue() {
        return this.getSchedulingFactoryInfo()
                .weatherFactoryProducingTypeIsQueue();
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
                    && existingPair.producingDateTime().equals(newProducing)
                    && existingPair.completedDateTime().equals(newCompleted)
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

    private LocalDateTime calcProducingDateTime(FactoryProcessSequence factoryProcessSequence, LocalDateTime previousCompletedDateTime) {
        LocalDateTime arrangeDateTime = factoryProcessSequence.getArrangeDateTime();
        return (previousCompletedDateTime == null || arrangeDateTime.isAfter(previousCompletedDateTime)) ? arrangeDateTime : previousCompletedDateTime;
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
            return cascadeUpdatesAfter(factoryProcessSequence, resultContainer);
        }

        return resultContainer;
    }

    private LocalDateTime calcProducingDateTime(FactoryProcessSequence factoryProcessSequence, FactoryComputedDateTimePair previousComputedPair) {
        LocalDateTime previousCompleted = (previousComputedPair == null) ? null : previousComputedPair.completedDateTime();

        LocalDateTime arrangeDateTime = factoryProcessSequence.getArrangeDateTime();
        return (previousCompleted == null || arrangeDateTime.isAfter(previousCompleted)) ? arrangeDateTime : previousCompleted;
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

}
