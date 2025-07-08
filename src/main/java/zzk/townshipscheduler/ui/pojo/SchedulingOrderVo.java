package zzk.townshipscheduler.ui.pojo;

import lombok.Data;
import zzk.townshipscheduler.backend.OrderType;
import zzk.townshipscheduler.backend.scheduling.model.ProductAmountBill;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Data
public final class SchedulingOrderVo {

    private int serial;

    private OrderType orderType;

    private LocalDateTime deadline;

    private ProductAmountBill productAmountBill;

    private List<SchedulingProducingArrangement> relatedArrangements;

    public LocalDateTime getCompletedDateTime() {
        List<LocalDateTime> relatedArrangementsCompletedDateTime = relatedArrangements.stream()
                .map(SchedulingProducingArrangement::getCompletedDateTime)
                .toList();
        if (relatedArrangementsCompletedDateTime.stream().anyMatch(Objects::isNull)) {
            return null;
        }else {
            return relatedArrangementsCompletedDateTime.stream().max(LocalDateTime::compareTo).orElseThrow();
        }
    }

}
