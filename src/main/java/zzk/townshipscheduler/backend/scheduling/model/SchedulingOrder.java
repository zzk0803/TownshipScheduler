package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.checkerframework.checker.units.qual.A;
import zzk.townshipscheduler.backend.scheduling.ProductAmountBill;
import zzk.townshipscheduler.backend.persistence.OrderEntityDto;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class SchedulingOrder implements SchedulingGameActionObject {

    @PlanningId
    @EqualsAndHashCode.Include
    private long id;

    private ProductAmountBill productAmountBill;

    private String orderType;

    private LocalDateTime deadline;

    public SchedulingOrder(long id, ProductAmountBill productAmountBill, String orderType, LocalDateTime deadline) {
        this.id = id;
        this.productAmountBill = productAmountBill;
        this.orderType = orderType;
        this.deadline = deadline;
    }

    public SchedulingOrder(ProductAmountBill productAmountBill, String orderType, LocalDateTime deadline) {
        this.productAmountBill = productAmountBill;
        this.orderType = orderType;
        this.deadline = deadline;
    }

    public SchedulingOrder(
            Map<SchedulingProduct, Integer> itemAmountMap,
            String orderType,
            LocalDateTime deadline
    ) {
        this.productAmountBill = ProductAmountBill.of(itemAmountMap);
        this.orderType = orderType;
        this.deadline = deadline;
    }

    @Override
    public List<SchedulingGameAction> getGameActionSet() {
        SchedulingGameActionOrderFulfill orderFulfillAction
                = new SchedulingGameActionOrderFulfill(this);

        List<SchedulingGameAction> orderItemProducingActions
                = this.getProductAmountBill().entrySet().stream()
                .flatMap(entry -> {
                    SchedulingProduct schedulingProduct = entry.getKey();
                    int amount = entry.getValue();
                    return IntStream.range(0, amount)
                            .mapToObj(_ -> schedulingProduct.getGameActionSet())
                            .flatMap(Collection::stream);
                })
                .peek(orderFulfillAction::biAssociateWholeToPart)
                .toList();

        ArrayList<SchedulingGameAction> result = new ArrayList<>(orderItemProducingActions.size()+3);
        result.add(orderFulfillAction);
        result.addAll(orderItemProducingActions);
        return result;
    }

    @Value
    public static class Id{

        private long value;

        public static Id of(long value) {
            return new Id(value);
        }

        public static Id of(OrderEntityDto orderEntityDto) {
            return of(orderEntityDto.getId());
        }
    }
}
