package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import lombok.*;
import zzk.townshipscheduler.backend.ProducingStructureType;

import java.time.Duration;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true,onlyExplicitlyIncluded = true)
@Data
@NoArgsConstructor
//@PlanningEntity
public class SchedulingFactoryInstance extends BasePlanningChainSupportFactoryOrAction {

    @EqualsAndHashCode.Include
    @PlanningId
    private String instanceId;

    private SchedulingFactoryInfo schedulingFactoryInfo;

    private int seqNum;

    private int producingLength;

    private int reapWindowSize;

    public ProducingStructureType getProducingStructureType() {
        return schedulingFactoryInfo.getProducingStructureType();
    }

    public void setSchedulingFactoryInfo(SchedulingFactoryInfo schedulingFactoryInfo) {
        this.schedulingFactoryInfo = schedulingFactoryInfo;
        setupFactoryInstanceId();
    }

    private void setupFactoryInstanceId() {
        this.instanceId = getSchedulingFactoryInfo().getCategoryName() + "#" + getSeqNum();
    }

    public int availableProducingQueueSizeWhen(LocalDateTime dateTime) {
        int availableSlots = getProducingLength();
        SchedulingPlayerFactoryAction action = getPlanningNext();
        while (action != null) {
            LocalDateTime actionCompletedDateTime = action.getShadowGameCompleteDateTime();
            if (actionCompletedDateTime == null || actionCompletedDateTime.isAfter(dateTime)) {
                availableSlots--;
            }
            action = action.getPlanningNext();
        }

        if (availableSlots < 0) {
            availableSlots = 0; // 队列已满，无法接纳更多任务
        }

        return availableSlots;
    }

    @Override
    public Duration nextAvailableAsDuration(LocalDateTime dateTime) {
        return super.nextAvailableAsDuration(dateTime);
    }

    @Override
    public String toString() {
        return this.schedulingFactoryInfo.getCategoryName() + "#" + this.getSeqNum() + ",size=" + this.getProducingLength();
    }

}
