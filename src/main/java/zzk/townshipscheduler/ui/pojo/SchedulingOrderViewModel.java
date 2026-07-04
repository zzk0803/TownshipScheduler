package zzk.townshipscheduler.ui.pojo;

import zzk.townshipscheduler.backend.scheduling.model.SchedulingOrder;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO for {@link SchedulingOrder}
 */
public record SchedulingOrderViewModel(
        long id,
        String orderType,
        LocalDateTime deadline,
        ProductAmountBillViewModel productAmountBill
) implements Serializable {
    public String getReadable() {
        return orderType() + "#" + id();
    }
}
