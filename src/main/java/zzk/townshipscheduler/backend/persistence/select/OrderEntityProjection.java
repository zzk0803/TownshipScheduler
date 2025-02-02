package zzk.townshipscheduler.backend.persistence.select;

import zzk.townshipscheduler.backend.persistence.OrderEntity;
import zzk.townshipscheduler.backend.OrderEntityScheduleState;
import zzk.townshipscheduler.backend.OrderType;
import zzk.townshipscheduler.backend.persistence.ProductEntity;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Projection for {@link OrderEntity}
 */
public interface OrderEntityProjection {

    Long getId();

    OrderType getOrderType();

    LocalDateTime getCreatedDateTime();

    boolean isBoolDeadLine();

    LocalDateTime getDeadLine();

    OrderEntityScheduleState getBillScheduleState();

    Map<ProductEntity, Integer> getProductAmountMap();

    boolean isBoolFinished();

    LocalDateTime getFinishedDateTime();

}
