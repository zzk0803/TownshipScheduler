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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Gatherer;
import java.util.stream.Stream;

import static zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInstance.FactoryComputedDateTimePair;
import static zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInstance.FactoryProcessSequence;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@PlanningEntity
public class SchedulingPlayer implements Serializable {

    public static final LocalTime DEFAULT_SLEEP_START = LocalTime.MIDNIGHT.minusHours(2);

    public static final LocalTime DEFAULT_SLEEP_END = LocalTime.MIDNIGHT.plusHours(8);


    public static final Predicate<FactoryProcessSequence> FACTORY_PROCESS_SEQUENCE_ASSIGNED_PREDICATE
            = factoryProcessSequence -> Objects.nonNull(
            factoryProcessSequence.getSchedulingFactoryInstanceReadableIdentifier())
                                        && Objects.nonNull(factoryProcessSequence.getArrangeDateTime());

    public static final Gatherer<FactoryProcessSequence, AtomicReference<LocalDateTime>, Pair<FactoryProcessSequence, FactoryComputedDateTimePair>> QUEUE_GATHERER
            = Gatherer.ofSequential(
            AtomicReference::new, (previousCompletedDateTimeRef, sequence, downstream) -> {
                LocalDateTime arrangeDateTime = sequence.getArrangeDateTime();
                if (arrangeDateTime == null) {
                    return true;
                }

                LocalDateTime previousCompletedDateTime = previousCompletedDateTimeRef.get();
                LocalDateTime currentProducingDateTime = (previousCompletedDateTime == null || previousCompletedDateTime.isBefore(arrangeDateTime))
                        ? arrangeDateTime
                        : previousCompletedDateTime;

                LocalDateTime currentCompletedDateTime = currentProducingDateTime.plus(sequence.getProducingDuration());

                previousCompletedDateTimeRef.set(currentCompletedDateTime);

                return downstream.push(
                        new Pair<>(
                                sequence,
                                new FactoryComputedDateTimePair(currentProducingDateTime, currentCompletedDateTime)
                        )
                ) && !downstream.isRejecting();
            }
    );

    public static final Function<Stream<FactoryProcessSequence>, Stream<Pair<FactoryProcessSequence, FactoryComputedDateTimePair>>> QUEUE_PROCESSOR
            = stream -> stream.gather(QUEUE_GATHERER);

    public static final Function<Stream<FactoryProcessSequence>, Stream<Pair<FactoryProcessSequence, FactoryComputedDateTimePair>>> SLOT_PROCESSOR
            = stream -> stream.filter(seq -> seq.getArrangeDateTime() != null)
            .map(
                    seq -> new Pair<>(
                            seq,
                            new FactoryComputedDateTimePair(
                                    seq.getArrangeDateTime(),
                                    seq.getArrangeDateTime().plus(seq.getProducingDuration())
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
    private Map<FactoryProcessSequence, FactoryComputedDateTimePair> shadowComputedMap = new LinkedHashMap<>();

    @ShadowSources(value = {"schedulingProducingArrangements[].factoryProcessSequence"})
    public Map<FactoryProcessSequence, FactoryComputedDateTimePair> supplierForShadowComputedMap() {

        return this.schedulingProducingArrangements.stream()
                .filter(schedulingProducingArrangement -> schedulingProducingArrangement.boolPlanningWellBeing() && schedulingProducingArrangement.getFactoryProcessSequence() != null)
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

    private Collector<SchedulingProducingArrangement, ?, Map<FactoryReadableIdentifier, Map<FactoryProcessSequence, FactoryComputedDateTimePair>>> buildSinglePassCollector(
            Predicate<SchedulingProducingArrangement> factoryTypePredicate,
            Function<Stream<FactoryProcessSequence>, Stream<Pair<FactoryProcessSequence, FactoryComputedDateTimePair>>> processSequenceToComputedPairFunction
    ) {

        return Collectors.filtering(
                factoryTypePredicate,
                Collectors.groupingBy(
                        schedulingProducingArrangement -> schedulingProducingArrangement.getPlanningFactoryInstance()
                                .getFactoryReadableIdentifier(),
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

    private Map<FactoryProcessSequence, FactoryComputedDateTimePair> mergeFinalResults(
            Map<FactoryReadableIdentifier, Map<FactoryProcessSequence, FactoryComputedDateTimePair>> slotMap,
            Map<FactoryReadableIdentifier, Map<FactoryProcessSequence, FactoryComputedDateTimePair>> queueMap
    ) {

        Map<FactoryProcessSequence, FactoryComputedDateTimePair> result = new LinkedHashMap<>();
        slotMap.values().forEach(result::putAll);
        queueMap.values().forEach(result::putAll);
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

}
