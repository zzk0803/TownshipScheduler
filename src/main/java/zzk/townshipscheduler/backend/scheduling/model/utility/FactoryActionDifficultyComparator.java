package zzk.townshipscheduler.backend.scheduling.model.utility;

import org.apache.commons.lang3.builder.CompareToBuilder;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingPlayerFactoryAction;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingExecutionMode;

import java.util.Comparator;

public class FactoryActionDifficultyComparator implements Comparator<SchedulingPlayerFactoryAction> {

    @Override
    public int compare(SchedulingPlayerFactoryAction former, SchedulingPlayerFactoryAction latter) {
        return new CompareToBuilder()
                .append(
                        former.getMaterials().size(),
                        latter.getMaterials().size()
                )
                .append(former.getActionId(), latter.getActionId())
                .toComparison();
    }

}
