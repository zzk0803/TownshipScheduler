package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.TreeMap;

@Data
@PlanningEntity
@NoArgsConstructor
@AllArgsConstructor
public class SchedulingArrangementsGlobalState {

    private List<SchedulingProducingArrangement> schedulingProducingArrangements;

    @ShadowVariable(supplierName = "supplierNameFactorySlotToFinishedLocalDateTimeMap")
    private TreeMap<SchedulingFactoryInstanceDateTimeSlot, LocalDateTime> factorySlotToFinishedLocalDateTimeMap
            = new TreeMap<>();

    public SchedulingArrangementsGlobalState(List<SchedulingProducingArrangement> schedulingProducingArrangements) {
        this.schedulingProducingArrangements = schedulingProducingArrangements;
    }

    @ShadowSources(
            value = {
                    "schedulingProducingArrangements[].nextProducingArrangement",
                    "schedulingProducingArrangements[].completedDateTime"
            },
            alignmentKey = "schedulingProducingArrangements"
    )
    private TreeMap<SchedulingFactoryInstanceDateTimeSlot, LocalDateTime> supplierNameFactorySlotToFinishedLocalDateTimeMap() {
        return this.schedulingProducingArrangements.stream()
                .filter(schedulingProducingArrangement -> schedulingProducingArrangement.isPlanningAssigned()
                        && schedulingProducingArrangement.getNextProducingArrangement() == null
                )
                .collect(
                        TreeMap::new,
                        (treeMap, schedulingProducingArrangement) -> {
                            treeMap.put(
                                    schedulingProducingArrangement.getPlanningFactoryDateTimeSlot(),
                                    schedulingProducingArrangement.getCompletedDateTime()
                            );
                        },
                        TreeMap::putAll
                );
    }

}
