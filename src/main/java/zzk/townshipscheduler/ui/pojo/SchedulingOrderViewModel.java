package zzk.townshipscheduler.ui.pojo;

import zzk.townshipscheduler.backend.OrderType;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingOrder;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO for {@link SchedulingOrder}
 */
public record SchedulingOrderViewModel(
        long id,
        OrderType orderType,
        LocalDateTime deadline,
        ProductAmountBillViewModel productAmountBill
) implements Serializable {
    public String getReadable() {
        return orderType() + "#" + id();
    }
}
