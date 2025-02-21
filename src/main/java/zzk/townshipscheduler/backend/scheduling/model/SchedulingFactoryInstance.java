package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import zzk.townshipscheduler.backend.ProducingStructureType;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
//@PlanningEntity
public class SchedulingFactoryInstance {

    @PlanningId
    @EqualsAndHashCode.Include
    private Integer id;

    private SchedulingFactoryInfo schedulingFactoryInfo;

    private int seqNum;

    private int producingLength;

    private int reapWindowSize;

    private List<SchedulingFactoryTimeSlotInstance> schedulingFactoryTimeSlotInstances = new ArrayList<>();

    public void addPeriodFactory(SchedulingFactoryTimeSlotInstance schedulingFactoryTimeSlotInstance) {
        schedulingFactoryTimeSlotInstances.add(schedulingFactoryTimeSlotInstance);
    }

    public ProducingStructureType getProducingStructureType() {
        return schedulingFactoryInfo.getProducingStructureType();
    }

//    public int availableProducingQueueSizeWhen(LocalDateTime dateTime) {
//        int availableSlots = getProducingLength();
//        SchedulingPlayerFactoryAction action = getPlanningNext();
//        while (action != null) {
//            LocalDateTime actionCompletedDateTime = action.getShadowGameCompleteDateTime();
//            if (actionCompletedDateTime == null || actionCompletedDateTime.isAfter(dateTime)) {
//                availableSlots--;
//            }
//            action = action.getPlanningNext();
//        }
//
//        if (availableSlots < 0) {
//            availableSlots = 0; // 队列已满，无法接纳更多任务
//        }
//
//        return availableSlots;
//    }

    @Override
    public String toString() {
        return this.schedulingFactoryInfo.getCategoryName() + "#" + this.getSeqNum() + ",size=" + this.getProducingLength();
    }

//    @Override
//    public Duration nextAvailableAsDuration(LocalDateTime dateTime) {
//        return super.nextAvailableAsDuration(dateTime);
//    }

}
