package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Value;
import zzk.townshipscheduler.backend.OrderType;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.IntStream;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class SchedulingOrder implements IGameArrangeObject, Serializable {

    @Serial
    private static final long serialVersionUID = 8878075265837510157L;

    @PlanningId
    @EqualsAndHashCode.Include
    private Long id;

    private ProductAmountBill productAmountBill;

    private OrderType orderType;

    private LocalDateTime deadline;

    public SchedulingOrder(long id, ProductAmountBill productAmountBill, OrderType orderType, LocalDateTime deadline) {
        this.id = id;
        this.productAmountBill = productAmountBill;
        this.orderType = orderType;
        this.deadline = deadline;
    }

    public SchedulingOrder(ProductAmountBill productAmountBill, OrderType orderType, LocalDateTime deadline) {
        this.productAmountBill = productAmountBill;
        this.orderType = orderType;
        this.deadline = deadline;
    }

    public SchedulingOrder(
            Map<SchedulingProduct, Integer> itemAmountMap,
            OrderType orderType,
            LocalDateTime deadline
    ) {
        this.productAmountBill = ProductAmountBill.of(itemAmountMap);
        this.orderType = orderType;
        this.deadline = deadline;
    }

    @Override
    public Long longIdentity() {
        return getId();
    }

    @Override
    public String readable() {
        return "Order#" + getId() +
               "~Type::" + getOrderType().name() +
               "~Deadline::" + (getDeadline() == null ? "N/A" : getDeadline());
    }

//    @Override
//    public List<SchedulingPlayerWarehouseAction> calcWarehouseActions() {
//        SchedulingPlayerWarehouseAction schedulingPlayerWarehouseAction
//                = new SchedulingPlayerWarehouseAction(this);
//        return List.of(schedulingPlayerWarehouseAction);
//    }
//
//    @Override
//    public List<SchedulingPlayerWarehouseAction> calcWarehouseActions(IGameActionObject targetObject) {
//        SchedulingPlayerWarehouseAction schedulingPlayerWarehouseAction
//                = new SchedulingPlayerWarehouseAction(targetObject);
//        return List.of(schedulingPlayerWarehouseAction);
//    }

    @Override
    public List<SchedulingProducingArrangement> calcFactoryActions() {
        return this.calcFactoryActions(this);
    }

    @Override
    public List<SchedulingProducingArrangement> calcFactoryActions(IGameArrangeObject targetObject) {
         return this.getProductAmountBill().entrySet()
                .stream()
                .flatMap(entry -> {
                    SchedulingProduct schedulingProduct = entry.getKey();
                    int amount = entry.getValue();
                    return IntStream.range(0, amount)
                            .mapToObj(_ -> schedulingProduct.calcFactoryActions(targetObject))
                            .flatMap(Collection::stream)
                            .peek(
                                    schedulingProducingArrangement -> schedulingProducingArrangement.setSchedulingOrder(this)
                            );
                })
                .toList();
    }

    @Override
    public Set<SchedulingProducingExecutionMode> getExecutionModeSet() {
        return Set.of();
    }

    @Override
    public Optional<LocalDateTime> optionalDeadline() {
        return Optional.ofNullable(getDeadline());
    }

    public boolean boolHasDeadline() {
        return optionalDeadline().isPresent();
    }

    @Value
    public static class Id {

        private long value;

        public static Id of(long value) {
            return new Id(value);
        }

    }

}
