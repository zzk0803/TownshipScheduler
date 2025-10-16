package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.solution.cloner.DeepPlanningClone;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.javatuples.Pair;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Gatherer;

@Slf4j
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@PlanningEntity
public class SchedulingArrangementsGlobalState {

    //<editor-fold desc="SLOT_GATHERER">
    public static final Gatherer<FactoryProcessSequence, FormerCompletedDateTimeRef, Pair<FactoryProcessSequence, FactoryComputedDateTimePair>> SLOT_GATHERER =
            Gatherer.of(
                    () -> null,
                    (_, arrangement, downstream) -> {

                        var arrangeDateTime = arrangement.getArrangeDateTime();
                        var producingDuration = arrangement.getProducingDuration();
                        if (arrangeDateTime == null) {
                            return true;
                        }

                        return downstream.push(
                                new Pair<>(
                                        arrangement,
                                        new FactoryComputedDateTimePair(
                                                arrangeDateTime,
                                                arrangeDateTime.plus(producingDuration)
                                        )
                                )
                        ) && !downstream.isRejecting();

                    },
                    Gatherer.defaultCombiner(),
                    Gatherer.defaultFinisher()
            );
    //</editor-fold>

    //<editor-fold desc="QUEUE_GATHERER">
    public static final Gatherer<FactoryProcessSequence, FormerCompletedDateTimeRef, Pair<FactoryProcessSequence, FactoryComputedDateTimePair>> QUEUE_GATHERER
            = Gatherer.ofSequential(
            FormerCompletedDateTimeRef::new,
            (formerCompletedRef, arrangement, downstream) -> {
                LocalDateTime arrangeDateTime = arrangement.getArrangeDateTime();
                if (arrangeDateTime == null) {
                    return true;
                }
                LocalDateTime previousCompletedDateTime = formerCompletedRef.value;
                LocalDateTime start = (previousCompletedDateTime == null)
                        ? arrangeDateTime
                        : previousCompletedDateTime.isAfter(
                                arrangeDateTime)
                                ? previousCompletedDateTime
                                : arrangeDateTime;
                LocalDateTime end = start.plus(arrangement.getProducingDuration());
                formerCompletedRef.value = end;
                return downstream.push(
                        new Pair<>(
                                arrangement,
                                new FactoryComputedDateTimePair(
                                        start,
                                        end
                                )
                        )
                ) && !downstream.isRejecting();
            }
    );
    //</editor-fold>

    public static final BiPredicate<FactoryProcessSequence, FactoryProcessSequence> BI_PREDICATE_1
            = (fps1, fps2) -> Objects.equals(
            fps1.getArrangementId(),
            fps2.getArrangementId()
    );

    public static final BiPredicate<FactoryProcessSequence, FactoryProcessSequence> BI_PREDICATE_2
            = (fps1, fps2) -> Objects.equals(
            fps1.getArrangeDateTime(),
            fps2.getArrangeDateTime()
    );

    public static final BiPredicate<FactoryProcessSequence, FactoryProcessSequence> BI_PREDICATE_3
            = (fps1, fps2) -> Objects.equals(
            fps1.getFactoryReadableIdentifier(),
            fps2.getFactoryReadableIdentifier()
    );

    public static final BiPredicate<FactoryProcessSequence, FactoryProcessSequence> FACTORY_PROCESS_SEQUENCE_BI_PREDICATE
            = BI_PREDICATE_1.and(
            BI_PREDICATE_2.negate()
                    .or(BI_PREDICATE_3.negate())
    );


    @PlanningId
    @EqualsAndHashCode.Include
    private String id = "SchedulingArrangementsGlobalState";

    private List<SchedulingProducingArrangement> schedulingProducingArrangements;

    @DeepPlanningClone
    private Map<FactoryReadableIdentifier, TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair>> companyComputedMap
            = new LinkedHashMap<>();

    @ToString.Include
    @ShadowVariable(supplierName = "supplierForShadowComputedMap")
    private Map<FactoryReadableIdentifier, TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair>> shadowComputedMap = new LinkedHashMap<>();

    @ShadowSources(value = {"schedulingProducingArrangements[].factoryProcessSequence"})
    public Map<FactoryReadableIdentifier, TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair>> supplierForShadowComputedMap() {
        Map<FactoryReadableIdentifier, TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair>> result = null;
        if ( companyComputedMap.isEmpty()) {
            result = fullCompute();
        } else {
            result = deltaCompute();
        }
        companyComputedMap = result;
        return result;
    }

    @NotNull
    private Map<FactoryReadableIdentifier, TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair>> fullCompute() {
        Map<FactoryReadableIdentifier, TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair>> slotCollected
                = this.schedulingProducingArrangements.stream()
                .filter(SchedulingProducingArrangement::isPlanningAssigned)
                .filter(SchedulingProducingArrangement::weatherFactoryProducingTypeIsSlot)
                .collect(
                        Collectors.groupingBy(
                                schedulingProducingArrangement -> schedulingProducingArrangement.getPlanningFactoryInstance()
                                        .getFactoryReadableIdentifier(),
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        producingArrangements -> producingArrangements.stream()
                                                .map(SchedulingProducingArrangement::getFactoryProcessSequence)
                                                .sorted(FactoryProcessSequence.COMPARATOR)
                                                .gather(SLOT_GATHERER)
                                                .collect(
                                                        TreeMap::new,
                                                        (treeMap, pair) -> treeMap.put(
                                                                pair.getValue0(),
                                                                pair.getValue1()
                                                        ),
                                                        TreeMap::putAll
                                                )
                                )
                        )
                );

