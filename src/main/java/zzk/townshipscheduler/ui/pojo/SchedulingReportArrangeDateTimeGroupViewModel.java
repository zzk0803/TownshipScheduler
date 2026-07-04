package zzk.townshipscheduler.ui.pojo;

import java.time.LocalDateTime;
import java.util.Collection;

public record SchedulingReportArrangeDateTimeGroupViewModel(
        LocalDateTime arrangeDateTime,
        Collection<SchedulingReportFactoryGroupViewModel> schedulingReportFactoryGroupViewModels
) {

}
