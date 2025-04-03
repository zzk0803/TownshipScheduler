package zzk.townshipscheduler.backend.scheduling.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.javatuples.Pair;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class SchedulingTypeQueueFactoryInstance
        extends BaseSchedulingFactoryInstance
        implements ISchedulingFactoryOrFactoryArrangement {

    public static final String SHADOW_ACTION_CONSEQUENCES = "shadowActionConsequences";

    private SchedulingFactoryQueueProducingArrangement nextQueueProducingArrangement;

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

    @Override
    public SchedulingFactoryInfo getFactoryInfo() {
        return super.getSchedulingFactoryInfo();
    }

    @Override
    public Pair<Integer, Duration> remainProducingCapacityAndNextAvailable(SchedulingDateTimeSlot schedulingDateTimeSlot) {
        List<ArrangeConsequence> arrangeConsequences = new ArrayList<>();
        SchedulingFactoryQueueProducingArrangement queueProducingArrangement
                = this.nextQueueProducingArrangement;
        while (queueProducingArrangement != null) {
            arrangeConsequences.addAll(queueProducingArrangement.calcConsequence());
            queueProducingArrangement = queueProducingArrangement.getNextQueueProducingArrangement();
        }

        var filteredArrangeConsequences
                = arrangeConsequences.stream()
                .filter(consequence -> consequence.getResource().getRoot() == this)
                .filter(consequence -> consequence.getResource() instanceof ArrangeConsequence.FactoryProducingLength)
                .sorted()
                .toList();

        Duration firstCapacityIncreaseDuration
                = filteredArrangeConsequences.stream()
                .filter(arrangeConsequence -> arrangeConsequence.getLocalDateTime()
                        .isAfter(schedulingDateTimeSlot.getStart()))
                .filter(arrangeConsequence -> arrangeConsequence.getResourceChange() instanceof ArrangeConsequence.Increase)
                .findFirst()
                .map(arrangeConsequence -> Duration.between(
                        schedulingDateTimeSlot.getStart(),
                        arrangeConsequence.getLocalDateTime()
                ))
                .orElse(null);

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

        return Pair.with(remain, remain > 0 ? Duration.ZERO : firstCapacityIncreaseDuration);
    }

}
