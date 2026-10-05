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

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@PlanningEntity
public class SchedulingPlayer
        implements Serializable {

    public static final Comparator<SchedulingProducingArrangement> ARRANGEMENT_COMPARATOR =
            Comparator.comparing(SchedulingProducingArrangement::getPlanningFactoryInstanceReadableIdentifier)
                    .thenComparing(SchedulingProducingArrangement::getArrangeDateTime)
                    .thenComparing(SchedulingProducingArrangement::getId);

    public static final LocalTime DEFAULT_SLEEP_START = LocalTime.MIN.minusHours(2);

    public static final LocalTime DEFAULT_SLEEP_END = LocalTime.MIDNIGHT.plusHours(8);

    private static final Function<Stream<SchedulingProducingArrangement>, Stream<Pair<SchedulingProducingArrangement, ComputedDateTimePair>>> QUEUE_PROCESSOR
            = stream -> stream.gather(
            Gatherer.<SchedulingProducingArrangement, AtomicReference<LocalDateTime>, Pair<SchedulingProducingArrangement, ComputedDateTimePair>>ofSequential(
                    AtomicReference::new,
                    (previousCompletedDateTimeRef, arrangement, downstream) -> {
                        LocalDateTime arrangeDateTime = arrangement.getArrangeDateTime();
                        if (arrangeDateTime == null) {
                            return true;
                        }

                        LocalDateTime previousCompletedDateTime = previousCompletedDateTimeRef.get();
                        LocalDateTime currentProducingDateTime =
                                (previousCompletedDateTime == null
                                 || previousCompletedDateTime.isBefore(arrangeDateTime))
                                        ? arrangeDateTime
                                        : previousCompletedDateTime;

                        LocalDateTime currentCompletedDateTime = currentProducingDateTime.plus(arrangement.getProducingDuration());

                        previousCompletedDateTimeRef.set(currentCompletedDateTime);

                        return downstream.push(
                                new Pair<>(
                                        arrangement,
                                        new ComputedDateTimePair(
                                                currentProducingDateTime,
                                                currentCompletedDateTime
                                        )
                                )
                        ) && !downstream.isRejecting();
                    }
            )
    );

    private static final Function<Stream<SchedulingProducingArrangement>, Stream<Pair<SchedulingProducingArrangement, ComputedDateTimePair>>> SLOT_PROCESSOR
            = stream -> stream.filter(schedulingProducingArrangement -> schedulingProducingArrangement.getArrangeDateTime() != null)
            .map(
                    schedulingProducingArrangement -> new Pair<>(
                            schedulingProducingArrangement,
                            new ComputedDateTimePair(
                                    schedulingProducingArrangement.getArrangeDateTime(),
                                    schedulingProducingArrangement.getArrangeDateTime()
                                            .plus(schedulingProducingArrangement.getProducingDuration())
                            )
                    )
            );

    @Serial
    private static final long serialVersionUID = -2467531974779697853L;

    @EqualsAndHashCode.Include
    @ToString.Include
    private String id = "test";

//    private Map<SchedulingProduct, Integer> productAmountMap;

    private ProductAmountBill productAmountMap;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime sleepStart = DEFAULT_SLEEP_START;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime sleepEnd = DEFAULT_SLEEP_END;

    @DeepPlanningClone
    private NavigableSet<SchedulingProducingArrangement> schedulingProducingArrangements;

    @ToString.Include
    @DeepPlanningClone
    @ShadowVariable(supplierName = "supplierForShadowComputedMap")
    private Map<SchedulingProducingArrangement, ComputedDateTimePair> shadowComputedMap = new LinkedHashMap<>();

    @ShadowSources(
            value = {
                    "schedulingProducingArrangements[].planningDateTimeSlot",
                    "schedulingProducingArrangements[].planningFactoryInstance"
            }
    )
    public Map<SchedulingProducingArrangement, ComputedDateTimePair> supplierForShadowComputedMap() {

        return this.schedulingProducingArrangements.stream()
                .filter(SchedulingProducingArrangement::boolPlanningAssigned)
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

    private Collector<SchedulingProducingArrangement, ?, Map<FactoryReadableIdentifier, Map<SchedulingProducingArrangement, ComputedDateTimePair>>> buildSinglePassCollector(
            Predicate<SchedulingProducingArrangement> factoryTypePredicate,
            Function<Stream<SchedulingProducingArrangement>, Stream<Pair<SchedulingProducingArrangement, ComputedDateTimePair>>> processSequenceToComputedPairFunction
    ) {

        return Collectors.filtering(
                factoryTypePredicate,
                Collectors.groupingBy(
                        SchedulingProducingArrangement::getPlanningFactoryInstanceReadableIdentifier,
                        LinkedHashMap::new,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                schedulingProducingArrangements -> {
                                    schedulingProducingArrangements.sort(ARRANGEMENT_COMPARATOR);

                                    return processSequenceToComputedPairFunction.apply(
                                            schedulingProducingArrangements.stream()
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

    private Map<SchedulingProducingArrangement, ComputedDateTimePair> mergeFinalResults(
            Map<FactoryReadableIdentifier, Map<SchedulingProducingArrangement, ComputedDateTimePair>> slotMap,
            Map<FactoryReadableIdentifier, Map<SchedulingProducingArrangement, ComputedDateTimePair>> queueMap
    ) {

        Map<SchedulingProducingArrangement, ComputedDateTimePair> result = new LinkedHashMap<>();
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
        if (Objects.isNull(schedulingProducingArrangement)) {
            return null;
        }

        return this.shadowComputedMap.get(schedulingProducingArrangement);
    }

    public LocalDateTime queryCompletedDateTime(SchedulingProducingArrangement schedulingProducingArrangement) {
        ComputedDateTimePair computedDateTimePair = query(schedulingProducingArrangement);
        if (computedDateTimePair == null) {
            return null;
        }
        return computedDateTimePair.completedDateTime();
    }

}
