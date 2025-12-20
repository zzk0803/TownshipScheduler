package zzk.townshipscheduler.backend.scheduling.model;

import java.io.Serial;
import java.util.LinkedHashMap;
import java.util.Map;


public final class ProductAmountBill extends LinkedHashMap<SchedulingProduct, Integer> {

    @Serial
    private static final long serialVersionUID = 6380073762508874203L;

    public static ProductAmountBill of(Map<SchedulingProduct, Integer> schedulingProductIntegerMap) {
        ProductAmountBill productAmountBill = new ProductAmountBill();
        productAmountBill.putAll(schedulingProductIntegerMap);
        return productAmountBill;
    }

}
