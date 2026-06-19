package zzk.townshipscheduler.backend.scheduling.model.utility;

import org.apache.commons.lang3.builder.CompareToBuilder;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;

import java.util.Comparator;

public class SchedulingProducingArrangementStrengthComparator implements Comparator<SchedulingProducingArrangement> {

    public static final Comparator<SchedulingProducingArrangement> INSTANCE = new SchedulingProducingArrangementStrengthComparator();

    @Override
    public int compare(
            SchedulingProducingArrangement former,
            SchedulingProducingArrangement latter
    ) {
        return new CompareToBuilder()
                .append(
                        former.getUuid(),
                        latter.getUuid()
                )
                .append(
                        former.getId(),
                        latter.getId()
                )
                .toComparison();
    }

}
