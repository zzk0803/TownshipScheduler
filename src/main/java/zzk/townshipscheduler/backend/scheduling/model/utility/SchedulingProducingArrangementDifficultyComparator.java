package zzk.townshipscheduler.backend.scheduling.model.utility;

import org.apache.commons.lang3.builder.CompareToBuilder;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;

import java.util.Comparator;

public class SchedulingProducingArrangementDifficultyComparator implements Comparator<SchedulingProducingArrangement> {

    public static final SchedulingProducingArrangementDifficultyComparator INSTANCE =
            new SchedulingProducingArrangementDifficultyComparator();

    @Override
    public int compare(SchedulingProducingArrangement former, SchedulingProducingArrangement latter) {
        return new CompareToBuilder()
                .append(
                        former.getSchedulingOrder().boolHasDeadline(),
                        latter.getSchedulingOrder().boolHasDeadline()
                )
                .append(
                        former.getDeepPrerequisiteProducingArrangements().size(),
                        latter.getDeepPrerequisiteProducingArrangements().size()
                )
                .append(
                        former.getUuid(),
                        latter.getUuid()
                )
                .toComparison();
    }

}
