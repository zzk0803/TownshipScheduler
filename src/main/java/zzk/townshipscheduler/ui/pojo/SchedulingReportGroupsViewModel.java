package zzk.townshipscheduler.ui.pojo;

import java.util.Collection;
import java.util.List;

public record SchedulingReportGroupsViewModel(
        Collection<SchedulingReportArrangeDateTimeGroupViewModel> schedulingReportArrangeDateTimeGroupViewModels
) {

    public static final SchedulingReportGroupsViewModel EMPTY_NULL_VALUE = new SchedulingReportGroupsViewModel(List.of());

}
