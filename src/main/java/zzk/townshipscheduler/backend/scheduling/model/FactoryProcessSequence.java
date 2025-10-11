package zzk.townshipscheduler.backend.scheduling.model;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Value
public class FactoryProcessSequence implements Comparable<FactoryProcessSequence> {

    public static final Comparator<FactoryProcessSequence> COMPARATOR
            = Comparator.comparing(FactoryProcessSequence::getArrangeDateTime)
            .thenComparingInt(FactoryProcessSequence::getArrangementId);

    @ToString.Include
    @EqualsAndHashCode.Include
    LocalDateTime arrangeDateTime;

    @ToString.Include
    @EqualsAndHashCode.Include
    Integer arrangementId;

    @ToString.Include
    @EqualsAndHashCode.Include
    FactoryReadableIdentifier schedulingFactoryInstanceReadableIdentifier;

    @ToString.Include
    Duration producingDuration;

    int slotGapDuration;

    public FactoryProcessSequence(SchedulingProducingArrangement schedulingProducingArrangement) {
        SchedulingDateTimeSlot planningDateTimeSlot = schedulingProducingArrangement.getPlanningDateTimeSlot();
        this.arrangeDateTime = planningDateTimeSlot.getStart();
        this.producingDuration = schedulingProducingArrangement.getProducingDuration();
        this.slotGapDuration = planningDateTimeSlot.getDurationInMinute();
        this.arrangementId = schedulingProducingArrangement.getId();
        this.schedulingFactoryInstanceReadableIdentifier = schedulingProducingArrangement.getPlanningFactoryInstance().getFactoryReadableIdentifier();
    }

    public static FactoryProcessSequence of(SchedulingProducingArrangement schedulingProducingArrangement) {
        return new FactoryProcessSequence(schedulingProducingArrangement);
    }

    @Override
    public int compareTo(FactoryProcessSequence that) {
        return COMPARATOR.compare(this, that);
    }

}
