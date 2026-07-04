package zzk.townshipscheduler.ui.pojo;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public record TownshipSchedulingProblemOrderBriefViewModel(
        int id,
        String orderType,
        LocalDateTime deadline,
        ProductAmountBillViewModel productAmountBill,
        Collection<SchedulingProducingArrangementViewModel> arrangementViewModels
) {

    public LocalDateTime calcCompletedDateTime() {
        List<LocalDateTime> relatedArrangementsCompletedDateTime = arrangementViewModels.stream()
                .map(SchedulingProducingArrangementViewModel::completedDateTime)
                .toList();
        if (relatedArrangementsCompletedDateTime.stream()
                .anyMatch(Objects::isNull)) {
            return null;
        } else {
            return relatedArrangementsCompletedDateTime.stream()
                    .max(LocalDateTime::compareTo)
                    .orElseThrow();
        }
    }

}
