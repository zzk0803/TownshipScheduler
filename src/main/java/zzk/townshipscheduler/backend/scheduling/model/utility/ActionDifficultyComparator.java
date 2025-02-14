package zzk.townshipscheduler.backend.scheduling.model.utility;

import org.apache.commons.lang3.builder.CompareToBuilder;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingPlayerFactoryAction;

import java.util.Comparator;
import java.util.List;

public class ActionDifficultyComparator implements Comparator<SchedulingPlayerFactoryAction> {

    @Override
    public int compare(SchedulingPlayerFactoryAction action1, SchedulingPlayerFactoryAction action2) {
        List<SchedulingPlayerFactoryAction> action1PrerequisiteActions = action1.getPrerequisiteActions();
        List<SchedulingPlayerFactoryAction> action2PrerequisiteActions = action2.getPrerequisiteActions();

        return new CompareToBuilder()
                .append(action1PrerequisiteActions.size(), action2PrerequisiteActions.size())
                .toComparison();
    }

}
