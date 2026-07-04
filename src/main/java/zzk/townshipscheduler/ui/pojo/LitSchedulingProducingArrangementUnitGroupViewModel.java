package zzk.townshipscheduler.ui.pojo;

import java.util.Collection;
import java.util.Set;

public record LitSchedulingProducingArrangementUnitGroupViewModel(
        Long orderId,
        String orderType,
        Set<NestedOrderProductViewModel> nestedOrderProductViewModelList
) {

    public boolean add(NestedOrderProductViewModel nestedOrderProductViewModel) {
        return nestedOrderProductViewModelList.add(nestedOrderProductViewModel);
    }

    public boolean addAll(Collection<? extends NestedOrderProductViewModel> c) {
        return nestedOrderProductViewModelList.addAll(c);
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
