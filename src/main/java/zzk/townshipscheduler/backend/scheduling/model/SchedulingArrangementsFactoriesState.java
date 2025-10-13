package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.cloner.DeepPlanningClone;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.javatuples.Pair;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Gatherer;

@Slf4j
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@PlanningEntity
public class SchedulingArrangementsFactoriesState {

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

    @EqualsAndHashCode.Include
    private SchedulingFactoryInfo schedulingFactoryInfo;

    @DeepPlanningClone
    private List<SchedulingProducingArrangement> schedulingProducingArrangements;

    @ToString.Include
    @ShadowVariable(supplierName = "supplierForMap")
    private Map<FactoryReadableIdentifier, Map<FactoryProcessSequence, FactoryComputedDateTimePair>> map = new LinkedHashMap<>();

    @ShadowSources(
            value = {
                    "schedulingProducingArrangements[].factoryProcessSequence"
            },
            alignmentKey = "schedulingProducingArrangements"
    )
    public Map<FactoryReadableIdentifier, Map<FactoryProcessSequence, FactoryComputedDateTimePair>> supplierForMap() {
        return this.schedulingProducingArrangements.stream()
                .filter(SchedulingProducingArrangement::isPlanningAssigned)
                .collect(
                        Collectors.groupingBy(
                                schedulingProducingArrangement -> schedulingProducingArrangement.getPlanningFactoryInstance()
                                        .getFactoryReadableIdentifier(),
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        producingArrangements -> producingArrangements.stream()
                                                .map(SchedulingProducingArrangement::getFactoryProcessSequence)
                                                .sorted(FactoryProcessSequence.COMPARATOR)
                                                .gather(getSchedulingFactoryInfo().weatherFactoryProducingTypeIsQueue()
                                                        ? QUEUE_GATHERER
                                                        : SLOT_GATHERER)
                                                .collect(
                                                        LinkedHashMap::new,
                                                        (treeMap, pair) -> treeMap.put(
                                                                pair.getValue0(),
                                                                pair.getValue1()
                                                        ),
                                                        LinkedHashMap::putAll
                                                )
                                )
                        )
                );
    }

    public FactoryComputedDateTimePair query(FactoryProcessSequence factoryProcessSequence) {
        return this.map.get(factoryProcessSequence.getSchedulingFactoryInstanceReadableIdentifier())
                .get(factoryProcessSequence);
    }

    public static class FormerCompletedDateTimeRef {

        public LocalDateTime value = null;

    }

}
