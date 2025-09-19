package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.solution.cloner.DeepPlanningClone;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import zzk.townshipscheduler.backend.ProducingStructureType;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

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
    private Set<FactoryProcessSequence> shadowFactorySequenceSet
            = new LinkedHashSet<>();

    @ShadowVariable(supplierName = "factoryProcessToDateTimePairMapSupplier")
    @DeepPlanningClone
    private TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair> factoryProcessToDateTimePairMap
            = new TreeMap<>();

    @ShadowSources(
            value = {
                    "planningFactoryInstanceProducingArrangements",
                    "planningFactoryInstanceProducingArrangements[].shadowFactoryProcessSequence"
            }
    )
    public TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair> factoryProcessToDateTimePairMapSupplier() {
        TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair> completedMap
                = (TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair>) prepareProducingAndCompletedMap(
                this.planningFactoryInstanceProducingArrangements.stream()
                        .map(SchedulingProducingArrangement::getShadowFactoryProcessSequence)
                        .collect(Collectors.toCollection(TreeSet::new))
        );
        log.info("completedMap={}",completedMap);
        return completedMap;
    }

    public void setupFactoryReadableIdentifier() {
        setFactoryReadableIdentifier(new FactoryReadableIdentifier(getCategoryName(), getSeqNum()));
    }

    public String getCategoryName() {
        return schedulingFactoryInfo.getCategoryName();
    }

    public boolean addFactoryProcessSequence(FactoryProcessSequence factoryProcessSequence) {
        return shadowFactorySequenceSet.add(factoryProcessSequence);
    }

    public boolean removeFactoryProcessSequence(Object o) {
        return shadowFactorySequenceSet.remove(o);
    }

    public boolean weatherFactoryProducingTypeIsQueue() {
        return this.getSchedulingFactoryInfo().weatherFactoryProducingTypeIsQueue();
    }

    public FactoryComputedDateTimePair queryProducingAndCompletedPair(SchedulingProducingArrangement schedulingProducingArrangement) {
        FactoryProcessSequence factoryProcessSequence = schedulingProducingArrangement.getShadowFactoryProcessSequence();
        SortedMap<FactoryProcessSequence, FactoryComputedDateTimePair> processSequenceDateTimePairMap
                = prepareProducingAndCompletedMap();
        return processSequenceDateTimePairMap.get(factoryProcessSequence);
    }

    public SortedMap<FactoryProcessSequence, FactoryComputedDateTimePair> prepareProducingAndCompletedMap() {
        return this.useComputeStrategy().prepareProducingAndCompletedMap(new TreeSet<>(getShadowFactorySequenceSet()));
    }

    public int getMaxPossibleOccupancyDurationMinutes() {
        return producingLength * this.getSchedulingFactoryInfo().calcMaxSupportedProductDurationMinutes();
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

        Map.Entry<FactoryProcessSequence, FactoryComputedDateTimePair> prefixProducingPairEntry =
                this.shadowProcessSequenceToComputePairMap.lowerEntry(factoryProcessSequence);

        LocalDateTime producingDateTime = calcProducingDateTime(
                factoryProcessSequence,
                prefixProducingPairEntry
        );
        LocalDateTime completedDateTime = calcCompletedDateTime(factoryProcessSequence, producingDateTime);
        this.shadowProcessSequenceToComputePairMap.put(
                factoryProcessSequence,
                new FactoryComputedDateTimePair(producingDateTime, completedDateTime)
        );

        NavigableMap<FactoryProcessSequence, FactoryComputedDateTimePair> tailOfProcessSequencePairMap
                = this.shadowProcessSequenceToComputePairMap.tailMap(
                factoryProcessSequence,
                false
        );
        cascade(tailOfProcessSequencePairMap);
    }

    private void cascade(NavigableMap<FactoryProcessSequence, FactoryComputedDateTimePair> tailOfProcessSequencePairMap) {
        if (tailOfProcessSequencePairMap.isEmpty()) {
            return;
        }

        List<FactoryProcessSequence> taiKeyList = new ArrayList<>(tailOfProcessSequencePairMap.keySet());

        Map.Entry<FactoryProcessSequence, FactoryComputedDateTimePair> prefixEntry
                = this.shadowProcessSequenceToComputePairMap.lowerEntry(taiKeyList.get(0));

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
            this.shadowProcessSequenceToComputePairMap.put(
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

    public void removeFactoryProcessSequence(FactoryProcessSequence factoryProcessSequence) {

        if (this.shadowProcessSequenceToComputePairMap.remove(factoryProcessSequence) == null) {
            return;
        }

        if (!weatherFactoryProducingTypeIsQueue()) {
            return;
        }

        NavigableMap<FactoryProcessSequence, FactoryComputedDateTimePair> tail =
                this.shadowProcessSequenceToComputePairMap.tailMap(factoryProcessSequence, false);
        cascade(tail);
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
