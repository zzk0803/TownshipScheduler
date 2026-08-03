package zzk.townshipscheduler.ui.pojo.scheduling.reactive;

import com.vaadin.flow.signals.local.ValueSignal;
import zzk.townshipscheduler.ui.pojo.scheduling.SchedulingFactoryInstanceViewModel;
import zzk.townshipscheduler.ui.pojo.scheduling.SchedulingOrderViewModel;
import zzk.townshipscheduler.ui.pojo.scheduling.SchedulingProductViewModel;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public record ReactiveSchedulingProducingArrangementViewModel(
        ReactiveSchedulingProducingArrangementViewModel.ArrangementViewModelId arrangementViewModelId,
        SchedulingProductViewModel product,
        SchedulingProductViewModel orderProduct,
        SchedulingOrderViewModel order,
        ReactiveSchedulingProducingArrangementViewModel.ArrangementViewModelId orderProductArrangementId,
        boolean boolDirectToOrder,
        List<ReactiveSchedulingProducingArrangementViewModel.ArrangementViewModelId> prerequisiteProducingArrangements,
        Duration producingDuration,
        Duration staticDeepPrerequisiteProducingDuration,
        Duration staticDeepProducingDuration,
        String factoryType,
        ValueSignal<SchedulingFactoryInstanceViewModel> assignedFactoryInstance,
        ValueSignal<LocalDateTime> arrangeDateTime,
        ValueSignal<LocalDateTime> producingDateTime,
        ValueSignal<LocalDateTime> completedDateTime
) implements Serializable{

    public boolean boolChild(ReactiveSchedulingProducingArrangementViewModel arrangement) {
        return prerequisiteProducingArrangements.contains(arrangement.arrangementViewModelId);
    }

    public boolean boolParent(ReactiveSchedulingProducingArrangementViewModel arrangement) {
        return arrangement.prerequisiteProducingArrangements.contains(this.arrangementViewModelId);
    }

    public record ArrangementViewModelId(
            int id,
            String uuid
    )
            implements Serializable {

        public static ReactiveSchedulingProducingArrangementViewModel.ArrangementViewModelId of(
                int id,
                String uuid
        ) {
            return new ReactiveSchedulingProducingArrangementViewModel.ArrangementViewModelId(
                    id,
                    uuid
            );
        }

    }

}
