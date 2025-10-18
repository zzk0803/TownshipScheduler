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

    public FactoryProcessSequence(SchedulingProducingArrangement schedulingProducingArrangement) {
        this.arrangeDateTime = schedulingProducingArrangement.getArrangeDateTime();
        this.producingDuration = schedulingProducingArrangement.getProducingDuration();
        this.arrangementId = schedulingProducingArrangement.getId();
        this.schedulingFactoryInstanceReadableIdentifier
                = schedulingProducingArrangement.getPlanningFactoryInstance().getFactoryReadableIdentifier();
    }

    @Override
    public int compareTo(FactoryProcessSequence that) {
        return COMPARATOR.compare(this, that);
    }

}
