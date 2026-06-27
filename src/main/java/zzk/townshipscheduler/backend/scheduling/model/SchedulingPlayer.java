package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.cloner.DeepPlanningClone;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.arxila.javatuples.Pair;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Gatherer;

import static zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInstance.FactoryComputedDateTimePair;
import static zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInstance.FactoryProcessSequence;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@PlanningEntity
public class SchedulingPlayer
        implements Serializable {

    public static final LocalTime DEFAULT_SLEEP_START = LocalTime.MIDNIGHT.minusHours(2);

    public static final LocalTime DEFAULT_SLEEP_END = LocalTime.MIDNIGHT.plusHours(8);

    //<editor-fold desc="SLOT_GATHERER">
    public static final Gatherer<
            FactoryProcessSequence,
            FormerCompletedDateTimeRef,
            Pair<FactoryProcessSequence, FactoryComputedDateTimePair>
            > SLOT_GATHERER = Gatherer.of(
            () -> null,
            (_, arrangement, downstream) -> {

                var arrangeDateTime = arrangement.getArrangeDateTime();
                var producingDuration = arrangement.getProducingDuration();
                if (arrangeDateTime == null) {
                    return true;
                }

                return downstream.push(new Pair<>(
                        arrangement,
                        new FactoryComputedDateTimePair(
                                arrangeDateTime,
                                arrangeDateTime.plus(producingDuration)
                        )
                )) && !downstream.isRejecting();

            },
            Gatherer.defaultCombiner(),
            Gatherer.defaultFinisher()
    );
    //</editor-fold>

    //<editor-fold desc="QUEUE_GATHERER">
    public static final Gatherer<
            FactoryProcessSequence,
            FormerCompletedDateTimeRef,
            Pair<FactoryProcessSequence, FactoryComputedDateTimePair>
            > QUEUE_GATHERER = Gatherer.ofSequential(
            FormerCompletedDateTimeRef::new,
            (formerCompletedRef, arrangement, downstream) -> {
                LocalDateTime arrangeDateTime = arrangement.getArrangeDateTime();
                if (arrangeDateTime == null) {
                    return true;
                }
                LocalDateTime previousCompletedDateTime = formerCompletedRef.value;
                LocalDateTime start;
                if (previousCompletedDateTime == null) {
                    start = arrangeDateTime;
                } else {
                    start = previousCompletedDateTime.isAfter(arrangeDateTime)
                            ? previousCompletedDateTime
                            : arrangeDateTime;
                }
                LocalDateTime end = start.plus(arrangement.getProducingDuration());
                formerCompletedRef.value = end;
                return downstream.push(new Pair<>(
                        arrangement,
                        new FactoryComputedDateTimePair(
                                start,
                                end
                        )
                )) && !downstream.isRejecting();
            }
    );
    //</editor-fold>

    public static final Predicate<FactoryProcessSequence> FACTORY_PROCESS_SEQUENCE_ASSIGNED_PREDICATE =
            factoryProcessSequence -> Objects.nonNull(factoryProcessSequence.getSchedulingFactoryInstanceReadableIdentifier()) && Objects.nonNull(
                    factoryProcessSequence.getArrangeDateTime());

    @Serial
    private static final long serialVersionUID = -2467531974779697853L;

    @EqualsAndHashCode.Include
    @ToString.Include
    private String id = "test";

    private Map<SchedulingProduct, Integer> productAmountMap;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime sleepStart = DEFAULT_SLEEP_START;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime sleepEnd = DEFAULT_SLEEP_END;

    @DeepPlanningClone
    private NavigableSet<SchedulingProducingArrangement> schedulingProducingArrangements;

    @ToString.Include
    @DeepPlanningClone
    @ShadowVariable(supplierName = "supplierForShadowComputedMap")
    private Map<FactoryProcessSequence, FactoryComputedDateTimePair> shadowComputedMap =
            new LinkedHashMap<>();

    @ShadowSources(value = {"schedulingProducingArrangements[].factoryProcessSequence"})
    public Map<FactoryProcessSequence, FactoryComputedDateTimePair> supplierForShadowComputedMap() {
        Map<FactoryReadableIdentifier, Map<FactoryProcessSequence, FactoryComputedDateTimePair>> slotCollected =
                this.schedulingProducingArrangements.stream()
                        .filter(SchedulingProducingArrangement::boolPlanningWellBeing)
                        .filter(SchedulingProducingArrangement::weatherFactoryProducingTypeIsSlot)
                        .collect(Collectors.groupingBy(
                                schedulingProducingArrangement -> schedulingProducingArrangement.getPlanningFactoryInstance()
                                        .getFactoryReadableIdentifier(),
                                Collectors.collectingAndThen(
                                        Collectors.toCollection(TreeSet::new),
                                        producingArrangements -> producingArrangements.stream()
                                                .map(SchedulingProducingArrangement::getFactoryProcessSequence)
                                                .filter(FACTORY_PROCESS_SEQUENCE_ASSIGNED_PREDICATE)
                                                .sorted(FactoryProcessSequence.COMPARATOR)
                                                .gather(SLOT_GATHERER)
                                                .collect(
                                                        LinkedHashMap::new,
                                                        (accumulatorMap, pair) -> accumulatorMap.put(
                                                                pair.value0(),
                                                                pair.value1()
                                                        ),
                                                        LinkedHashMap::putAll
                                                )
                                )
                        ));

        Map<FactoryReadableIdentifier, Map<FactoryProcessSequence, FactoryComputedDateTimePair>> queueCollected =
                this.schedulingProducingArrangements.stream()
                        .filter(SchedulingProducingArrangement::boolPlanningWellBeing)
                        .filter(SchedulingProducingArrangement::weatherFactoryProducingTypeIsQueue)
                        .collect(Collectors.groupingBy(
                                schedulingProducingArrangement -> schedulingProducingArrangement.getPlanningFactoryInstance()
                                        .getFactoryReadableIdentifier(),
                                Collectors.collectingAndThen(
                                        Collectors.toCollection(TreeSet::new),
                                        producingArrangements -> producingArrangements.stream()
                                                .map(SchedulingProducingArrangement::getFactoryProcessSequence)
                                                .filter(FACTORY_PROCESS_SEQUENCE_ASSIGNED_PREDICATE)
                                                .sorted(FactoryProcessSequence.COMPARATOR)
                                                .gather(QUEUE_GATHERER)
                                                .collect(
                                                        LinkedHashMap::new,
                                                        (accumulatorMap, pair) -> accumulatorMap.put(
                                                                pair.value0(),
                                                                pair.value1()
                                                        ),
                                                        LinkedHashMap::putAll
                                                )
                                )
                        ));

        Map<FactoryProcessSequence, FactoryComputedDateTimePair> result = new LinkedHashMap<>();
        for (Map<FactoryProcessSequence, FactoryComputedDateTimePair> computedDateTimePairMap : slotCollected.values()) {
            result.putAll(computedDateTimePairMap);
        }
        for (Map<FactoryProcessSequence, FactoryComputedDateTimePair> computedDateTimePairMap : queueCollected.values()) {
            result.putAll(computedDateTimePairMap);
        }
        return result;
    }

    public LocalDateTime queryProducingDateTime(SchedulingProducingArrangement schedulingProducingArrangement) {
        FactoryComputedDateTimePair computedDateTimePair = query(schedulingProducingArrangement);
        if (computedDateTimePair == null) {
            return null;
        }
        return computedDateTimePair.producingDateTime();
    }

    public FactoryComputedDateTimePair query(SchedulingProducingArrangement schedulingProducingArrangement) {
        return query(schedulingProducingArrangement.getFactoryProcessSequence());
    }

    public FactoryComputedDateTimePair query(FactoryProcessSequence factoryProcessSequence) {
        if (Objects.isNull(factoryProcessSequence)) {
            return null;
        }

        return this.shadowComputedMap.get(factoryProcessSequence);
    }

    public LocalDateTime queryCompletedDateTime(SchedulingProducingArrangement schedulingProducingArrangement) {
        FactoryComputedDateTimePair computedDateTimePair = query(schedulingProducingArrangement);
        if (computedDateTimePair == null) {
            return null;
        }
        return computedDateTimePair.completedDateTime();
    }

    public static class FormerCompletedDateTimeRef {

        public LocalDateTime value = null;

    }

}
