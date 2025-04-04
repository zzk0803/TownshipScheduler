package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.javatuples.Pair;

import java.time.Duration;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class BaseSchedulingFactoryInstance {

    @PlanningId
    @EqualsAndHashCode.Include
    protected Integer id;

    protected SchedulingFactoryInfo schedulingFactoryInfo;

    protected int seqNum;

    protected int producingLength;

    protected int reapWindowSize;

    @Override
    public String toString() {
        return this.schedulingFactoryInfo.getCategoryName() + "#" + this.getSeqNum() + ",size=" + this.getProducingLength();
    }

    public abstract Pair<Integer, Duration> remainProducingCapacityAndNextAvailable(SchedulingDateTimeSlot schedulingDateTimeSlot);
}
