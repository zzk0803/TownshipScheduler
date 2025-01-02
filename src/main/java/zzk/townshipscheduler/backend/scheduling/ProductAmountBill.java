package zzk.townshipscheduler.backend.scheduling;

import zzk.townshipscheduler.backend.scheduling.model.SchedulingProduct;

import java.util.LinkedHashMap;
import java.util.Map;


public class ProductAmountBill extends LinkedHashMap<SchedulingProduct, Integer> {

    public static ProductAmountBill of(Map<SchedulingProduct, Integer> schedulingProductIntegerMap) {
        ProductAmountBill productAmountBill = new ProductAmountBill();
        productAmountBill.putAll(schedulingProductIntegerMap);
        return productAmountBill;
    }

}
