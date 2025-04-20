package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.solution.cloner.DeepPlanningClone;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.javatuples.Pair;
import zzk.townshipscheduler.backend.ProducingStructureType;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@PlanningEntity
public class SchedulingFactoryInstance {

    @PlanningId
    @EqualsAndHashCode.Include
    private Integer id;

    @JsonIgnore
    private SchedulingFactoryInfo schedulingFactoryInfo;

    private int seqNum;

    private int producingLength;

    private int reapWindowSize;

    private String readableIdentifier;

    @JsonIgnore
    @InverseRelationShadowVariable(sourceVariableName = SchedulingProducingArrangement.PLANNING_FACTORY_INSTANCE)
    private List<SchedulingProducingArrangement> planningFactoryInstanceProducingArrangements = new ArrayList<>();

    @JsonIgnore
    @DeepPlanningClone
    private SortedSet<FactoryProcessSequence> shadowFactorySequenceSet
            = new ConcurrentSkipListSet<>(
            Comparator.comparing(FactoryProcessSequence::getArrangeDateTime)
                    .thenComparingInt(FactoryProcessSequence::getArrangementId)
    );

    public boolean addFactoryProcessSequence(FactoryProcessSequence factoryProcessSequence) {
        return shadowFactorySequenceSet.add(factoryProcessSequence);
    }

    public boolean removeFactoryProcessSequence(Object o) {
        return shadowFactorySequenceSet.remove(o);
    }

    public Pair<LocalDateTime, LocalDateTime> queryProducingAndCompletedPair(SchedulingProducingArrangement schedulingProducingArrangement) {
        FactoryProcessSequence factoryProcessSequence = schedulingProducingArrangement.getShadowFactoryProcessSequence();
        SortedMap<FactoryProcessSequence, Pair<LocalDateTime, LocalDateTime>> processSequenceDateTimePairMap
                = prepareProducingAndCompletedMap();
        return processSequenceDateTimePairMap.get(factoryProcessSequence);
    }

    public SortedMap<FactoryProcessSequence, Pair<LocalDateTime, LocalDateTime>> prepareProducingAndCompletedMap() {
        return this.useComputeStrategy().prepareProducingAndCompletedMap(this.shadowFactorySequenceSet);
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
               "readableIdentifier='" + readableIdentifier + '\'' +
               ", producingLength=" + producingLength +
               ", reapWindowSize=" + reapWindowSize +
               '}';
    }

    @JsonProperty
    public String getCategoryName() {
        return schedulingFactoryInfo.getCategoryName();
    }

    public boolean typeEqual(SchedulingFactoryInstance that) {
        return this.getSchedulingFactoryInfo().typeEqual(that.getSchedulingFactoryInfo());
    }

    private interface ProducingAndCompletedDateTimeComputeStrategy {

        SortedMap<FactoryProcessSequence, Pair<LocalDateTime, LocalDateTime>> prepareProducingAndCompletedMap(
                SortedSet<FactoryProcessSequence> shadowFactorySequenceSet
        );

    }

    private final class TypeQueueProducingAndCompletedDateTimeComputeStrategy
            implements ProducingAndCompletedDateTimeComputeStrategy {

        @Override
        public SortedMap<FactoryProcessSequence, Pair<LocalDateTime, LocalDateTime>> prepareProducingAndCompletedMap(
                SortedSet<FactoryProcessSequence> shadowFactorySequenceSet
        ) {

            SortedMap<FactoryProcessSequence, Pair<LocalDateTime, LocalDateTime>> computingProducingCompletedMap
                    = new ConcurrentSkipListMap<>(
                    Comparator.comparing(FactoryProcessSequence::getArrangeDateTime)
                            .thenComparingInt(FactoryProcessSequence::getArrangementId)
            );

            for (FactoryProcessSequence current : shadowFactorySequenceSet) {
                Duration producingDuration = current.getProducingDuration();
                LocalDateTime arrangeDateTime = current.getArrangeDateTime();
                boolean sequenced = current.isSequenced();

                LocalDateTime previousAlmostCompletedDateTime
                        = computingProducingCompletedMap.headMap(current)
                        .values().stream()
                        .map(Pair::getValue1)
                        .max(LocalDateTime::compareTo)
                        .orElse(null);

                LocalDateTime producingDateTime;
                if (previousAlmostCompletedDateTime == null) {
                    producingDateTime = arrangeDateTime;
                } else {
                    if (arrangeDateTime.isAfter(previousAlmostCompletedDateTime)) {
                        producingDateTime = arrangeDateTime;
                    } else if (arrangeDateTime.isBefore(previousAlmostCompletedDateTime)) {
                        producingDateTime = previousAlmostCompletedDateTime;
                    } else {
                        producingDateTime = arrangeDateTime;
                    }
                }
                LocalDateTime completedDateTime = producingDateTime.plus(producingDuration);

                computingProducingCompletedMap.put(
                        current,
                        new Pair<>(producingDateTime, completedDateTime)
                );

            }

            return computingProducingCompletedMap;

        }

    }

    private final class TypeSlotProducingAndCompletedDateTimeComputeStrategy
            implements ProducingAndCompletedDateTimeComputeStrategy {

        @Override
        public SortedMap<FactoryProcessSequence, Pair<LocalDateTime, LocalDateTime>> prepareProducingAndCompletedMap(
                SortedSet<FactoryProcessSequence> shadowFactorySequenceSet
        ) {

            SortedMap<FactoryProcessSequence, Pair<LocalDateTime, LocalDateTime>> computingProducingCompletedMap
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
                        new Pair<>(arrangeDateTime, completedDateTime)
                );

            }

            return computingProducingCompletedMap;


        }

    }

}