        Map<FactoryReadableIdentifier, TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair>> result
                = this.schedulingProducingArrangements.stream()
                .filter(SchedulingProducingArrangement::isPlanningAssigned)
                .filter(SchedulingProducingArrangement::weatherFactoryProducingTypeIsQueue)
                .collect(
                        Collectors.groupingBy(
                                schedulingProducingArrangement -> schedulingProducingArrangement.getPlanningFactoryInstance()
                                        .getFactoryReadableIdentifier(),
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        producingArrangements -> producingArrangements.stream()
                                                .map(SchedulingProducingArrangement::getFactoryProcessSequence)
                                                .sorted(FactoryProcessSequence.COMPARATOR)
                                                .gather(QUEUE_GATHERER)
                                                .collect(
                                                        TreeMap::new,
                                                        (treeMap, pair) -> treeMap.put(
                                                                pair.getValue0(),
                                                                pair.getValue1()
                                                        ),
                                                        TreeMap::putAll
                                                )
                                )
                        )
                );

        result.putAll(slotCollected);
        return result;
    }

    @NotNull
    private Map<FactoryReadableIdentifier, TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair>> deltaCompute() {
        this.schedulingProducingArrangements.stream()
                .map(SchedulingProducingArrangement::getFactoryProcessSequence)
                .forEach(factoryProcessSequence -> {
                    FactoryReadableIdentifier factoryReadableIdentifier = factoryProcessSequence.getFactoryReadableIdentifier();
                    companyComputedMap.compute(
                            factoryReadableIdentifier,
                            (keyInMap, oldPartMap) -> deltaCompute(
                                    factoryProcessSequence,
                                    Objects.requireNonNullElseGet(oldPartMap, TreeMap::new)
                            )
                    );
                });
        return companyComputedMap;
    }

    public TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair> deltaCompute(
            FactoryProcessSequence argProcessSequence,
            TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair> partMap
    ) {

        List<FactoryProcessSequence> toDel = partMap.keySet().stream()
                .filter(existProcessSequence -> FACTORY_PROCESS_SEQUENCE_BI_PREDICATE.test(existProcessSequence, argProcessSequence))
                .collect(Collectors.toCollection(ArrayList::new));

        if (!toDel.isEmpty()) {
            for (FactoryProcessSequence sequence : toDel) {
                this.removeFactoryProcessSequence(
                        sequence,
                        sequence.getFactoryReadableIdentifier().isBoolFactorySlotType(),
                        partMap
                );
            }
        }

        addFactoryProcessSequence(
                argProcessSequence,
                argProcessSequence.getFactoryReadableIdentifier().isBoolFactorySlotType(),
                partMap
        );

        return partMap;
    }

    public void addFactoryProcessSequence(
            FactoryProcessSequence factoryProcessSequence, boolean boolFactorySlotType,
            TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair> partMap
    ) {

        if (boolFactorySlotType) {
            partMap.put(
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
                partMap.lowerEntry(factoryProcessSequence);

        LocalDateTime producingDateTime = calcProducingDateTime(
                factoryProcessSequence,
                prefixProducingPairEntry
        );
        LocalDateTime completedDateTime = calcCompletedDateTime(factoryProcessSequence, producingDateTime);
        partMap.put(
                factoryProcessSequence,
                new FactoryComputedDateTimePair(producingDateTime, completedDateTime)
        );

        NavigableMap<FactoryProcessSequence, FactoryComputedDateTimePair> tailOfProcessSequencePairMap
                = partMap.tailMap(
                factoryProcessSequence,
                false
        );
        cascade(tailOfProcessSequencePairMap, partMap);
    }

    public void removeFactoryProcessSequence(
            FactoryProcessSequence factoryProcessSequence,
            boolean boolFactorySlotType,
            TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair> partMap
    ) {

        if (partMap.remove(factoryProcessSequence) == null) {
            return;
        }

        if (boolFactorySlotType) {
            return;
        }

        NavigableMap<FactoryProcessSequence, FactoryComputedDateTimePair> tail =
                partMap.tailMap(factoryProcessSequence, false);
        cascade(tail, partMap);
    }

    private void cascade(
            NavigableMap<FactoryProcessSequence, FactoryComputedDateTimePair> tailOfProcessSequencePairMap,
            TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair> partMap
    ) {
        if (tailOfProcessSequencePairMap.isEmpty()) {
            return;
        }

        List<FactoryProcessSequence> taiKeyList = new ArrayList<>(tailOfProcessSequencePairMap.keySet());

        Map.Entry<FactoryProcessSequence, FactoryComputedDateTimePair> prefixEntry
                = partMap.lowerEntry(taiKeyList.get(0));

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
            partMap.put(
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

    private LocalDateTime calcCompletedDateTime(
            FactoryProcessSequence factoryProcessSequence,
            LocalDateTime producingDateTime
    ) {
        return producingDateTime.plus(factoryProcessSequence.getProducingDuration());
    }


    public FactoryComputedDateTimePair query(FactoryProcessSequence factoryProcessSequence) {
        TreeMap<FactoryProcessSequence, FactoryComputedDateTimePair> computedDateTimePairTreeMap
                = this.shadowComputedMap.get(factoryProcessSequence.getFactoryReadableIdentifier());
        if (Objects.isNull(computedDateTimePairTreeMap)) {
            return null;
        }
        return computedDateTimePairTreeMap.get(factoryProcessSequence);
    }

    public static class FormerCompletedDateTimeRef {

        public LocalDateTime value = null;

    }

}
