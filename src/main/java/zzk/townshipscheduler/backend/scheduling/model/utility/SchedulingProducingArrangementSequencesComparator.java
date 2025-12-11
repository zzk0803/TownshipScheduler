package zzk.townshipscheduler.backend.scheduling.model.utility;

import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Objects;

public class SchedulingProducingArrangementSequencesComparator implements Comparator<SchedulingProducingArrangement> {


    @Override
    public int compare(SchedulingProducingArrangement former, SchedulingProducingArrangement latter) {
        LocalDateTime formerDateTime = Objects.requireNonNullElse(
                former.getArrangeDateTime(),
                former.getSchedulingWorkCalendar().getStartDateTime().plus(former.getStaticPrerequisiteProducingDuration())
        );
        LocalDateTime latterDateTime = Objects.requireNonNullElse(
                latter.getArrangeDateTime(),
                latter.getSchedulingWorkCalendar().getStartDateTime().plus(latter.getStaticPrerequisiteProducingDuration())
        );
        return formerDateTime.compareTo(latterDateTime);
    }

}
