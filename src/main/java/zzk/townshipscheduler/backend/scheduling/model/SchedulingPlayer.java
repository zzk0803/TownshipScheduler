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
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.*;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@PlanningEntity
public class SchedulingPlayer
        implements Serializable {

    public static final LocalTime DEFAULT_SLEEP_START = LocalTime.MIN.minusHours(2);

    public static final LocalTime DEFAULT_SLEEP_END = LocalTime.MIDNIGHT.plusHours(8);

    private static final Predicate<FactoryProcessSequence> FACTORY_PROCESS_SEQUENCE_ASSIGNED_PREDICATE
            = factoryProcessSequence -> Objects.nonNull(
            factoryProcessSequence.getSchedulingFactoryInstanceReadableIdentifier())
                                        && Objects.nonNull(factoryProcessSequence.getArrangeDateTime());

    private static final Function<Stream<FactoryProcessSequence>, Stream<Pair<FactoryProcessSequence, ComputedDateTimePair>>> QUEUE_PROCESSOR_2
            = stream -> stream.gather(
            Gatherers.scan(
                    () -> new Pair<>(
                            FactoryProcessSequence.EMPTY_NULL_VALUE,
                            ComputedDateTimePair.EMPTY_NULL_VALUE
                    ),
                    (previousPair, factoryProcessSequence) -> {
                        LocalDateTime arrangeDateTime = factoryProcessSequence.getArrangeDateTime();
                        Duration duration = factoryProcessSequence.getProducingDuration();
                        ComputedDateTimePair previousComputedDateTimePair = previousPair.value1();
                        if (previousComputedDateTimePair.equals(ComputedDateTimePair.EMPTY_NULL_VALUE)) {
                            return new Pair<>(
                                    factoryProcessSequence,
                                    new ComputedDateTimePair(
                                            arrangeDateTime,
                                            arrangeDateTime.plus(duration)
                                    )
                            );
                        } else {
                            LocalDateTime previousCompletedDateTime = previousComputedDateTimePair.completedDateTime();
                            LocalDateTime currentProducingDateTime =
                                    (previousCompletedDateTime == null
                                     || previousCompletedDateTime.isBefore(arrangeDateTime))
                                            ? arrangeDateTime
                                            : previousCompletedDateTime;

                            LocalDateTime currentCompletedDateTime = currentProducingDateTime.plus(duration);
                            return new Pair<>(
                                    factoryProcessSequence,
                                    new ComputedDateTimePair(
                                            currentProducingDateTime,
                                            currentCompletedDateTime
                                    )
                            );
                        }
                    }
            )
    );

    private static final Function<Stream<FactoryProcessSequence>, Stream<Pair<FactoryProcessSequence, ComputedDateTimePair>>> QUEUE_PROCESSOR
            = stream -> stream.gather(
            Gatherer.<FactoryProcessSequence, AtomicReference<LocalDateTime>, Pair<FactoryProcessSequence, ComputedDateTimePair>>ofSequential(
                    AtomicReference::new,
                    (previousCompletedDateTimeRef, sequence, downstream) -> {
                        LocalDateTime arrangeDateTime = sequence.getArrangeDateTime();
                        if (arrangeDateTime == null) {
                            return true;
                        }

                        LocalDateTime previousCompletedDateTime = previousCompletedDateTimeRef.get();
                        LocalDateTime currentProducingDateTime =
                                (previousCompletedDateTime == null
                                 || previousCompletedDateTime.isBefore(arrangeDateTime))
                                        ? arrangeDateTime
                                        : previousCompletedDateTime;

                        LocalDateTime currentCompletedDateTime = currentProducingDateTime.plus(sequence.getProducingDuration());

                        previousCompletedDateTimeRef.set(currentCompletedDateTime);

                        return downstream.push(
                                new Pair<>(
                                        sequence,
                                        new ComputedDateTimePair(
                                                currentProducingDateTime,
                                                currentCompletedDateTime
                                        )
                                )
                        ) && !downstream.isRejecting();
                    }
            )
    );

    private static final Function<Stream<FactoryProcessSequence>, Stream<Pair<FactoryProcessSequence, ComputedDateTimePair>>> SLOT_PROCESSOR
            = stream -> stream.filter(factoryProcessSequence -> factoryProcessSequence.getArrangeDateTime() != null)
            .map(
                    factoryProcessSequence -> new Pair<>(
                            factoryProcessSequence,
                            new ComputedDateTimePair(
                                    factoryProcessSequence.getArrangeDateTime(),
                                    factoryProcessSequence.getArrangeDateTime()
                                            .plus(factoryProcessSequence.getProducingDuration())
                            )
                    )
            );

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
    private Map<FactoryProcessSequence, ComputedDateTimePair> shadowComputedMap = new LinkedHashMap<>();

    @ShadowSources(value = {"schedulingProducingArrangements[].factoryProcessSequence"})
    public Map<FactoryProcessSequence, ComputedDateTimePair> supplierForShadowComputedMap() {

        return this.schedulingProducingArrangements.stream()
                .filter(schedulingProducingArrangement -> schedulingProducingArrangement.boolPlanningAssigned() && schedulingProducingArrangement.getFactoryProcessSequence() != null)
                .collect(
                        Collectors.teeing(
                                buildSinglePassCollector(
                                        SchedulingProducingArrangement::weatherFactoryProducingTypeIsSlot,
                                        SLOT_PROCESSOR
                                ),
                                buildSinglePassCollector(
                                        SchedulingProducingArrangement::weatherFactoryProducingTypeIsQueue,
                                        QUEUE_PROCESSOR
                                ),
                                this::mergeFinalResults
                        )
                );
    }

    private Collector<SchedulingProducingArrangement, ?, Map<FactoryReadableIdentifier, Map<FactoryProcessSequence, ComputedDateTimePair>>> buildSinglePassCollector(
            Predicate<SchedulingProducingArrangement> factoryTypePredicate,
            Function<Stream<FactoryProcessSequence>, Stream<Pair<FactoryProcessSequence, ComputedDateTimePair>>> processSequenceToComputedPairFunction
    ) {

        return Collectors.filtering(
                factoryTypePredicate,
                Collectors.groupingBy(
                        SchedulingProducingArrangement::getPlanningFactoryInstanceReadableIdentifier,
                        LinkedHashMap::new,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                schedulingProducingArrangements -> {
                                    schedulingProducingArrangements.sort(
                                            Comparator.comparing(
                                                    SchedulingProducingArrangement::getFactoryProcessSequence,
                                                    FactoryProcessSequence.COMPARATOR
                                            )
                                    );

                                    return processSequenceToComputedPairFunction.apply(
                                            schedulingProducingArrangements.stream()
                                                    .map(SchedulingProducingArrangement::getFactoryProcessSequence)
                                    ).collect(
                                            Collectors.toMap(
                                                    Pair::value0,
                                                    Pair::value1,
                                                    (existing, replacement) -> replacement,
                                                    LinkedHashMap::new
                                            )
                                    );
                                }
                        )
                )
        );
    }

    private Map<FactoryProcessSequence, ComputedDateTimePair> mergeFinalResults(
            Map<FactoryReadableIdentifier, Map<FactoryProcessSequence, ComputedDateTimePair>> slotMap,
            Map<FactoryReadableIdentifier, Map<FactoryProcessSequence, ComputedDateTimePair>> queueMap
    ) {

        Map<FactoryProcessSequence, ComputedDateTimePair> result = new LinkedHashMap<>();
        slotMap.values().forEach(result::putAll);
        queueMap.values().forEach(result::putAll);
        return result;
    }

    public LocalDateTime queryProducingDateTime(SchedulingProducingArrangement schedulingProducingArrangement) {
        ComputedDateTimePair computedDateTimePair = query(schedulingProducingArrangement);
        if (computedDateTimePair == null) {
            return null;
        }
        return computedDateTimePair.producingDateTime();
    }

    public ComputedDateTimePair query(SchedulingProducingArrangement schedulingProducingArrangement) {
        return query(schedulingProducingArrangement.getFactoryProcessSequence());
    }

    public ComputedDateTimePair query(FactoryProcessSequence factoryProcessSequence) {
        if (Objects.isNull(factoryProcessSequence)) {
            return null;
        }

        return this.shadowComputedMap.get(factoryProcessSequence);
    }

    public LocalDateTime queryCompletedDateTime(SchedulingProducingArrangement schedulingProducingArrangement) {
        ComputedDateTimePair computedDateTimePair = query(schedulingProducingArrangement);
        if (computedDateTimePair == null) {
            return null;
        }
        return computedDateTimePair.completedDateTime();
    }

}
