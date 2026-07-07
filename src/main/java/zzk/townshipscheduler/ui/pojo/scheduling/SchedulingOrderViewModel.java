package zzk.townshipscheduler.ui.pojo.scheduling;

import zzk.townshipscheduler.backend.scheduling.model.SchedulingOrder;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * DTO for {@link SchedulingOrder}
 */
public record SchedulingOrderViewModel(
        long id,
        String orderType,
        LocalDateTime deadline,
        ProductAmountBillViewModel productAmountBill
)
        implements Serializable {

    public String getReadable() {
        return orderType() + "#" + id();
    }

    public LitSchedulingOrderVo toLitSchedulingOrderVo() {
        return new LitSchedulingOrderVo(
                id,
                orderType,
                Optional.ofNullable(deadline())
                        .map(localDateTime -> localDateTime.format(
                                DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        )
                        .orElse("N/A")
        );
    }

}
