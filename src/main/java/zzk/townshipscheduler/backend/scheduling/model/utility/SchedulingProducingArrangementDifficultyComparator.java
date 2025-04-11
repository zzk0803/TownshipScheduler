package zzk.townshipscheduler.backend.scheduling.model.utility;

import org.apache.commons.lang3.builder.CompareToBuilder;
import zzk.townshipscheduler.backend.scheduling.model.BaseSchedulingProducingArrangement;

import java.util.Comparator;

public class SchedulingProducingArrangementDifficultyComparator implements Comparator<BaseSchedulingProducingArrangement> {

    @Override
    public int compare(BaseSchedulingProducingArrangement former, BaseSchedulingProducingArrangement latter) {
        return new CompareToBuilder()
                .append(
                        former.getSupportProducingArrangements().size(),
                        latter.getSupportProducingArrangements().size()
                )
                .append(
                        former.getPrerequisiteProducingArrangements().size(),
                        latter.getPrerequisiteProducingArrangements().size()
                )
                .toComparison();
    }

}
