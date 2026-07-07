package zzk.townshipscheduler.ui.pojo.scheduling.reactive;

import com.vaadin.flow.signals.local.ValueSignal;
import zzk.townshipscheduler.ui.pojo.scheduling.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public record ReactiveTownshipSchedulingProblemViewModel(
        String uuid,
        Collection<SchedulingProductViewModel> schedulingProductViewModels,
        Collection<SchedulingFactoryInfoViewModel> schedulingFactoryInfoViewModels,
        Collection<SchedulingOrderViewModel> schedulingOrderViewModels,
        Collection<SchedulingFactoryInstanceViewModel> schedulingFactoryInstanceViewModels,
        Collection<SchedulingDateTimeSlotViewModel> schedulingDateTimeSlotViewModels,
        Collection<ReactiveSchedulingProducingArrangementViewModel> schedulingProducingArrangementReactiveViewModels,
        SchedulingWorkCalendarViewModel schedulingWorkCalendar,
        int dateTimeSlotDurationInMinute,
        SchedulingPlayerViewModel schedulingPlayer,
        ValueSignal<String> solverStatus,
        ValueSignal<String> score,
        ValueSignal<Boolean> feasible
)
        implements Serializable {

    public static final ReactiveTownshipSchedulingProblemViewModel EMPTY_NULL_VALUE
            = new ReactiveTownshipSchedulingProblemViewModel(
            "0",
            Set.of(),
            Set.of(),
            Set.of(),
            Set.of(),
            Set.of(),
            Set.of(),
            null,
            0,
            null,
            new ValueSignal<>("N/A"),
            new ValueSignal<>("N/A"),
            new ValueSignal<>(false)
    );

    public List<ReactiveTownshipSchedulingProblemOrderBriefViewModel> toTownshipSchedulingProblemOrderBriefViewModels() {
        return schedulingOrderViewModels.stream()
                .map(
                        schedulingOrder -> {
                            LocalDateTime deadline = schedulingOrder.deadline() != null
                                    ? schedulingOrder.deadline()
                                    : schedulingWorkCalendar.endDateTime();
                            List<ReactiveSchedulingProducingArrangementViewModel> list
                                    = this.schedulingProducingArrangementReactiveViewModels.stream()
                                    .filter(reactiveSchedulingProducingArrangementViewModel -> reactiveSchedulingProducingArrangementViewModel.order()
                                            .equals(schedulingOrder))
                                    .toList();
                            return new ReactiveTownshipSchedulingProblemOrderBriefViewModel(
                                    Math.toIntExact(schedulingOrder.id()),
                                    schedulingOrder.orderType(),
                                    deadline,
                                    schedulingOrder.productAmountBill(),
                                    list
                            );
                        })
                .toList();
    }

}
