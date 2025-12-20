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

import java.time.LocalDateTime;
import java.util.*;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@PlanningEntity
public class SchedulingFactoryInstance {

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

    public void addFactoryProcessSequence(FactoryProcessSequence factoryProcessSequence) {

        if (!weatherFactoryProducingTypeIsQueue()) {
            this.shadowProcessSequenceToComputePairMap.put(
                    factoryProcessSequence,
                    new FactoryComputedDateTimePair(
                            factoryProcessSequence.getArrangeDateTime(),
                            factoryProcessSequence.getArrangeDateTime()
                                    .plus(factoryProcessSequence.getProducingDuration())
                    )
            );
            return;
        }

        FactoryComputedDateTimePair computedDateTimePair = computeDateTimePair(factoryProcessSequence);

        this.shadowProcessSequenceToComputePairMap.put(factoryProcessSequence, computedDateTimePair);

//        NavigableMap<FactoryProcessSequence, FactoryComputedDateTimePair> tailOfProcessSequencePairMap =
//                this.shadowProcessSequenceToComputePairMap.tailMap(
//                        factoryProcessSequence,
//                        false
//                );
//        cascade(tailOfProcessSequencePairMap);
        cascadeUpdatesAfter(factoryProcessSequence);
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

    private void cascadeUpdatesAfter(FactoryProcessSequence fromSequence) {
        NavigableMap<FactoryProcessSequence, FactoryComputedDateTimePair> tailMap =
                shadowProcessSequenceToComputePairMap.tailMap(fromSequence, false);

        if (tailMap.isEmpty()) return;

        Map.Entry<FactoryProcessSequence, FactoryComputedDateTimePair> startPrevEntry =
                shadowProcessSequenceToComputePairMap.lowerEntry(tailMap.firstKey());

        LocalDateTime currentCompleted = (startPrevEntry != null)
                ? startPrevEntry.getValue()
                .completedDateTime()
                : null;

        Set<FactoryProcessSequence> iteratingSet = tailMap.keySet();
        for (FactoryProcessSequence current : iteratingSet) {
            FactoryComputedDateTimePair existingPair = shadowProcessSequenceToComputePairMap.get(current);
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

            shadowProcessSequenceToComputePairMap.put(
                    current,
                    new FactoryComputedDateTimePair(newProducing, newCompleted)
            );
            currentCompleted = newCompleted;
        }
    }

    private void cascade(NavigableMap<FactoryProcessSequence, FactoryComputedDateTimePair> tailOfProcessSequencePairMap) {
        if (tailOfProcessSequencePairMap.isEmpty()) {
            return;
        }

        List<FactoryProcessSequence> taiKeyList = new ArrayList<>(tailOfProcessSequencePairMap.keySet());

        Map.Entry<FactoryProcessSequence, FactoryComputedDateTimePair> prefixEntry = this.shadowProcessSequenceToComputePairMap.lowerEntry(taiKeyList.get(0));

        LocalDateTime prefixMaxCompletedDateTime = (prefixEntry == null)
                ? null
                : prefixEntry.getValue()
                        .completedDateTime();

        for (FactoryProcessSequence factoryProcessSequence : taiKeyList) {
            LocalDateTime producingDateTime = calcProducingDateTime(factoryProcessSequence, prefixMaxCompletedDateTime);
            LocalDateTime completedDateTime = calcCompletedDateTime(factoryProcessSequence, producingDateTime);
            this.shadowProcessSequenceToComputePairMap.put(factoryProcessSequence, new FactoryComputedDateTimePair(producingDateTime, completedDateTime));
            prefixMaxCompletedDateTime = completedDateTime;
        }
    }

    private LocalDateTime calcProducingDateTime(FactoryProcessSequence factoryProcessSequence, LocalDateTime previousCompletedDateTime) {
        LocalDateTime arrangeDateTime = factoryProcessSequence.getArrangeDateTime();
        return (previousCompletedDateTime == null || arrangeDateTime.isAfter(previousCompletedDateTime)) ? arrangeDateTime : previousCompletedDateTime;
    }

    public boolean weatherFactoryProducingTypeIsQueue() {
        return this.getSchedulingFactoryInfo()
                .weatherFactoryProducingTypeIsQueue();
    }

    public void removeFactoryProcessSequence(FactoryProcessSequence factoryProcessSequence) {

        if (!shadowProcessSequenceToComputePairMap.containsKey(factoryProcessSequence)) {
            return;
        }
        shadowProcessSequenceToComputePairMap.remove(factoryProcessSequence);

        if (weatherFactoryProducingTypeIsQueue()) {
            cascadeUpdatesAfter(factoryProcessSequence);
        }

//        if (this.shadowProcessSequenceToComputePairMap.remove(factoryProcessSequence) == null) {
//            return;
//        }
//
//        if (!weatherFactoryProducingTypeIsQueue()) {
//            return;
//        }
//
//        NavigableMap<FactoryProcessSequence, FactoryComputedDateTimePair> tail
//                = this.shadowProcessSequenceToComputePairMap.tailMap(
//                factoryProcessSequence,
//                false
//        );
//        cascade(tail);
    }

    private LocalDateTime calcProducingDateTime(FactoryProcessSequence factoryProcessSequence, FactoryComputedDateTimePair previousComputedPair) {
        LocalDateTime previousCompleted = (previousComputedPair == null) ? null : previousComputedPair.completedDateTime();

        LocalDateTime arrangeDateTime = factoryProcessSequence.getArrangeDateTime();
        return (previousCompleted == null || arrangeDateTime.isAfter(previousCompleted)) ? arrangeDateTime : previousCompleted;
    }

    public SortedMap<FactoryProcessSequence, FactoryComputedDateTimePair> prepareProducingAndCompletedMap() {
        return Collections.unmodifiableNavigableMap(this.shadowProcessSequenceToComputePairMap);
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
