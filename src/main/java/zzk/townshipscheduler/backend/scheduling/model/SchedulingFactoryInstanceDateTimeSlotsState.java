package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@PlanningEntity
public class SchedulingFactoryInstanceDateTimeSlotsState {

    @PlanningId
    @EqualsAndHashCode.Include
    private String id = "SchedulingFactoryInstanceDateTimeSlotsState";

    private List<SchedulingFactoryInstanceDateTimeSlot> schedulingFactoryInstanceDateTimeSlotList = new ArrayList<>();

    @ShadowVariable(supplierName = "slotToLastCompletedMapSupplier")
    private LinkedHashMap<SchedulingFactoryInstance, TreeMap<SchedulingFactoryInstanceDateTimeSlot, LocalDateTime>> slotToLastCompletedMap
            = new LinkedHashMap<>();

    @ShadowSources({"schedulingFactoryInstanceDateTimeSlotList[].tailArrangementCompletedDateTime"})
    private LinkedHashMap<SchedulingFactoryInstance, TreeMap<SchedulingFactoryInstanceDateTimeSlot, LocalDateTime>> slotToLastCompletedMapSupplier() {
        return this.schedulingFactoryInstanceDateTimeSlotList.stream()
                .collect(
                        Collectors.groupingBy(
                                SchedulingFactoryInstanceDateTimeSlot::getFactoryInstance,
                                LinkedHashMap::new,
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        list -> list.stream()
                                                .collect(
                                                        TreeMap::new,
                                                        (treeMap, factoryInstanceDateTimeSlot) -> {
                                                            treeMap.put(
                                                                    factoryInstanceDateTimeSlot,
                                                                    factoryInstanceDateTimeSlot.getTailArrangementCompletedDateTime()
                                                            );
                                                        },
                                                        TreeMap::putAll
                                                )
                                )
                        )
                );
    }

    public LocalDateTime queryFirstProducingDateTimeArrangement(SchedulingFactoryInstanceDateTimeSlot schedulingFactoryInstanceDateTimeSlot) {
        SchedulingFactoryInstance factoryInstance = schedulingFactoryInstanceDateTimeSlot.getFactoryInstance();
        if (factoryInstance.weatherFactoryProducingTypeIsSlot()) {
            return schedulingFactoryInstanceDateTimeSlot.getStart();
        }

        TreeMap<SchedulingFactoryInstanceDateTimeSlot, LocalDateTime> instanceDateTimeSlotLocalDateTimeTreeMap
                = this.slotToLastCompletedMap.get(factoryInstance);
        if (instanceDateTimeSlotLocalDateTimeTreeMap == null) {
            return schedulingFactoryInstanceDateTimeSlot.getStart();
        }

        return instanceDateTimeSlotLocalDateTimeTreeMap
                .headMap(
                        schedulingFactoryInstanceDateTimeSlot,
                        false
                )
                .entrySet()
                .stream()
                .filter((entry) -> entry.getValue()
                        .isAfter(schedulingFactoryInstanceDateTimeSlot.getStart()))
                .max(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .orElse(schedulingFactoryInstanceDateTimeSlot.getStart());
    }

}
