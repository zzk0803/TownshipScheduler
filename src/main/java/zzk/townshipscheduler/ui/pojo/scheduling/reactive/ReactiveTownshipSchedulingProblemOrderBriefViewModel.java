package zzk.townshipscheduler.ui.pojo.scheduling.reactive;

import com.vaadin.flow.signals.local.AbstractLocalSignal;
import zzk.townshipscheduler.ui.pojo.scheduling.ProductAmountBillViewModel;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public record ReactiveTownshipSchedulingProblemOrderBriefViewModel(
        int id,
        String orderType,
        LocalDateTime deadline,
        ProductAmountBillViewModel productAmountBill,
        Collection<ReactiveSchedulingProducingArrangementViewModel> arrangementViewModels
) {

    public LocalDateTime calcCompletedDateTime() {
        List<LocalDateTime> relatedArrangementsCompletedDateTime
                = arrangementViewModels.stream()
                .map(ReactiveSchedulingProducingArrangementViewModel::completedDateTime)
                .map(AbstractLocalSignal::get)
                .toList();
        if (relatedArrangementsCompletedDateTime.stream()
                .anyMatch(Objects::isNull)) {
            return LocalDateTime.MAX;
        } else {
            return relatedArrangementsCompletedDateTime.stream()
                    .max(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.MAX);
        }
    }

}
