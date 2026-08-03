package zzk.townshipscheduler.ui.pojo.scheduling;

import java.time.LocalDateTime;
import java.util.Collection;

public record SchedulingReportArrangeDateTimeGroupViewModel(
        LocalDateTime arrangeDateTime,
        Collection<SchedulingReportFactoryGroupViewModel> schedulingReportFactoryGroupViewModels
) implements Comparable<SchedulingReportArrangeDateTimeGroupViewModel> {

    @Override
    public int compareTo(SchedulingReportArrangeDateTimeGroupViewModel that) {
        return this.arrangeDateTime().compareTo(that.arrangeDateTime());
    }

}
