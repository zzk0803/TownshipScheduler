package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.javatuples.Pair;

import java.time.Duration;
import java.util.List;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonSubTypes(
        value = {
                @JsonSubTypes.Type(SchedulingFactoryInstanceTypeQueue.class),
                @JsonSubTypes.Type(SchedulingFactoryInstanceTypeSlot.class)
        }
)
public abstract class BaseSchedulingFactoryInstance {

    @PlanningId
    @EqualsAndHashCode.Include
    protected Integer id;

    @JsonIgnore
    protected SchedulingFactoryInfo schedulingFactoryInfo;

    protected int seqNum;

    protected int producingLength;

    protected int reapWindowSize;

    @Override
    public String toString() {
        return this.schedulingFactoryInfo.getCategoryName() + "#" + this.getSeqNum() + ",size=" + this.getProducingLength();
    }

    public String getReadableIdentifier() {
        return this.schedulingFactoryInfo.getCategoryName() + "#" + this.getSeqNum();
    }

    public Pair<Integer, List<ArrangeConsequence>> remainProducingCapacityAndRecoveryArrangeConsequence(SchedulingDateTimeSlot schedulingDateTimeSlot) {
        var filteredArrangeConsequences = useFilteredArrangeConsequences();
        var recoveryConsequences = filteredArrangeConsequences.stream()
                .filter(arrangeConsequence -> arrangeConsequence.getLocalDateTime()
                                                      .isAfter(schedulingDateTimeSlot.getStart()) || arrangeConsequence.getLocalDateTime()
                                                      .isEqual(schedulingDateTimeSlot.getStart()))
                .filter(arrangeConsequence -> arrangeConsequence.getResourceChange() instanceof ArrangeConsequence.Increase)
                .toList();

        int remain = filteredArrangeConsequences.stream()
                .filter(arrangeConsequence -> arrangeConsequence.getLocalDateTime()
                                                      .isBefore(schedulingDateTimeSlot.getStart())
                                              || arrangeConsequence.getLocalDateTime()
                                                      .isEqual(schedulingDateTimeSlot.getStart()))
                .reduce(
                        getProducingLength(),
                        (integer, arrangeConsequence) -> arrangeConsequence.getResourceChange().apply(integer),
                        Integer::sum
                );

        return Pair.with(remain, recoveryConsequences);

    }

    public abstract List<ArrangeConsequence> useFilteredArrangeConsequences();

    @JsonProperty
    public String getCategoryName() {
        return schedulingFactoryInfo.getCategoryName();
    }

    public boolean typeEqual(BaseSchedulingFactoryInstance that) {
        return this.getSchedulingFactoryInfo().typeEqual(that.getSchedulingFactoryInfo());
    }

}
