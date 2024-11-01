package zzk.townshipscheduler.backend.scheduling.model;

import zzk.townshipscheduler.backend.persistence.Order;
import zzk.townshipscheduler.port.GoodId;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class SchedulingOrder {

    private long id;

    private String orderType;

    private Map<SchedulingGoods, Integer> bill;

    private LocalDateTime deadline;

    public SchedulingOrder() {
    }

    public SchedulingOrder(Order order) {
        this.id = order.getId();
        this.orderType = order.getOrderType().name();
        this.deadline = order.getDeadLine();
        this.bill = new LinkedHashMap<>();
    }

    public SchedulingOrder(long id, String orderType, Map<SchedulingGoods, Integer> bill, LocalDateTime deadline) {
        this.id = id;
        this.orderType = orderType;
        this.bill = bill;
        this.deadline = deadline;
    }

}
