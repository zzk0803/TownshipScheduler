package zzk.townshipscheduler.ui.pojo;

import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * DTO for {@link TownshipSchedulingProblem}
 */
public record TownshipSchedulingProblemViewModel(
        String uuid,
        Collection<SchedulingProductViewModel> schedulingProductViewModels,
        Collection<SchedulingFactoryInfoViewModel> schedulingFactoryInfoViewModels,
        Collection<SchedulingOrderViewModel> schedulingOrderViewModels,
        Collection<SchedulingFactoryInstanceViewModel> schedulingFactoryInstanceViewModels,
        Collection<SchedulingDateTimeSlotViewModel> schedulingDateTimeSlotViewModels,
        Collection<SchedulingProducingArrangementViewModel> schedulingProducingArrangementViewModels,
        SchedulingWorkCalendarViewModel schedulingWorkCalendar,
        SchedulingPlayerViewModel schedulingPlayer,
        String solverStatus,
        String score,
        boolean feasible
) implements Serializable {

    public TownshipSchedulingProblemViewModel update(
            Collection<SchedulingProducingArrangementViewModel> schedulingProducingArrangements,
            String solverStatus,
            String score,
            boolean feasible
    ) {
        return new TownshipSchedulingProblemViewModel(
                this.uuid,
                this.schedulingProductViewModels,
                this.schedulingFactoryInfoViewModels,
                this.schedulingOrderViewModels,
                this.schedulingFactoryInstanceViewModels,
                this.schedulingDateTimeSlotViewModels,
                schedulingProducingArrangements,
                this.schedulingWorkCalendar,
                this.schedulingPlayer,
                solverStatus,
                score,
                feasible
        );
    }

}
