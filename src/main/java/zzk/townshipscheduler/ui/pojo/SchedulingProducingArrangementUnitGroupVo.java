package zzk.townshipscheduler.ui.pojo;

import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Value
public class SchedulingProducingArrangementUnitGroupVo {

    Long orderId;

    String orderType;

    Set<NestedOrderProduct> nestedOrderProductList = new HashSet<>();

    public boolean add(NestedOrderProduct nestedOrderProduct) {
        return nestedOrderProductList.add(nestedOrderProduct);
    }

    public boolean addAll(@NotNull Collection<? extends NestedOrderProduct> c) {
        return nestedOrderProductList.addAll(c);
    }

    @Value
    public static class NestedOrderProduct {

        String arrangementOrderProductName;

        Integer arrangementOrderProductArrangeId;

        public static NestedOrderProduct of(String orderProductName, Integer orderProductArrangeId) {
            return new NestedOrderProduct(orderProductName, orderProductArrangeId);
        }

    }

}
