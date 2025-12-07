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
            = Comparator.comparing(FactoryProcessSequence::getPlanningDateTimeSlot)
            .thenComparingInt(FactoryProcessSequence::getArrangementId);

    @ToString.Include
    @EqualsAndHashCode.Include
    private Integer arrangementId;

    @ToString.Include
    @EqualsAndHashCode.Include
    private FactoryReadableIdentifier schedulingFactoryInstanceReadableIdentifier;

    @ToString.Include
    @EqualsAndHashCode.Include
    private Duration producingDuration;

    @ToString.Include
    @EqualsAndHashCode.Include
    private SchedulingDateTimeSlot planningDateTimeSlot;

    public FactoryProcessSequence(SchedulingProducingArrangement schedulingProducingArrangement) {
        this.planningDateTimeSlot = schedulingProducingArrangement.getPlanningDateTimeSlot();
        this.producingDuration = schedulingProducingArrangement.getProducingDuration();
        this.arrangementId = schedulingProducingArrangement.getId();
        this.schedulingFactoryInstanceReadableIdentifier = schedulingProducingArrangement.getPlanningFactoryInstance()
                .getFactoryReadableIdentifier();
    }

    public static FactoryProcessSequence of(SchedulingProducingArrangement schedulingProducingArrangement) {
        return new FactoryProcessSequence(schedulingProducingArrangement);
    }

    @Override
    public int compareTo(FactoryProcessSequence that) {
        return COMPARATOR.compare(
                this,
                that
        );
    }

}
