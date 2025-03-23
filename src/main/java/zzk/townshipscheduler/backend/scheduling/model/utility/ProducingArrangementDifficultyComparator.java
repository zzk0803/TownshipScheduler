package zzk.townshipscheduler.backend.scheduling.model.utility;

import org.apache.commons.lang3.builder.CompareToBuilder;
import zzk.townshipscheduler.backend.scheduling.model.BaseProducingArrangement;

import java.util.Comparator;

public class ProducingArrangementDifficultyComparator implements Comparator<BaseProducingArrangement> {

    @Override
    public int compare(BaseProducingArrangement former, BaseProducingArrangement latter) {
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
