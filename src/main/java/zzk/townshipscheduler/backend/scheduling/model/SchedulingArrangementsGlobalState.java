package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.solution.cloner.DeepPlanningClone;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.javatuples.Pair;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Gatherer;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@PlanningEntity
public class SchedulingArrangementsGlobalState {

    private static final Gatherer<FactoryProcessSequence, FormerCompletedDateTimeRef, Pair<FactoryProcessSequence, FactoryComputedDateTimePair>>
            QUEUE_GATHERER
            = Gatherer.ofSequential(
            FormerCompletedDateTimeRef::new,
            (formerCompletedDateTimeRef, factoryProcessSequence, downstream) -> {
                LocalDateTime arrangeDateTime = factoryProcessSequence.getArrangeDateTime();
                LocalDateTime start = (formerCompletedDateTimeRef.value == null)
                        ? arrangeDateTime
                        : formerCompletedDateTimeRef.value.isAfter(arrangeDateTime)
                                ? formerCompletedDateTimeRef.value
                                : arrangeDateTime;
                LocalDateTime end = start.plus(factoryProcessSequence.getProducingDuration());
                return downstream.push(
                        new Pair<>(
                                factoryProcessSequence,
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
    private String id = "SchedulingArrangementsGlobalState";

    @DeepPlanningClone
    private List<SchedulingProducingArrangement> schedulingProducingArrangements;

    @ToString.Include
    @ShadowVariable(supplierName = "supplierForMap")
    @DeepPlanningClone
    private Map<SchedulingFactoryInstance, Map<UUID, FactoryComputedDateTimePair>> map = new LinkedHashMap<>();

    @ShadowSources(
            value = {
                    "schedulingProducingArrangements[].planningFactoryInstance",
                    "schedulingProducingArrangements[].planningDateTimeSlot"
            }
    )
    public Map<SchedulingFactoryInstance, Map<FactoryProcessSequence, FactoryComputedDateTimePair>> supplierForMap() {
        return this.schedulingProducingArrangements.stream()
                .filter(SchedulingProducingArrangement::weatherFactoryProducingTypeIsQueue)
                .filter(SchedulingProducingArrangement::isPlanningAssigned)
                .collect(
                        Collectors.groupingBy(
                                SchedulingProducingArrangement::getPlanningFactoryInstance,
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        producingArrangements -> producingArrangements.stream()
                                                .map(SchedulingProducingArrangement::toFactoryProcessSequence)
                                                .sorted(FactoryProcessSequence.COMPARATOR)
                                                .gather(QUEUE_GATHERER)
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

    public FactoryComputedDateTimePair query(SchedulingProducingArrangement schedulingProducingArrangement) {
        if (schedulingProducingArrangement.getPlanningDateTimeSlot() == null || schedulingProducingArrangement.getPlanningFactoryInstance() == null) {
            return null;
        }

        if (schedulingProducingArrangement.weatherFactoryProducingTypeIsQueue()) {
            Map<UUID, FactoryComputedDateTimePair> uuidFactoryComputedDateTimePairMap =
                    map.get(schedulingProducingArrangement.getPlanningFactoryInstance());
            if (uuidFactoryComputedDateTimePairMap == null) {
                return null;
            }
            return uuidFactoryComputedDateTimePairMap.get(schedulingProducingArrangement.getUuid());
        } else {
            LocalDateTime producingDateTime = schedulingProducingArrangement.getPlanningDateTimeSlot().getStart();
            Duration producingDuration = schedulingProducingArrangement.getProducingDuration();
            return new FactoryComputedDateTimePair(
                    producingDateTime,
                    producingDateTime.plus(producingDuration)
            );
        }
    }

    private static final class FormerCompletedDateTimeRef {

        public LocalDateTime value = null;

    }

}
