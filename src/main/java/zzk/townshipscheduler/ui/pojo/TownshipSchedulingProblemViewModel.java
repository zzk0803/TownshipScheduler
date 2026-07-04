package zzk.townshipscheduler.ui.pojo;

import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

import java.io.Serializable;
import java.time.LocalDateTime;
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
        int dateTimeSlotDurationInMinute,
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
                dateTimeSlotDurationInMinute,
                this.schedulingPlayer,
                solverStatus,
                score,
                feasible
        );
    }

    public List<TownshipSchedulingProblemOrderBriefViewModel> toTownshipSchedulingProblemOrderBriefViewModels() {
        return schedulingOrderViewModels.stream()
                .map(
                        schedulingOrder -> {
                            LocalDateTime deadline = schedulingOrder.deadline() != null ? schedulingOrder.deadline() : schedulingWorkCalendar.endDateTime();
                            return new TownshipSchedulingProblemOrderBriefViewModel(
                                    Math.toIntExact(schedulingOrder.id()),
                                    schedulingOrder.orderType(),
                                    deadline,
                                    schedulingOrder.productAmountBill(),
                                    this.schedulingProducingArrangementViewModels.stream()
                                            .filter(schedulingProducingArrangementViewModel -> schedulingProducingArrangementViewModel.order()
                                                    .equals(schedulingOrder))
                                            .toList()
                            );
                        })
                .toList();
    }

}
