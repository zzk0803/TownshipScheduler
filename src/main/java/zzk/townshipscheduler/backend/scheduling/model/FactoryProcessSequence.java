package zzk.townshipscheduler.backend.scheduling.model;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Objects;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Value
public class FactoryProcessSequence implements Comparable<FactoryProcessSequence> {

    public static final Comparator<FactoryProcessSequence> COMPARATOR
            = Comparator.comparing(FactoryProcessSequence::getArrangeDateTime)
            .thenComparing(FactoryProcessSequence::getFactoryReadableIdentifier)
            .thenComparingInt(FactoryProcessSequence::getArrangementId);

    @ToString.Include
    @EqualsAndHashCode.Include
    LocalDateTime arrangeDateTime;

    @ToString.Include
    @EqualsAndHashCode.Include
    Integer arrangementId;

    @ToString.Include
    @EqualsAndHashCode.Include
    FactoryReadableIdentifier factoryReadableIdentifier;

    @ToString.Include
    Duration producingDuration;

    public FactoryProcessSequence(SchedulingProducingArrangement schedulingProducingArrangement) {
        this.arrangementId = schedulingProducingArrangement.getId();

        SchedulingDateTimeSlot planningDateTimeSlot = schedulingProducingArrangement.getPlanningDateTimeSlot();
        this.arrangeDateTime = Objects.nonNull(planningDateTimeSlot) ? planningDateTimeSlot.getStart() : null;
        SchedulingFactoryInstance schedulingFactoryInstance = schedulingProducingArrangement.getPlanningFactoryInstance();
        this.factoryReadableIdentifier = Objects.nonNull(schedulingFactoryInstance)
                ? schedulingFactoryInstance.getFactoryReadableIdentifier()
                : null;

        this.producingDuration = schedulingProducingArrangement.getProducingDuration();
    }

    public static FactoryProcessSequence of(SchedulingProducingArrangement schedulingProducingArrangement) {
        return new FactoryProcessSequence(schedulingProducingArrangement);
    }

    @Override
    public int compareTo(FactoryProcessSequence that) {
        return COMPARATOR.compare(this, that);
    }

}
