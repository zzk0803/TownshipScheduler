package zzk.townshipscheduler.ui.pojo.scheduling;

import java.time.LocalDateTime;
import java.util.Collection;

public record SchedulingReportArrangeDateTimeGroupViewModel(
        LocalDateTime arrangeDateTime,
        Collection<SchedulingReportFactoryGroupViewModel> schedulingReportFactoryGroupViewModels
) {

}
