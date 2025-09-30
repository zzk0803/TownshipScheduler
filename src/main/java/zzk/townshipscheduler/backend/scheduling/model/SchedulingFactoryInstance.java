package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import zzk.townshipscheduler.backend.ProducingStructureType;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

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

    private int parallelProducing = 1;

    @ToString.Include
    private int producingQueue;

    @ToString.Include
    private int reapWindowSize;

    @Setter(AccessLevel.PRIVATE)
    @ToString.Include
    private FactoryReadableIdentifier factoryReadableIdentifier;

    private List<SchedulingFactoryInstanceDateTimeSlot> schedulingFactoryInstanceDateTimeSlotList = new ArrayList<>();

    @ShadowVariable(supplierName = "slotToLastCompletedMapSupplier")
    private TreeMap<SchedulingFactoryInstanceDateTimeSlot, LocalDateTime> slotToLastCompletedMap = new TreeMap<>();

    @ShadowSources({"schedulingFactoryInstanceDateTimeSlotList[].tailArrangementCompletedDateTime"})
    private TreeMap<SchedulingFactoryInstanceDateTimeSlot, LocalDateTime> slotToLastCompletedMapSupplier() {
        TreeMap<SchedulingFactoryInstanceDateTimeSlot, LocalDateTime> result = schedulingFactoryInstanceDateTimeSlotList.stream()
                .collect(
                        TreeMap::new,
                        (treeMap, factoryInstanceDateTimeSlot) -> {
                            treeMap.compute(
                                    factoryInstanceDateTimeSlot,
                                    (slot, localDateTime) -> {
                                        LocalDateTime ldt = slot.getTailArrangementCompletedDateTime();
                                        return ldt != null ? ldt : slot.getStart();
                                    }
                            );
                        }, TreeMap::putAll
                );
        return result;
    }

    public LocalDateTime queryFormerCompletedDateTimeOrArgSlotDateTime(SchedulingFactoryInstanceDateTimeSlot factoryInstanceDateTimeSlot) {
        NavigableMap<SchedulingFactoryInstanceDateTimeSlot, LocalDateTime> headedMap
                = slotToLastCompletedMap.headMap(
                factoryInstanceDateTimeSlot,
                false
        );
        return headedMap.values().stream()
                .filter(localDateTime -> localDateTime.isAfter(factoryInstanceDateTimeSlot.getStart()))
                .max(Comparator.naturalOrder())
                .orElse(factoryInstanceDateTimeSlot.getStart());
    }

    public void setupFactoryReadableIdentifier() {
        setFactoryReadableIdentifier(
                new FactoryReadableIdentifier(getCategoryName(), getSeqNum())
        );
    }

    public String getCategoryName() {
        return schedulingFactoryInfo.getCategoryName();
    }

    public boolean weatherFactoryProducingTypeIsQueue() {
        return this.getSchedulingFactoryInfo().weatherFactoryProducingTypeIsQueue();
    }

    public Stream<SchedulingFactoryInstanceDateTimeSlot> schedulingFactoryInstanceDateTimeSlotStream() {
        return this.schedulingFactoryInstanceDateTimeSlotList.stream();
    }

    public SortedMap<FactoryProcessSequence, FactoryComputedDateTimePair> prepareProducingAndCompletedMap() {
        return this.useComputeStrategy().prepareProducingAndCompletedMap(new TreeSet<>());
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
               ", producingLength=" + producingQueue +
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
                        .map(FactoryComputedDateTimePair::getCompletedDateTime)
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
