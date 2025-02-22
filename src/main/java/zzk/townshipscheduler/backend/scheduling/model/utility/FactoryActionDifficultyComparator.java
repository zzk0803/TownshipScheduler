package zzk.townshipscheduler.backend.scheduling.model.utility;

import org.apache.commons.lang3.builder.CompareToBuilder;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingPlayerFactoryAction;

import java.util.Comparator;

public class FactoryActionDifficultyComparator implements Comparator<SchedulingPlayerFactoryAction> {

    @Override
    public int compare(SchedulingPlayerFactoryAction former, SchedulingPlayerFactoryAction latter) {
        return new CompareToBuilder()
                .append(
                        former.getProducingExecutionMode().getMaterials().size(),
                        latter.getProducingExecutionMode().getMaterials().size()
                )
                .append(
                        former.getActionDuration(),
                        latter.getActionDuration()
                )
                .append(
                        former.getActionId(),
                        latter.getActionId()
                )
                .toComparison();
    }

}
