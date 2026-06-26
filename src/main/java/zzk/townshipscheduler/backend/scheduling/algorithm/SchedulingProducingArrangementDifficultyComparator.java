package zzk.townshipscheduler.backend.scheduling.algorithm;

import org.apache.commons.lang3.builder.CompareToBuilder;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;

import java.util.Comparator;

public class SchedulingProducingArrangementDifficultyComparator
        implements Comparator<SchedulingProducingArrangement> {

    public static final Comparator<SchedulingProducingArrangement> INSTANCE = new SchedulingProducingArrangementDifficultyComparator();

    @Override
    public int compare(
            SchedulingProducingArrangement former,
            SchedulingProducingArrangement latter
    ) {
        return new CompareToBuilder()
                .append(
                        former.getDeepPrerequisiteProducingArrangementsSize(),
                        latter.getDeepPrerequisiteProducingArrangementsSize()
                )
                .append(
                        former.boolBearSuccessorProducingArrangement(),
                        latter.boolBearSuccessorProducingArrangement()
                )
                .append(
                        former.getUuid(),
                        latter.getUuid()
                )
                .toComparison();
    }

}
