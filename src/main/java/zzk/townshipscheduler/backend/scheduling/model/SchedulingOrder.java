package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import lombok.Data;
import zzk.townshipscheduler.backend.persistence.OrderEntity;

import java.time.LocalDateTime;
import java.util.*;

@Data
public class SchedulingOrder {

    private String id;

    private long entityId;

    private String orderType;

    private Map<SchedulingProduct, Integer> itemAmountMap;

    private LocalDateTime deadline;

    public SchedulingOrder() {
    }

    public SchedulingOrder(OrderEntity orderEntity) {
        this.id = UUID.randomUUID().toString();
        this.entityId = orderEntity.getId();
        this.orderType = orderEntity.getOrderType().name();
        this.deadline = orderEntity.getDeadLine();
        this.itemAmountMap = new LinkedHashMap<>();
    }

    public SchedulingOrder(long id, String orderType, Map<SchedulingProduct, Integer> itemAmountMap, LocalDateTime deadline) {
        this.id=UUID.randomUUID().toString();
        this.entityId = id;
        this.orderType = orderType;
        this.itemAmountMap = itemAmountMap;
        this.deadline = deadline;
    }

    public List<SchedulingProducing> calcProducingGoods() {
        List<SchedulingProducing> result = new ArrayList<>();

        this.itemAmountMap.forEach((schedulingProduct, amount) -> {
            for (int i = 0; i < amount; i++) {
                result.addAll(schedulingProduct.calcProducingGoods());
            }
        });
        return result;
    }
}
