package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Value;
import zzk.townshipscheduler.backend.OrderType;
import zzk.townshipscheduler.backend.persistence.select.OrderEntityDto;
import zzk.townshipscheduler.backend.scheduling.ProductAmountBill;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.IntStream;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class SchedulingOrder implements SchedulingGameActionObject {

    @PlanningId
    @EqualsAndHashCode.Include
    private Long id;

    private ProductAmountBill productAmountBill;

//    private String orderType;

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
    public String readable() {
        String stringBuilder = "Order#" + getId() +
                               "~Type::" + getOrderType().name() +
                               "~Deadline::" + (getDeadline() == null ? "N/A" : getDeadline());
        return stringBuilder;
    }

    @Override
    public List<SchedulingPlayerWarehouseAction> calcWarehouseActions() {
        SchedulingPlayerWarehouseAction schedulingPlayerWarehouseAction
                = new SchedulingPlayerWarehouseAction(this);
        return List.of(schedulingPlayerWarehouseAction);
    }

    @Override
    public List<SchedulingPlayerFactoryAction> calcFactoryActions() {
        List<SchedulingPlayerFactoryAction> result
                = this.getProductAmountBill().entrySet().stream()
                .flatMap(entry -> {
                    SchedulingProduct schedulingProduct = entry.getKey();
                    int amount = entry.getValue();
                    return IntStream.range(0, amount)
                            .mapToObj(_ -> schedulingProduct.calcFactoryActions())
                            .flatMap(Collection::stream);
                })
                .toList();

        return result;
    }

    @Override
    public List<SchedulingPlayerFactoryAction> calcFactoryActions(SchedulingGameActionObject targetObject) {
        List<SchedulingPlayerFactoryAction> result
                = this.getProductAmountBill().entrySet().stream()
                .flatMap(entry -> {
                    SchedulingProduct schedulingProduct = entry.getKey();
                    int amount = entry.getValue();
                    return IntStream.range(0, amount)
                            .mapToObj(_ -> schedulingProduct.calcFactoryActions(targetObject))
                            .flatMap(Collection::stream);
                })
                .toList();

        return result;
    }

    @Override
    public Set<SchedulingGameActionExecutionMode> getExecutionModeSet() {
        return Set.of();
    }

    @Override
    public Optional<LocalDateTime> optionalDeadline() {
        return Optional.ofNullable(getDeadline());
    }

    @Value
    public static class Id {

        private long value;

        public static Id of(OrderEntityDto orderEntityDto) {
            return of(orderEntityDto.getId());
        }

        public static Id of(long value) {
            return new Id(value);
        }

    }

}
