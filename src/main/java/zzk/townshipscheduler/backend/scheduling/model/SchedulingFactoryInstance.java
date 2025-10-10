package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.javatuples.Pair;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Gatherer;

@Log4j2
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@PlanningEntity
public class SchedulingFactoryInstance {

    private static final Gatherer<SchedulingProducingArrangement, Void, Pair<SchedulingProducingArrangement, FactoryComputedDateTimePair>>
            SLOT_GATHERER
            = Gatherer.of(
            () -> null,
            (_, schedulingProducingArrangement, downstream) -> {
                SchedulingDateTimeSlot schedulingDateTimeSlot = schedulingProducingArrangement.getPlanningDateTimeSlot();
                if (schedulingDateTimeSlot == null) {
                    return true;
                }

                LocalDateTime start = schedulingDateTimeSlot.getStart();
                return downstream.push(new Pair<>(
                        schedulingProducingArrangement,
                        new FactoryComputedDateTimePair(
                                start,
                                start.plus(schedulingProducingArrangement.getProducingDuration())
                        )
                )) && !downstream.isRejecting();
            },
            Gatherer.defaultCombiner(),
            Gatherer.defaultFinisher()
    );

    private static final Gatherer<SchedulingProducingArrangement, FormerCompletedDateTimeRef, Pair<SchedulingProducingArrangement, FactoryComputedDateTimePair>>
            QUEUE_GATHERER
            = Gatherer.ofSequential(
            FormerCompletedDateTimeRef::new,
            (formerCompletedDateTimeRef, schedulingProducingArrangement, downstream) -> {
                SchedulingDateTimeSlot schedulingDateTimeSlot = schedulingProducingArrangement.getPlanningDateTimeSlot();
                if (schedulingDateTimeSlot == null) {
                    return true;
                }

                LocalDateTime arrangeDateTime = schedulingDateTimeSlot.getStart();
                LocalDateTime start = (formerCompletedDateTimeRef.value == null)
                        ? arrangeDateTime
                        : formerCompletedDateTimeRef.value.isAfter(arrangeDateTime)
                                ? formerCompletedDateTimeRef.value
                                : arrangeDateTime;
                LocalDateTime end = start.plus(schedulingProducingArrangement.getProducingDuration());
                return downstream.push(
                        new Pair<>(
                                schedulingProducingArrangement,
                                new FactoryComputedDateTimePair(
                                        start,
                                        formerCompletedDateTimeRef.value = end
                                )
                        )
                ) && !downstream.isRejecting();
            }
    );

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

    @ShadowVariable(supplierName = "supplierForArrangementToComputedPairMap")
    private TreeMap<SchedulingProducingArrangement, FactoryComputedDateTimePair> arrangementToComputedPairMap = new TreeMap<>();

    @ShadowSources(
            {
                    "planningFactoryInstanceProducingArrangements",
                    "planningFactoryInstanceProducingArrangements[].planningDateTimeSlot"
            }
    )
    public TreeMap<SchedulingProducingArrangement, FactoryComputedDateTimePair> supplierForArrangementToComputedPairMap() {
        return this.planningFactoryInstanceProducingArrangements.stream()
                .sorted(SchedulingProducingArrangement::compareTo)
                .gather(weatherFactoryProducingTypeIsQueue() ? QUEUE_GATHERER : SLOT_GATHERER)
                .collect(
                        TreeMap::new,
                        (treeMap, pair) -> treeMap.put(
                                pair.getValue0(),
                                pair.getValue1()
                        ),
                        TreeMap::putAll
                );
    }

    public boolean weatherFactoryProducingTypeIsQueue() {
        return this.getSchedulingFactoryInfo().weatherFactoryProducingTypeIsQueue();
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

    public Integer queryIndexInFactoryArrangements(SchedulingProducingArrangement schedulingProducingArrangement) {
        return this.planningFactoryInstanceProducingArrangements.indexOf(schedulingProducingArrangement);
    }

    public SchedulingProducingArrangement queryPreviousFactoryProducingArrangement(SchedulingProducingArrangement schedulingProducingArrangement) {
        if (getSchedulingFactoryInfo().weatherFactoryProducingTypeIsQueue()) {
            return this.arrangementToComputedPairMap.lowerKey(schedulingProducingArrangement);
        } else {
            return null;
        }
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

    public TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair> removeFactoryProcessSequence(
            TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair> factoryProcessToDateTimePairMap,
            FactoryProcessSequence factoryProcessSequence
    ) {

        if (factoryProcessToDateTimePairMap.remove(factoryProcessSequence) == null) {
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

    public LocalDateTime queryArrangementProducingDateTime(SchedulingProducingArrangement schedulingProducingArrangement) {
        FactoryComputedDateTimePair computedDateTimePair
                = this.arrangementToComputedPairMap.get(schedulingProducingArrangement);
        return Objects.nonNull(computedDateTimePair) ? computedDateTimePair.producingDateTime() : null;
    }

    public LocalDateTime queryArrangementCompletedDateTime(SchedulingProducingArrangement schedulingProducingArrangement) {
        FactoryComputedDateTimePair computedDateTimePair
                = this.arrangementToComputedPairMap.get(schedulingProducingArrangement);
        return Objects.nonNull(computedDateTimePair) ? computedDateTimePair.completedDateTime() : null;
    }

    private static final class FormerCompletedDateTimeRef {

        public LocalDateTime value = null;

    }

}
