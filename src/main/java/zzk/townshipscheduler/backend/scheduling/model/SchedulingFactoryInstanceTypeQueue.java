package zzk.townshipscheduler.backend.scheduling.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

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

    @JsonIgnore
    public List<SchedulingProducingArrangementFactoryTypeQueue> getFlattenProducingArrangements() {
        SchedulingProducingArrangementFactoryTypeQueue firstArrangement
                = this.getNextQueueProducingArrangement();

        if (Objects.isNull(firstArrangement)) {
            return List.of();
        }

        return Stream.iterate(
                firstArrangement,
                (arrangement) -> arrangement.getNextQueueProducingArrangement() != null,
                SchedulingProducingArrangementFactoryTypeQueue::getNextQueueProducingArrangement
        ).toList();
    }

    @Override
    public List<ArrangeConsequence> useFilteredArrangeConsequences() {
        List<ArrangeConsequence> arrangeConsequences = new ArrayList<>();
        SchedulingProducingArrangementFactoryTypeQueue queueProducingArrangement
                = this.nextQueueProducingArrangement;
        while (queueProducingArrangement != null) {
            arrangeConsequences.addAll(queueProducingArrangement.calcConsequence());
            queueProducingArrangement = queueProducingArrangement.getNextQueueProducingArrangement();
        }

        return arrangeConsequences.stream()
                .filter(consequence -> consequence.getResource().getRoot() == this)
                .filter(consequence -> consequence.getResource() instanceof ArrangeConsequence.FactoryProducingLength)
                .sorted()
                .toList();
    }


}
