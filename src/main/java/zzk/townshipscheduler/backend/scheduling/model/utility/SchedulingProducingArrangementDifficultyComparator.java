package zzk.townshipscheduler.backend.scheduling.model.utility;

import org.apache.commons.lang3.builder.CompareToBuilder;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;

import java.util.Comparator;

public class SchedulingProducingArrangementDifficultyComparator implements Comparator<SchedulingProducingArrangement> {

    @Override
    public int compare(SchedulingProducingArrangement former, SchedulingProducingArrangement latter) {
        return new CompareToBuilder()
                .append(
                        former.getPrerequisiteProducingArrangements().size(),
                        latter.getPrerequisiteProducingArrangements().size()
                )
                .append(
                        former.getProducingDuration(),
                        latter.getProducingDuration()
                )
                .append(former.getId(), latter.getId())
                .toComparison();
    }

}
