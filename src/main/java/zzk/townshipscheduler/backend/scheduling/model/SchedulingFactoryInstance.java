package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.solution.cloner.DeepPlanningClone;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import zzk.townshipscheduler.backend.ProducingStructureType;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

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
                    = new ConcurrentSkipListMap<>(
                    Comparator.comparing(FactoryProcessSequence::getArrangeDateTime)
                            .thenComparingInt(FactoryProcessSequence::getArrangementId)
            );

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
