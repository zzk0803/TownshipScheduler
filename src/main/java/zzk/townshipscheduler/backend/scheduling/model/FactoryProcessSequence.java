package zzk.townshipscheduler.backend.scheduling.model;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.function.Function;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Value
public class FactoryProcessSequence
        implements Comparable<FactoryProcessSequence>, Serializable {

    public static final Function<SchedulingProducingArrangement, Integer> DEFAULT_SEQUENTIAL_ID_FUNCTION =
            SchedulingProducingArrangement::getId;

    public static final Comparator<FactoryProcessSequence> COMPARATOR =
            Comparator.comparing(FactoryProcessSequence::getSchedulingFactoryInstanceReadableIdentifier)
                    .thenComparing(FactoryProcessSequence::getArrangeDateTime)
                    .thenComparing(FactoryProcessSequence::getSequentialId)
                    .thenComparing(FactoryProcessSequence::getArrangementId);

    @Serial
    private static final long serialVersionUID = -2296858586337992130L;

    @EqualsAndHashCode.Include
    LocalDateTime arrangeDateTime;

    @EqualsAndHashCode.Include
    Integer arrangementId;

    @EqualsAndHashCode.Include
    Integer sequentialId;

    @EqualsAndHashCode.Include
    FactoryReadableIdentifier schedulingFactoryInstanceReadableIdentifier;

    Duration producingDuration;

    public FactoryProcessSequence(
            SchedulingProducingArrangement schedulingProducingArrangement
    ) {
        this(
                schedulingProducingArrangement,
                DEFAULT_SEQUENTIAL_ID_FUNCTION
        );
    }

    public FactoryProcessSequence(
            SchedulingProducingArrangement schedulingProducingArrangement,
            Function<SchedulingProducingArrangement, Integer> sequentialIdFunction
    ) {
        this.arrangementId = schedulingProducingArrangement.getId();
        this.arrangeDateTime = schedulingProducingArrangement.getArrangeDateTime();
        this.schedulingFactoryInstanceReadableIdentifier = schedulingProducingArrangement.getPlanningFactoryInstance()
                .getFactoryReadableIdentifier();
        this.producingDuration = schedulingProducingArrangement.getProducingDuration();
        this.sequentialId = sequentialIdFunction.apply(schedulingProducingArrangement);
    }

    @Override
    public int compareTo(FactoryProcessSequence that) {
        return COMPARATOR.compare(
                this,
                that
        );
    }

}
