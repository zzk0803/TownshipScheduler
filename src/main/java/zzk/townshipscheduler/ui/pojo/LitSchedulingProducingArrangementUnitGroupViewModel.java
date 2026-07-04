package zzk.townshipscheduler.ui.pojo;

import java.util.Collection;
import java.util.Set;

public record LitSchedulingProducingArrangementUnitGroupViewModel(
        long orderId,
        String orderType,
        Set<NestedOrderProductViewModel> nestedOrderProductList
) {

    public boolean add(NestedOrderProductViewModel nestedOrderProductViewModel) {
        return nestedOrderProductList.add(nestedOrderProductViewModel);
    }

    public boolean addAll(Collection<? extends NestedOrderProductViewModel> c) {
        return nestedOrderProductList.addAll(c);
    }

    public static record NestedOrderProductViewModel(
            String arrangementOrderProductName,
            Integer arrangementOrderProductArrangeId
    ) {

        public static NestedOrderProductViewModel of(String orderProductName, Integer orderProductArrangeId) {
            return new NestedOrderProductViewModel(orderProductName, orderProductArrangeId);
        }

    }

}
