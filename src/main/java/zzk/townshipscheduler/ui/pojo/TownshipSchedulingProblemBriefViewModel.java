package zzk.townshipscheduler.ui.pojo;

import java.util.Collection;

public record TownshipSchedulingProblemBriefViewModel(
        String uuid,
        String solverStatus,
        Collection<SchedulingOrderViewModel> orderList
) {

}
