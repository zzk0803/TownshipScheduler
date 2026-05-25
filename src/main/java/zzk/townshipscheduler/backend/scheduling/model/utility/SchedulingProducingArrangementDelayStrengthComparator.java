package zzk.townshipscheduler.backend.scheduling.model.utility;

import org.apache.commons.lang3.builder.CompareToBuilder;

import java.util.Comparator;

public class SchedulingProducingArrangementDelayStrengthComparator implements Comparator<Integer> {

    @Override
    public int compare(
            Integer latter,
            Integer former
    ) {
        return new CompareToBuilder()
                .append(
                        former,
                        latter
                )
                .toComparison();
    }

}
