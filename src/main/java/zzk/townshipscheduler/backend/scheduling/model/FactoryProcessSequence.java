package zzk.townshipscheduler.backend.scheduling.model;

import lombok.EqualsAndHashCode;
import lombok.Value;
import zzk.townshipscheduler.backend.ProducingStructureType;

import java.time.Duration;
import java.time.LocalDateTime;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Value
public class FactoryProcessSequence {

    @EqualsAndHashCode.Include
    LocalDateTime arrangeDateTime;

    @EqualsAndHashCode.Include
    Integer arrangementId;

    String factoryInstanceId;

    boolean sequenced;

    Duration producingDuration;

    int slotGapDuration;

    public FactoryProcessSequence(SchedulingProducingArrangement schedulingProducingArrangement) {
        SchedulingDateTimeSlot planningDateTimeSlot = schedulingProducingArrangement.getPlanningDateTimeSlot();
        this.arrangeDateTime = planningDateTimeSlot.getStart();
        this.producingDuration = schedulingProducingArrangement.getProducingDuration();
        this.slotGapDuration = planningDateTimeSlot.getDurationInMinute();
        this.arrangementId = schedulingProducingArrangement.getId();
        this.factoryInstanceId = schedulingProducingArrangement.getPlanningFactoryInstance().getReadableIdentifier();
        this.sequenced = schedulingProducingArrangement.getPlanningFactoryInstance()
                                 .getSchedulingFactoryInfo()
                                 .getProducingStructureType() == ProducingStructureType.QUEUE;
    }

    public FactoryProcessSequence(
            SchedulingProducingArrangement schedulingProducingArrangement,
            SchedulingDateTimeSlot schedulingDateTimeSlot
    ) {
        this.arrangeDateTime = schedulingDateTimeSlot.getStart();
        this.slotGapDuration = schedulingDateTimeSlot.getDurationInMinute();
        this.arrangementId = schedulingProducingArrangement.getId();
        this.producingDuration = schedulingProducingArrangement.getProducingDuration();
        this.factoryInstanceId = schedulingProducingArrangement.getPlanningFactoryInstance().getReadableIdentifier();
        this.sequenced = schedulingProducingArrangement.getPlanningFactoryInstance()
                                 .getSchedulingFactoryInfo()
                                 .getProducingStructureType() == ProducingStructureType.QUEUE;
    }

}
