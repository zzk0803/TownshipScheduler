package zzk.townshipscheduler.backend.scheduling.algorithm;

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
