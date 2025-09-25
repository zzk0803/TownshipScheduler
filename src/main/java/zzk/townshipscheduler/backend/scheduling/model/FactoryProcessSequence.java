package zzk.townshipscheduler.backend.scheduling.model;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Value
public class FactoryProcessSequence implements Comparable<FactoryProcessSequence> {

    public static final Comparator<FactoryProcessSequence> COMPARATOR
            = Comparator.comparing(FactoryProcessSequence::getArrangeDateTime)
            .thenComparingInt(FactoryProcessSequence::getArrangementId);

    @EqualsAndHashCode.Include
    LocalDateTime arrangeDateTime;

    @EqualsAndHashCode.Include
    Integer arrangementId;

    @EqualsAndHashCode.Include
    FactoryReadableIdentifier schedulingFactoryInstanceReadableIdentifier;

    Duration producingDuration;

    int slotGapDuration;

    public static FactoryProcessSequence of(SchedulingProducingArrangement schedulingProducingArrangement) {
        return new FactoryProcessSequence(schedulingProducingArrangement);
    }

    private FactoryProcessSequence(SchedulingProducingArrangement schedulingProducingArrangement) {
        SchedulingDateTimeSlot planningDateTimeSlot = schedulingProducingArrangement.getPlanningDateTimeSlot();
        this.arrangeDateTime = planningDateTimeSlot.getStart();
        this.producingDuration = schedulingProducingArrangement.getProducingDuration();
        this.slotGapDuration = planningDateTimeSlot.getDurationInMinute();
        this.arrangementId = schedulingProducingArrangement.getId();
        this.schedulingFactoryInstanceReadableIdentifier
                = schedulingProducingArrangement.getPlanningFactoryInstance().getFactoryReadableIdentifier();
    }

    @Override
    public int compareTo(FactoryProcessSequence that) {
        return COMPARATOR.compare(this, that);
    }

}
