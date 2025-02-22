package zzk.townshipscheduler.backend.scheduling.model.utility;

import org.apache.commons.lang3.builder.CompareToBuilder;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryTimeSlotInstance;

import java.util.Comparator;

public class PlanningTimeSlotFactoryStrengthComparator implements Comparator<SchedulingFactoryTimeSlotInstance> {

    @Override
    public int compare(SchedulingFactoryTimeSlotInstance former, SchedulingFactoryTimeSlotInstance latter) {
        int formerRemainSize = former.calcRemainProducingQueueSize();
        int latterRemainSize = latter.calcRemainProducingQueueSize();
        return new CompareToBuilder()
                .append(
                        formerRemainSize,
                        latterRemainSize
                )
                .append(
                        former.getId(),
                        latter.getId()
                )
                .toComparison();
    }

}
