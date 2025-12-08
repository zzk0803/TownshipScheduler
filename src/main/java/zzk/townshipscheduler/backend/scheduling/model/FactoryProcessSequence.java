package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.Duration;
import java.util.Comparator;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Data
public class FactoryProcessSequence implements Comparable<FactoryProcessSequence> {

    public static final Comparator<FactoryProcessSequence> COMPARATOR
            = Comparator.comparing(FactoryProcessSequence::getPlanningDateTimeSlot)
            .thenComparingInt(FactoryProcessSequence::getArrangementId);

    @PlanningId
    @EqualsAndHashCode.Include
    private String id;

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

    public FactoryProcessSequence(
            SchedulingProducingArrangement schedulingProducingArrangement,
            SchedulingFactoryInstance schedulingFactoryInstance,
            SchedulingDateTimeSlot schedulingDateTimeSlot
    ) {
        this.planningDateTimeSlot = schedulingDateTimeSlot;
        this.producingDuration = schedulingProducingArrangement.getProducingDuration();
        this.arrangementId = schedulingProducingArrangement.getId();
        this.schedulingFactoryInstanceReadableIdentifier = schedulingFactoryInstance.getFactoryReadableIdentifier();
        setupId();
    }

    private void setupId() {
        String fpsId = getArrangementId() + getSchedulingFactoryInstanceReadableIdentifier().toString() + getPlanningDateTimeSlot().toString();
        setId(fpsId);
    }

    public FactoryProcessSequence(SchedulingProducingArrangement schedulingProducingArrangement) {
        this.planningDateTimeSlot = schedulingProducingArrangement.getPlanningDateTimeSlot();
        this.producingDuration = schedulingProducingArrangement.getProducingDuration();
        this.arrangementId = schedulingProducingArrangement.getId();
        this.schedulingFactoryInstanceReadableIdentifier = schedulingProducingArrangement.getPlanningFactoryInstance()
                .getFactoryReadableIdentifier();
        setupId();
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
