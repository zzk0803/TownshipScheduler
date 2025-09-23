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
import zzk.townshipscheduler.backend.ProducingStructureType;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
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

    public String getCategoryName() {
        return schedulingFactoryInfo.getCategoryName();
    }

    public SortedMap<FactoryProcessSequence, FactoryComputedDateTimePair> prepareProducingAndCompletedMap(TreeSet<FactoryProcessSequence> factoryProcessSequenceTreeSet) {
        return this.useComputeStrategy().prepareProducingAndCompletedMap(factoryProcessSequenceTreeSet);
    }

    public ProducingAndCompletedDateTimeComputeStrategy useComputeStrategy() {
        if (getSchedulingFactoryInfo() == null) {
            throw new IllegalStateException();
        }

        ProducingStructureType producingStructureType = getSchedulingFactoryInfo().getProducingStructureType();
        if (producingStructureType == ProducingStructureType.SLOT) {
            return new TypeSlotProducingAndCompletedDateTimeComputeStrategy();
        } else if (producingStructureType == ProducingStructureType.QUEUE) {
            return new TypeQueueProducingAndCompletedDateTimeComputeStrategy();
        } else {
            throw new IllegalStateException();
        }
    }

    public void setupFactoryReadableIdentifier() {
        setFactoryReadableIdentifier(new FactoryReadableIdentifier(getCategoryName(), getSeqNum()));
    }


    public boolean weatherFactoryProducingTypeIsQueue() {
        return this.getSchedulingFactoryInfo().weatherFactoryProducingTypeIsQueue();
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

    public void addFactoryProcessSequence(FactoryProcessSequence factoryProcessSequence) {

        if (!weatherFactoryProducingTypeIsQueue()) {
            this.factoryProcessToDateTimePairMap.put(
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
                this.factoryProcessToDateTimePairMap.lowerEntry(factoryProcessSequence);

        LocalDateTime producingDateTime = calcProducingDateTime(
                factoryProcessSequence,
                prefixProducingPairEntry
        );
        LocalDateTime completedDateTime = calcCompletedDateTime(factoryProcessSequence, producingDateTime);
        this.factoryProcessToDateTimePairMap.put(
                factoryProcessSequence,
                new FactoryComputedDateTimePair(producingDateTime, completedDateTime)
        );

        NavigableMap<FactoryProcessSequence, FactoryComputedDateTimePair> tailOfProcessSequencePairMap
                = this.factoryProcessToDateTimePairMap.tailMap(
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
                = this.factoryProcessToDateTimePairMap.lowerEntry(taiKeyList.get(0));

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
            this.factoryProcessToDateTimePairMap.put(
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

    private interface ProducingAndCompletedDateTimeComputeStrategy {

        SortedMap<FactoryProcessSequence, FactoryComputedDateTimePair> prepareProducingAndCompletedMap(
                SortedSet<FactoryProcessSequence> shadowFactorySequenceSet
        );

    }

    private final class TypeQueueProducingAndCompletedDateTimeComputeStrategy
            implements ProducingAndCompletedDateTimeComputeStrategy {

        @Override
        public SortedMap<FactoryProcessSequence, FactoryComputedDateTimePair> prepareProducingAndCompletedMap(
                SortedSet<FactoryProcessSequence> shadowFactorySequenceSet
        ) {

            TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair> computingProducingCompletedMap
                    = new TreeMap<>();

            for (FactoryProcessSequence current : shadowFactorySequenceSet) {
                Duration producingDuration = current.getProducingDuration();
                LocalDateTime arrangeDateTime = current.getArrangeDateTime();

                LocalDateTime previousCompletedDateTime
                        = Optional.ofNullable(computingProducingCompletedMap.lowerKey(current))
                        .map(computingProducingCompletedMap::get)
                        .map(FactoryComputedDateTimePair::completedDateTime)
                        .orElse(null);

                LocalDateTime producingDateTime;
                if (previousCompletedDateTime == null) {
                    producingDateTime = arrangeDateTime;
                } else {
                    producingDateTime = arrangeDateTime.isAfter(previousCompletedDateTime)
                            ? arrangeDateTime
                            : previousCompletedDateTime;
                }
                LocalDateTime completedDateTime = producingDateTime.plus(producingDuration);

                computingProducingCompletedMap.put(
                        current,
                        new FactoryComputedDateTimePair(producingDateTime, completedDateTime)
                );

            }

            return computingProducingCompletedMap;

        }

    }

    private final class TypeSlotProducingAndCompletedDateTimeComputeStrategy
            implements ProducingAndCompletedDateTimeComputeStrategy {

        @Override
        public SortedMap<FactoryProcessSequence, FactoryComputedDateTimePair> prepareProducingAndCompletedMap(
                SortedSet<FactoryProcessSequence> shadowFactorySequenceSet
        ) {

            SortedMap<FactoryProcessSequence, FactoryComputedDateTimePair> computingProducingCompletedMap
                    = new TreeMap<>();

            for (FactoryProcessSequence current : shadowFactorySequenceSet) {
                Duration producingDuration = current.getProducingDuration();
                LocalDateTime arrangeDateTime = current.getArrangeDateTime();
                LocalDateTime completedDateTime = arrangeDateTime.plus(producingDuration);

                computingProducingCompletedMap.put(
                        current,
                        new FactoryComputedDateTimePair(arrangeDateTime, completedDateTime)
                );

            }

            return computingProducingCompletedMap;


        }

    }

}
