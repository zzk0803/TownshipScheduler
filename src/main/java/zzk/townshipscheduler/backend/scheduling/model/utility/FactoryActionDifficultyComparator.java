package zzk.townshipscheduler.backend.scheduling.model.utility;

import org.apache.commons.lang3.builder.CompareToBuilder;
import zzk.townshipscheduler.backend.scheduling.model.AbstractPlayerProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingPlayerProducingArrangement;

import java.util.Comparator;

public class FactoryActionDifficultyComparator implements Comparator<AbstractPlayerProducingArrangement> {

    @Override
    public int compare(AbstractPlayerProducingArrangement former, AbstractPlayerProducingArrangement latter) {
        return new CompareToBuilder()
                .append(
                        former.getSchedulingProduct().getLevel(),
                        latter.getSchedulingProduct().getLevel()
                )
                .append(
                        former.getMaterials().size(),
                        latter.getMaterials().size()
                )
                .append(former.getActionId(), latter.getActionId())
                .toComparison();
    }

}
