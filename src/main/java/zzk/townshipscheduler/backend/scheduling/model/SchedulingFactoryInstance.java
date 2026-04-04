package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.cloner.DeepPlanningClone;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
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
public class SchedulingFactoryInstance
        implements Serializable {

    public static final String PLANNING_FACTORY_INSTANCE_PRODUCING_ARRANGEMENTS
            = "planningFactoryInstanceProducingArrangements";

    public static final String VALUE_RANGE_FOR_SCHEDULING_PRODUCING_ARRANGEMENT
            = "valueRangeForSchedulingProducingArrangement";

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
    @PlanningListVariable(valueRangeProviderRefs = VALUE_RANGE_FOR_SCHEDULING_PRODUCING_ARRANGEMENT)
    private List<SchedulingProducingArrangement> planningFactoryInstanceProducingArrangements = new ArrayList<>();

    @DeepPlanningClone
    private TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair> shadowProcessSequenceToComputePairMap = new TreeMap<>();

    @ValueRangeProvider(id = VALUE_RANGE_FOR_SCHEDULING_PRODUCING_ARRANGEMENT)
    public List<SchedulingProducingArrangement> valueRangeForSchedulingProducingArrangement(TownshipSchedulingProblem townshipSchedulingProblem) {
        return townshipSchedulingProblem.valueRangeForSchedulingProducingArrangement(this);
    }

    public void setupFactoryReadableIdentifier() {
        setFactoryReadableIdentifier(new FactoryReadableIdentifier(getCategoryName(), getSeqNum()));
    }

    public String getCategoryName() {
        return schedulingFactoryInfo.getCategoryName();
    }

    public Map<FactoryProcessSequence, FactoryComputedDateTimePair> changeFactoryProcessSequence(
            FactoryProcessSequence factoryProcessSequence,
            TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair> statefulContainerAsMap
    ) {
        statefulContainerAsMap.keySet()
                .stream()
                .filter(iteratingKey -> iteratingKey.equals(factoryProcessSequence) && iteratingKey.compareTo(factoryProcessSequence) != 0)
                .forEach(foundProcessSequence -> {
                    this.removeFactoryProcessSequence(foundProcessSequence, statefulContainerAsMap);
                })
        ;
        return this.addFactoryProcessSequence(factoryProcessSequence, statefulContainerAsMap);
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

        FactoryComputedDateTimePair computedDateTimePair = computeDateTimePair(factoryProcessSequence, statefulContainerAsMap);

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

    private @NonNull FactoryComputedDateTimePair computeDateTimePair(FactoryProcessSequence factoryProcessSequence) {
        return this.computeDateTimePair(factoryProcessSequence, this.shadowProcessSequenceToComputePairMap);
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
            return cascadeUpdatesAfter(factoryProcessSequence, resultContainer, statefulContainerAsMap);
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
