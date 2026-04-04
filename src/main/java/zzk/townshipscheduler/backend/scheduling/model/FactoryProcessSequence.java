package zzk.townshipscheduler.backend.scheduling.model;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Objects;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Value
public class FactoryProcessSequence implements Comparable<FactoryProcessSequence>, Serializable {

    public static final Comparator<FactoryProcessSequence> COMPARATOR
            = Comparator.comparing(FactoryProcessSequence::getArrangeDateTime)
            .thenComparingInt(FactoryProcessSequence::getSequenceId);

    @Serial
    private static final long serialVersionUID = -264984659974196003L;

    LocalDateTime arrangeDateTime;

    Integer sequenceId;

    @EqualsAndHashCode.Include
    Integer arrangeId;

    @EqualsAndHashCode.Include
    FactoryReadableIdentifier schedulingFactoryInstanceReadableIdentifier;

    Duration producingDuration;

    public FactoryProcessSequence(SchedulingProducingArrangement schedulingProducingArrangement) {
        this.arrangeDateTime = schedulingProducingArrangement.getArrangeDateTime();
        this.producingDuration = schedulingProducingArrangement.getProducingDuration();
        this.arrangeId = schedulingProducingArrangement.getId();
        this.sequenceId = schedulingProducingArrangement.getIndexInFactory();
        this.schedulingFactoryInstanceReadableIdentifier
                = schedulingProducingArrangement.getPlanningFactoryInstance()
                .getFactoryReadableIdentifier();
    }

    @Override
    public int compareTo(FactoryProcessSequence that) {
        return COMPARATOR.compare(this, that);
    }

}
