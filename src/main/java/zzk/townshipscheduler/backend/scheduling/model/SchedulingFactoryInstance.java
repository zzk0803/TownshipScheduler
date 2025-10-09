package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.solution.cloner.DeepPlanningClone;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.time.LocalDateTime;
import java.util.*;

@Log4j2
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@PlanningEntity
public class SchedulingFactoryInstance {

    @PlanningId
    @EqualsAndHashCode.Include
    private Integer id;

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
    @ShadowVariable(supplierName = "factoryProcessToDateTimePairMapSupplier")
    private TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair> factoryProcessToDateTimePairMap
            = new TreeMap<>();

    @ShadowSources(value = {
            "planningFactoryInstanceProducingArrangements",
            "planningFactoryInstanceProducingArrangements[].planningDateTimeSlot"
    })
    public TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair> factoryProcessToDateTimePairMapSupplier() {
        TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair> computedMap = new TreeMap<>();
        for (SchedulingProducingArrangement arrangement : this.planningFactoryInstanceProducingArrangements) {
            FactoryProcessSequence factoryProcessSequence = new FactoryProcessSequence(arrangement);
            computedMap = addFactoryProcessSequence(computedMap, factoryProcessSequence);
        }
        return computedMap;
    }

    public FactoryComputedDateTimePair queryComputedDateTimePair(SchedulingProducingArrangement schedulingProducingArrangement) {
        return factoryProcessToDateTimePairMap.get(new FactoryProcessSequence(schedulingProducingArrangement));
    }

    public void setupFactoryReadableIdentifier() {
        setFactoryReadableIdentifier(new FactoryReadableIdentifier(getCategoryName(), getSeqNum()));
    }

    public String getCategoryName() {
        return schedulingFactoryInfo.getCategoryName();
    }

    public int getMaxPossibleOccupancyDurationMinutes() {
        return producingLength * this.getSchedulingFactoryInfo().calcMaxSupportedProductDurationMinutes();
    }

    public TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair> addFactoryProcessSequence(
            TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair> factoryProcessToDateTimePairMap,
            FactoryProcessSequence factoryProcessSequence
    ) {

        if (!weatherFactoryProducingTypeIsQueue()) {
            factoryProcessToDateTimePairMap.put(
                    factoryProcessSequence,
                    new FactoryComputedDateTimePair(
                            factoryProcessSequence.getArrangeDateTime(),
                            factoryProcessSequence.getArrangeDateTime()
                                    .plus(factoryProcessSequence.getProducingDuration())
                    )
            );
            return factoryProcessToDateTimePairMap;
        }

        Map.Entry<FactoryProcessSequence, FactoryComputedDateTimePair> prefixProducingPairEntry =
                factoryProcessToDateTimePairMap.lowerEntry(factoryProcessSequence);

        LocalDateTime producingDateTime = calcProducingDateTime(
                factoryProcessSequence,
                prefixProducingPairEntry
        );
        LocalDateTime completedDateTime = calcCompletedDateTime(factoryProcessSequence, producingDateTime);
        factoryProcessToDateTimePairMap.put(
                factoryProcessSequence,
                new FactoryComputedDateTimePair(producingDateTime, completedDateTime)
        );

        NavigableMap<FactoryProcessSequence, FactoryComputedDateTimePair> tailOfProcessSequencePairMap
                = factoryProcessToDateTimePairMap.tailMap(
                factoryProcessSequence,
                false
        );
        cascade(factoryProcessToDateTimePairMap, tailOfProcessSequencePairMap);
        return factoryProcessToDateTimePairMap;
    }

    public boolean weatherFactoryProducingTypeIsQueue() {
        return this.getSchedulingFactoryInfo().weatherFactoryProducingTypeIsQueue();
    }

    private void cascade(
            TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair> factoryProcessToDateTimePairMap,
            NavigableMap<FactoryProcessSequence, FactoryComputedDateTimePair> tailOfProcessSequencePairMap
    ) {
        if (tailOfProcessSequencePairMap.isEmpty()) {
            return;
        }

        List<FactoryProcessSequence> taiKeyList = new ArrayList<>(tailOfProcessSequencePairMap.keySet());

        Map.Entry<FactoryProcessSequence, FactoryComputedDateTimePair> prefixEntry
                = factoryProcessToDateTimePairMap.lowerEntry(taiKeyList.get(0));

        LocalDateTime prefixMaxCompletedDateTime
                = (prefixEntry == null)
                ? null
                : prefixEntry.getValue().completedDateTime();

        for (FactoryProcessSequence factoryProcessSequence : taiKeyList) {
            LocalDateTime producingDateTime = calcProducingDateTime(
                    factoryProcessSequence,
                    prefixMaxCompletedDateTime
            );
            LocalDateTime completedDateTime = calcCompletedDateTime(
                    factoryProcessSequence,
                    producingDateTime
            );
            factoryProcessToDateTimePairMap.put(
                    factoryProcessSequence,
                    new FactoryComputedDateTimePair(producingDateTime, completedDateTime)
            );
            prefixMaxCompletedDateTime = completedDateTime;
        }
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

    public TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair> removeFactoryProcessSequence(
            TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair> factoryProcessToDateTimePairMap,
            FactoryProcessSequence factoryProcessSequence
    ) {

        if (this.factoryProcessToDateTimePairMap.remove(factoryProcessSequence) == null) {
            return factoryProcessToDateTimePairMap;
        }

        if (!weatherFactoryProducingTypeIsQueue()) {
            return factoryProcessToDateTimePairMap;
        }

        NavigableMap<FactoryProcessSequence, FactoryComputedDateTimePair> tail =
                factoryProcessToDateTimePairMap.tailMap(factoryProcessSequence, false);
        cascade(factoryProcessToDateTimePairMap, tail);
        return factoryProcessToDateTimePairMap;
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

    @Override
    public String toString() {
        return "SchedulingFactoryInstance{" +
               "readableIdentifier='" + factoryReadableIdentifier + '\'' +
               ", producingLength=" + producingLength +
               ", reapWindowSize=" + reapWindowSize +
               '}';
    }

    public boolean typeEqual(SchedulingFactoryInstance that) {
        return this.getSchedulingFactoryInfo().typeEqual(that.getSchedulingFactoryInfo());
    }

}
