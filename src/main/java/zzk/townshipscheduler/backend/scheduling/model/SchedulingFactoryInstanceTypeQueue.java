package zzk.townshipscheduler.backend.scheduling.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@JsonIgnoreType
public class SchedulingFactoryInstanceTypeQueue
        extends BaseSchedulingFactoryInstance
        implements ISchedulingFactoryOrFactoryArrangement {

    public static final String SHADOW_ACTION_CONSEQUENCES = "shadowActionConsequences";

    @JsonIgnore
    private SchedulingProducingArrangementFactoryTypeQueue nextQueueProducingArrangement;

    @JsonIgnore
    @Override
    public LocalDateTime getCompletedDateTime() {
        var producingArrangement = this.getNextQueueProducingArrangement();
        if (producingArrangement == null) {
            return null;
        }
        while (producingArrangement.getNextQueueProducingArrangement() != null) {
            producingArrangement = producingArrangement.getNextQueueProducingArrangement();
        }
        return producingArrangement.getCompletedDateTime();
    }

    @JsonIgnore
    @Override
    public SchedulingFactoryInfo getFactoryInfo() {
        return super.getSchedulingFactoryInfo();
    }

    @Override
    public List<ArrangeConsequence> useFilteredArrangeConsequences() {
        return getForwardArrangements().stream()
                .map(BaseSchedulingProducingArrangement::calcConsequence)
                .flatMap(Collection::stream)
                .filter(consequence -> consequence.getResource().getRoot() == this)
                .filter(consequence -> consequence.getResource() instanceof ArrangeConsequence.FactoryProducingLength)
                .sorted()
                .toList();
    }

}
